package com.openkiosk.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Abre el kiosco al encender la TV. En Android 10+ el sistema puede bloquear este arranque;
 * por eso lo más confiable es usar OpenKiosk como launcher (pantalla de inicio). Ver README.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val launch = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launch)
            } catch (e: Exception) {
                // Si el sistema lo bloquea, el launcher (HOME) se encarga.
            }
        }
    }
}
