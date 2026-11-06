package com.openkiosk.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/** Ajustes del anfitrión. Si hay PIN configurado, se pide antes de mostrar nada. */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContentView(R.layout.activity_settings)
        val requirePin = intent.getBooleanExtra(EXTRA_REQUIRE_PIN, true)
        if (requirePin && prefs.pin.isNotEmpty()) askPin() else showForm()
    }

    private fun askPin() {
        findViewById<View>(R.id.form).visibility = View.INVISIBLE
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.pin_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.pin_title)
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (input.text.toString() == prefs.pin) {
                    showForm()
                } else {
                    Toast.makeText(this, R.string.toast_bad_pin, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .show()
    }

    private fun showForm() {
        findViewById<View>(R.id.form).visibility = View.VISIBLE
        val url = findViewById<EditText>(R.id.url)
        val pin = findViewById<EditText>(R.id.pin)
        val reload = findViewById<EditText>(R.id.reload)
        val hosts = findViewById<EditText>(R.id.hosts)

        url.setText(prefs.url)
        pin.setText(prefs.pin)
        reload.setText(prefs.reloadMinutes.toString())
        hosts.setText(prefs.extraHosts)
        findViewById<TextView>(R.id.version).text = getString(R.string.version_fmt, BuildConfig.VERSION_NAME)

        findViewById<Button>(R.id.save).setOnClickListener { save(url, pin, reload, hosts) }
        findViewById<Button>(R.id.home_settings).setOnClickListener { openHomeSettings() }
        findViewById<Button>(R.id.system_settings).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, R.string.toast_no_settings, Toast.LENGTH_LONG).show()
            }
        }
        url.requestFocus()
    }

    private fun save(url: EditText, pin: EditText, reload: EditText, hosts: EditText) {
        val u = url.text.toString().trim()
        if (!UrlPolicy.isValidHomeUrl(u)) {
            url.error = getString(R.string.err_url)
            url.requestFocus()
            return
        }
        val p = pin.text.toString().trim()
        if (p.isNotEmpty() && !Prefs.isValidPin(p)) {
            pin.error = getString(R.string.err_pin)
            pin.requestFocus()
            return
        }
        prefs.url = u
        prefs.pin = p
        prefs.reloadMinutes = reload.text.toString().trim().toIntOrNull()?.coerceIn(0, 1440) ?: 60
        prefs.extraHosts = hosts.text.toString().trim()
        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        finish()
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
        Toast.makeText(this, R.string.toast_home_hint, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_REQUIRE_PIN = "require_pin"
    }
}
