package com.openkiosk.tv

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Configuración remota por ADB, para no tener que teclear con el control:
 *
 *   adb shell am start -n com.openkiosk.tv/.MainActivity \
 *     --es url "https://tu-sitio.com/?casa=1" --es name "Casa de Playa 4B" --es set_pin 123456
 *
 * Si ya hay un PIN, hay que mandar --es pin <PIN actual o código del autenticador>.
 * Los intentos fallidos cuentan para el bloqueo progresivo igual que en la pantalla.
 * El código de recuperación y el autenticador se configuran en la TV (necesitan verse en pantalla).
 */
object Provisioning {
    const val EXTRA_URL = "url"
    const val EXTRA_NAME = "name"
    const val EXTRA_PIN = "pin"
    const val EXTRA_SET_PIN = "set_pin"
    const val EXTRA_RELOAD = "reload_minutes"
    const val EXTRA_HOSTS = "allowed_hosts"
    const val EXTRA_APPS = "allowed_apps"
    const val EXTRA_LAUNCHER_IDLE = "launcher_idle"
    const val EXTRA_MEDIA_IDLE = "media_idle"

    private val ALL = listOf(
        EXTRA_URL, EXTRA_NAME, EXTRA_PIN, EXTRA_SET_PIN, EXTRA_RELOAD, EXTRA_HOSTS,
        EXTRA_APPS, EXTRA_LAUNCHER_IDLE, EXTRA_MEDIA_IDLE,
    )

    /** @return true si se aplicó una configuración nueva. */
    fun apply(context: Context, prefs: Prefs, intent: Intent?): Boolean {
        if (intent == null || ALL.none { intent.hasExtra(it) }) return false
        val sec = SecurityStore(context)
        try {
            if (sec.hasPin) {
                when (sec.verifyPinOrTotp(intent.getStringExtra(EXTRA_PIN) ?: "")) {
                    SecurityStore.Result.OK_PIN, SecurityStore.Result.OK_TOTP -> Unit
                    SecurityStore.Result.LOCKED -> {
                        toast(context, context.getString(R.string.toast_locked, minutes(sec.lockRemainingMs())))
                        return false
                    }
                    else -> {
                        toast(context, context.getString(R.string.toast_bad_pin))
                        return false
                    }
                }
            }

            val url = intent.getStringExtra(EXTRA_URL)
            if (url != null) {
                if (!UrlPolicy.isValidHomeUrl(url)) {
                    toast(context, context.getString(R.string.toast_invalid_url))
                    return false
                }
                prefs.url = url.trim()
            }
            intent.getStringExtra(EXTRA_NAME)?.let { prefs.propertyName = it.trim().take(60) }
            intent.getStringExtra(EXTRA_RELOAD)?.toIntOrNull()?.let { prefs.reloadMinutes = it.coerceIn(0, 1440) }
            intent.getStringExtra(EXTRA_HOSTS)?.let { prefs.extraHosts = it.trim() }
            intent.getStringExtra(EXTRA_APPS)?.let { v -> prefs.allowedApps = v.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
            intent.getStringExtra(EXTRA_LAUNCHER_IDLE)?.toIntOrNull()?.let { prefs.launcherIdleMinutes = it.coerceIn(0, 240) }
            intent.getStringExtra(EXTRA_MEDIA_IDLE)?.toIntOrNull()?.let { prefs.mediaIdleMinutes = it.coerceIn(0, 240) }
            intent.getStringExtra(EXTRA_SET_PIN)?.let {
                if (SecurityStore.isValidPin(it)) sec.setPin(it)
                else toast(context, context.getString(R.string.err_pin))
            }
            toast(context, context.getString(R.string.toast_saved))
            return true
        } finally {
            ALL.forEach { intent.removeExtra(it) }
        }
    }

    fun minutes(ms: Long): Long = maxOf(1L, (ms + 59_999L) / 60_000L)

    private fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
