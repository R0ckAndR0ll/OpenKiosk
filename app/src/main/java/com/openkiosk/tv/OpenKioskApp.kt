package com.openkiosk.tv

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** Lleva la cuenta de si alguna pantalla de OpenKiosk está al frente. */
class OpenKioskApp : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) { KioskState.resumed++ }
            override fun onActivityPaused(a: Activity) { KioskState.resumed = maxOf(0, KioskState.resumed - 1) }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        })
    }
}

object KioskState {
    @Volatile var resumed = 0
    val inForeground: Boolean get() = resumed > 0
}
