package com.openkiosk.tv

import android.content.Context

/**
 * Configuración guardada en el dispositivo. Todo vive local: sin nube, sin cuentas.
 * El PIN y los secretos de recuperación viven aparte, en [SecurityStore].
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("openkiosk", Context.MODE_PRIVATE)

    /** URL de la pantalla de bienvenida (solo https). */
    var url: String
        get() = sp.getString(K_URL, "") ?: ""
        set(v) = sp.edit().putString(K_URL, v).apply()

    /** Nombre de la propiedad (se usa en el autenticador y en las pantallas de la app). */
    var propertyName: String
        get() = sp.getString(K_NAME, "") ?: ""
        set(v) = sp.edit().putString(K_NAME, v).apply()

    /** Recarga completa cada N minutos (0 = nunca). */
    var reloadMinutes: Int
        get() = sp.getInt(K_RELOAD, 60)
        set(v) = sp.edit().putInt(K_RELOAD, v).apply()

    /** Dominios extra permitidos además del de la URL, separados por coma. */
    var extraHosts: String
        get() = sp.getString(K_HOSTS, "") ?: ""
        set(v) = sp.edit().putString(K_HOSTS, v).apply()

    val isConfigured: Boolean get() = url.isNotBlank()

    companion object {
        private const val K_URL = "url"
        private const val K_NAME = "property_name"
        private const val K_RELOAD = "reload_minutes"
        private const val K_HOSTS = "extra_hosts"
    }
}
