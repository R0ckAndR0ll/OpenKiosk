package com.openkiosk.tv

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

/** Apps de TV instaladas que el anfitrión puede permitir en el launcher. */
object AppCatalog {

    data class Entry(val pkg: String, val label: String, val banner: Drawable?, val icon: Drawable?)

    /** Apps de streaming conocidas; si el anfitrión no eligió ninguna, se muestran las que estén instaladas. */
    val DEFAULT_STREAMING = listOf(
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "com.disney.disneyplus",
        "com.wbd.stream",
        "com.amazon.amazonvideo.livingroom",
        "com.apple.atve.androidtv.appletv",
        "com.amco.clarovideo_atv",
        "com.spotify.tv.android",
        "com.google.android.youtube.tvmusic",
        "tv.pluto.android",
        "com.paramount.android.pplus",
        "com.crunchyroll.crunchyroid",
    )

    /** Todas las apps que aparecen en el lanzador de Android TV (menos OpenKiosk). */
    fun installed(ctx: Context): List<Entry> {
        val pm = ctx.packageManager
        val byPkg = LinkedHashMap<String, ResolveInfo>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg != ctx.packageName && pkg !in byPkg) byPkg[pkg] = ri
            }
        }
        return byPkg.map { (pkg, ri) ->
            Entry(
                pkg = pkg,
                label = ri.loadLabel(pm).toString(),
                banner = try { ri.activityInfo.loadBanner(pm) } catch (e: Exception) { null },
                icon = try { ri.loadIcon(pm) } catch (e: Exception) { null },
            )
        }.sortedBy { it.label.lowercase() }
    }

    /** Lista que verá el huésped, en el orden elegido por el anfitrión. */
    fun allowed(ctx: Context, prefs: Prefs): List<Entry> {
        val all = installed(ctx).associateBy { it.pkg }
        val chosen = prefs.allowedApps
        val order = if (chosen.isNotEmpty()) chosen else DEFAULT_STREAMING
        return order.mapNotNull { all[it] }
    }

    fun launch(ctx: Context, pkg: String): Boolean {
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
