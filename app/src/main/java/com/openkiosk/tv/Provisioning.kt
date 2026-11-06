package com.openkiosk.tv

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Configuración remota por ADB, para no tener que teclear con el control:
 *
 *   adb shell am start -n com.openkiosk.tv/.MainActivity \
 *     --es url "https://tu-sitio.com/?casa=1" --es set_pin 1234
 *
 * Si ya hay un PIN configurado, hay que mandarlo con --es pin <PIN actual>.
 */
object Provisioning {
    const val EXTRA_URL = "url"
    const val EXTRA_PIN = "pin"
    const val EXTRA_SET_PIN = "set_pin"
    const val EXTRA_RELOAD = "reload_minutes"
    const val EXTRA_HOSTS = "allowed_hosts"

    /** @return true si se aplicó una configuración nueva. */
    fun apply(context: Context, prefs: Prefs, intent: Intent?): Boolean {
        if (intent == null) return false
        val url = intent.getStringExtra(EXTRA_URL) ?: return false

        val currentPin = prefs.pin
        if (currentPin.isNotEmpty() && intent.getStringExtra(EXTRA_PIN) != currentPin) {
            Toast.makeText(context, R.string.toast_bad_pin, Toast.LENGTH_LONG).show()
            clear(intent)
            return false
        }
        if (!UrlPolicy.isValidHomeUrl(url)) {
            Toast.makeText(context, R.string.toast_invalid_url, Toast.LENGTH_LONG).show()
            clear(intent)
            return false
        }

        prefs.url = url.trim()
        intent.getStringExtra(EXTRA_SET_PIN)?.let { if (Prefs.isValidPin(it)) prefs.pin = it }
        intent.getStringExtra(EXTRA_RELOAD)?.toIntOrNull()?.let { prefs.reloadMinutes = it.coerceIn(0, 1440) }
        intent.getStringExtra(EXTRA_HOSTS)?.let { prefs.extraHosts = it.trim() }
        clear(intent)
        Toast.makeText(context, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        return true
    }

    private fun clear(intent: Intent) {
        listOf(EXTRA_URL, EXTRA_PIN, EXTRA_SET_PIN, EXTRA_RELOAD, EXTRA_HOSTS).forEach { intent.removeExtra(it) }
    }
}
