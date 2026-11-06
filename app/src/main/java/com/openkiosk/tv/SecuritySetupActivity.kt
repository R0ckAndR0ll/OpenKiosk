package com.openkiosk.tv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

/**
 * Se muestra justo después de crear o restablecer el PIN (siempre ya autenticado):
 *  1. Muestra UNA vez el código de recuperación (solo se guarda su hash).
 *  2. Muestra el QR del autenticador y pide confirmar un código para activarlo.
 */
class SecuritySetupActivity : Activity() {

    private lateinit var sec: SecurityStore
    private lateinit var pendingSecret: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Evita capturas de pantalla o que el código aparezca en "apps recientes".
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_security_setup)
        sec = SecurityStore(this)

        val prefs = Prefs(this)
        val label = prefs.propertyName.ifBlank { "TV" }

        val recovery = sec.newRecoveryCode()
        findViewById<TextView>(R.id.recovery_code).text = recovery

        pendingSecret = Totp.generateSecret()
        val qrSize = (resources.displayMetrics.density * 220).toInt()
        findViewById<ImageView>(R.id.totp_qr).setImageBitmap(Qr.bitmap(Totp.uri(pendingSecret, label), qrSize))
        findViewById<TextView>(R.id.totp_secret).text = pendingSecret.chunked(4).joinToString(" ")

        val input = findViewById<EditText>(R.id.totp_confirm)
        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            val counter = Totp.match(pendingSecret, input.text.toString().trim(), -1L)
            if (counter >= 0) {
                sec.enableTotp(pendingSecret, counter)
                Toast.makeText(this, R.string.toast_totp_on, Toast.LENGTH_LONG).show()
                finish()
            } else {
                input.error = getString(R.string.err_totp)
                input.requestFocus()
            }
        }
        findViewById<Button>(R.id.btn_skip).setOnClickListener { skip() }
        input.requestFocus()
    }

    private fun skip() {
        sec.skipTotp()
        Toast.makeText(this, R.string.toast_totp_skipped, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            skip()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
