package com.openkiosk.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/**
 * Ajustes del anfitrión. Si hay PIN, se pide el PIN o el código del autenticador antes de
 * mostrar nada. Si se olvidó, se puede usar el código de recuperación (un solo uso).
 */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var sec: SecurityStore

    /** Tras usar el código de recuperación (o migrar un PIN viejo) hay que poner un PIN nuevo. */
    private var mustSetNewPin = false
    private var regenerateSecurity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        prefs = Prefs(this)
        sec = SecurityStore(this)
        setContentView(R.layout.activity_settings)
        if (sec.hasPin) askUnlock() else showForm()
    }

    private fun askUnlock() {
        findViewById<View>(R.id.form).visibility = View.INVISIBLE
        val locked = sec.lockRemainingMs()
        if (locked > 0) {
            toast(getString(R.string.toast_locked, Provisioning.minutes(locked)))
            finish()
            return
        }
        val input = codeInput(getString(R.string.pin_hint), numeric = true, max = 6)
        AlertDialog.Builder(this)
            .setTitle(R.string.pin_title)
            .setMessage(if (sec.hasTotp) R.string.pin_msg_totp else R.string.pin_msg)
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                when (sec.verifyPinOrTotp(input.text.toString())) {
                    SecurityStore.Result.OK_PIN, SecurityStore.Result.OK_TOTP -> {
                        mustSetNewPin = sec.mustChangePin
                        showForm()
                    }
                    SecurityStore.Result.LOCKED -> {
                        toast(getString(R.string.toast_locked, Provisioning.minutes(sec.lockRemainingMs())))
                        finish()
                    }
                    else -> {
                        toast(getString(R.string.toast_bad_pin))
                        askUnlock()
                    }
                }
            }
            .setNeutralButton(R.string.btn_forgot) { _, _ -> askRecovery() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .show()
    }

    private fun askRecovery() {
        val input = codeInput(getString(R.string.recovery_hint), numeric = false, max = 14)
        AlertDialog.Builder(this)
            .setTitle(R.string.recovery_title)
            .setMessage(R.string.recovery_msg)
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                when (sec.verifyRecovery(input.text.toString())) {
                    SecurityStore.Result.OK_RECOVERY -> {
                        mustSetNewPin = true
                        regenerateSecurity = true
                        showForm()
                    }
                    SecurityStore.Result.LOCKED -> {
                        toast(getString(R.string.toast_locked, Provisioning.minutes(sec.lockRemainingMs())))
                        finish()
                    }
                    else -> {
                        toast(getString(R.string.toast_bad_recovery))
                        askUnlock()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> askUnlock() }
            .show()
    }

    private fun showForm() {
        findViewById<View>(R.id.form).visibility = View.VISIBLE
        val url = findViewById<EditText>(R.id.url)
        val name = findViewById<EditText>(R.id.name)
        val pin = findViewById<EditText>(R.id.pin)
        val reload = findViewById<EditText>(R.id.reload)
        val hosts = findViewById<EditText>(R.id.hosts)
        val pinLabel = findViewById<TextView>(R.id.pin_label)

        url.setText(prefs.url)
        name.setText(prefs.propertyName)
        reload.setText(prefs.reloadMinutes.toString())
        hosts.setText(prefs.extraHosts)
        pinLabel.setText(
            when {
                !sec.hasPin -> R.string.label_pin_new
                mustSetNewPin -> R.string.label_pin_required
                else -> R.string.label_pin_change
            }
        )
        findViewById<TextView>(R.id.security_status).text = getString(
            if (sec.hasTotp) R.string.sec_status_on else R.string.sec_status_off
        )
        findViewById<TextView>(R.id.version).text = getString(R.string.version_fmt, BuildConfig.VERSION_NAME)

        findViewById<Button>(R.id.save).setOnClickListener { save(url, name, pin, reload, hosts) }
        findViewById<Button>(R.id.security).setOnClickListener {
            if (!sec.hasPin) {
                pin.error = getString(R.string.err_pin_first)
                pin.requestFocus()
            } else {
                startActivity(Intent(this, SecuritySetupActivity::class.java))
                finish()
            }
        }
        findViewById<Button>(R.id.home_settings).setOnClickListener { openHomeSettings() }
        findViewById<Button>(R.id.system_settings).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {
                toast(getString(R.string.toast_no_settings))
            }
        }
        if (mustSetNewPin || !sec.hasPin) pin.requestFocus() else url.requestFocus()
    }

    private fun save(url: EditText, name: EditText, pin: EditText, reload: EditText, hosts: EditText) {
        val u = url.text.toString().trim()
        if (!UrlPolicy.isValidHomeUrl(u)) {
            url.error = getString(R.string.err_url)
            url.requestFocus()
            return
        }
        val p = pin.text.toString().trim()
        val pinRequired = !sec.hasPin || mustSetNewPin
        if (p.isEmpty() && pinRequired) {
            pin.error = getString(R.string.err_pin)
            pin.requestFocus()
            return
        }
        if (p.isNotEmpty() && !SecurityStore.isValidPin(p)) {
            pin.error = getString(R.string.err_pin)
            pin.requestFocus()
            return
        }

        val firstPin = !sec.hasPin
        prefs.url = u
        prefs.propertyName = name.text.toString().trim().take(60)
        prefs.reloadMinutes = reload.text.toString().trim().toIntOrNull()?.coerceIn(0, 1440) ?: 60
        prefs.extraHosts = hosts.text.toString().trim()
        if (p.isNotEmpty()) sec.setPin(p)
        toast(getString(R.string.toast_saved))

        // PIN nuevo, recuperación usada, o configuración pendiente: mostrar códigos de seguridad.
        if (firstPin || regenerateSecurity || sec.setupPending) {
            startActivity(Intent(this, SecuritySetupActivity::class.java))
        }
        finish()
    }

    private fun codeInput(hint: String, numeric: Boolean, max: Int) = EditText(this).apply {
        this.hint = hint
        inputType = if (numeric) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        filters = arrayOf(InputFilter.LengthFilter(max))
        isSingleLine = true
    }

    private fun openHomeSettings() {
        for (action in listOf(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_SETTINGS)) {
            try {
                startActivity(Intent(action))
                return
            } catch (e: Exception) {
                // probamos la siguiente opción
            }
        }
        toast(getString(R.string.toast_home_hint))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        const val EXTRA_REQUIRE_PIN = "require_pin"
    }
}
