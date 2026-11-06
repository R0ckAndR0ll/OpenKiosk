package com.openkiosk.tv

import android.net.Uri

/**
 * Lista blanca de navegación: el kiosco solo abre el dominio configurado (y sus subdominios)
 * más los dominios extra que el anfitrión autorice. Cualquier otro destino se bloquea, para que
 * ni un huésped ni una página comprometida puedan "pasear" el navegador fuera del sitio.
 */
object UrlPolicy {

    fun isValidHomeUrl(url: String): Boolean {
        val u = try {
            Uri.parse(url.trim())
        } catch (e: Exception) {
            return false
        }
        return u.scheme.equals("https", ignoreCase = true) && !u.host.isNullOrBlank()
    }

    fun isAllowed(uri: Uri, prefs: Prefs): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "about" || scheme == "data" || scheme == "blob") return true
        if (scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return allowedHosts(prefs).any { host == it || host.endsWith(".$it") }
    }

    fun allowedHosts(prefs: Prefs): List<String> {
        val home = Uri.parse(prefs.url).host?.lowercase()
        val extras = prefs.extraHosts
            .split(',', ';', ' ')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        return listOfNotNull(home) + extras
    }
}
