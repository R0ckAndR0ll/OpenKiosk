package com.openkiosk.tv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Regresa a la bienvenida cuando el huésped deja una app en pausa o sin sonido durante N minutos
 * (por ejemplo, se quedó dormido y Netflix preguntó "¿Sigues viendo?").
 *
 * Privacidad: solo se lee si algo se está REPRODUCIENDO (sí/no). Nunca se leen títulos, series ni
 * notificaciones, y nada se guarda ni se envía.
 *
 * Android solo permite esta lectura a un "servicio de notificaciones" autorizado. En Google TV se
 * autoriza una vez por ADB:
 *   adb shell cmd notification allow_listener com.openkiosk.tv/com.openkiosk.tv.MediaWatcherService
 */
class MediaWatcherService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastActive = SystemClock.elapsedRealtime()

    private val tick = object : Runnable {
        override fun run() {
            check()
            handler.postDelayed(this, CHECK_MS)
        }
    }

    override fun onListenerConnected() {
        lastActive = SystemClock.elapsedRealtime()
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, CHECK_MS)
    }

    override fun onListenerDisconnected() {
        handler.removeCallbacks(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun check() {
        val now = SystemClock.elapsedRealtime()
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        // TV apagada o OpenKiosk al frente: no hay nada que vigilar.
        if (!power.isInteractive || KioskState.inForeground) {
            lastActive = now
            return
        }
        if (isPlaying()) {
            lastActive = now
            return
        }
        val prefs = Prefs(this)
        val limit = prefs.mediaIdleMinutes * 60_000L
        if (prefs.isConfigured && limit > 0 && now - lastActive >= limit) {
            lastActive = now
            try {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // Si el sistema bloquea el arranque en segundo plano, ver README (SYSTEM_ALERT_WINDOW).
            }
        }
    }

    private fun isPlaying(): Boolean {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.isMusicActive) return true
        return try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            msm.getActiveSessions(ComponentName(this, MediaWatcherService::class.java)).any {
                when (it.playbackState?.state) {
                    PlaybackState.STATE_PLAYING,
                    PlaybackState.STATE_BUFFERING,
                    PlaybackState.STATE_FAST_FORWARDING,
                    PlaybackState.STATE_REWINDING -> true
                    else -> false
                }
            }
        } catch (e: SecurityException) {
            false
        }
    }

    companion object {
        private const val CHECK_MS = 30_000L

        fun isEnabled(ctx: Context): Boolean {
            val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
            return flat.contains(ComponentName(ctx, MediaWatcherService::class.java).flattenToString()) ||
                flat.contains(ComponentName(ctx, MediaWatcherService::class.java).flattenToShortString())
        }
    }
}
