package com.openkiosk.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Pantalla principal del kiosco: un WebView a pantalla completa con la URL del anfitrión.
 *
 * Acceso a ajustes (protegido por PIN):
 *  - Presionar ATRÁS 5 veces seguidas, o
 *  - Mantener presionado OK/centro 3 segundos, o
 *  - Botón MENÚ / AJUSTES del control (si existe).
 */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private var web: WebView? = null
    private val handler = Handler(Looper.getMainLooper())

    private var loadedUrl: String? = null
    private var triedCache = false
    private var showingSetup = false

    private var backCount = 0
    private var lastBackAt = 0L
    private var centerDownAt = 0L
    private var longPressFired = false

    private val reloadTask = object : Runnable {
        override fun run() {
            if (!triedCache) web?.reload()
            scheduleReload()
        }
    }

    private val retryTask = Runnable {
        triedCache = false
        web?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
        loadHome(force = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        createWebView()
        Provisioning.apply(this, prefs, intent)
        if (!prefs.isConfigured) openSettings(requirePin = false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (Provisioning.apply(this, prefs, intent)) loadHome(force = true)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (prefs.isConfigured) {
            if (loadedUrl != prefs.url || showingSetup) loadHome(force = true)
            scheduleReload()
        } else if (!showingSetup) {
            showingSetup = true
            web?.loadDataWithBaseURL(null, StatusPage.setup(this), "text/html", "utf-8", null)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(reloadTask)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        web?.destroy()
        web = null
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView() {
        web?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        val w = WebView(this)
        w.setBackgroundColor(Color.BLACK)
        w.isFocusable = true
        w.isFocusableInTouchMode = true
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        w.webChromeClient = WebChromeClient()
        w.webViewClient = KioskClient()
        setContentView(w)
        w.requestFocus()
        web = w
        loadedUrl = null
    }

    private inner class KioskClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            return !UrlPolicy.isAllowed(request.url, prefs)
        }

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            !UrlPolicy.isAllowed(Uri.parse(url), prefs)

        override fun onPageFinished(view: WebView, url: String?) {
            if (url != null && url.startsWith("https")) showingSetup = false
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) onLoadFailed()
        }

        @Deprecated("Deprecated in Java")
        override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
            onLoadFailed()
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            // El motor del navegador se cayó: lo recreamos y volvemos a cargar.
            handler.post {
                createWebView()
                loadHome(force = true)
            }
            return true
        }
    }

    fun loadHome(force: Boolean = false) {
        val url = prefs.url
        if (url.isBlank()) return
        val w = web ?: return
        if (!force && loadedUrl == url) return
        loadedUrl = url
        w.loadUrl(url)
    }

    /** Sin red o error: primero intenta la última copia guardada; si no hay, muestra "conectando". */
    private fun onLoadFailed() {
        val w = web ?: return
        if (!triedCache) {
            triedCache = true
            w.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            w.loadUrl(prefs.url)
        } else {
            w.loadDataWithBaseURL(null, StatusPage.offline(this), "text/html", "utf-8", null)
        }
        handler.removeCallbacks(retryTask)
        handler.postDelayed(retryTask, RETRY_MS)
    }

    private fun scheduleReload() {
        handler.removeCallbacks(reloadTask)
        val minutes = prefs.reloadMinutes
        if (minutes > 0) handler.postDelayed(reloadTask, minutes * 60_000L)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (event.action == KeyEvent.ACTION_UP) onBackTap()
                return true // el huésped no puede salir con ATRÁS
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                if (event.action == KeyEvent.ACTION_UP) openSettings(requirePin = true)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                val now = SystemClock.uptimeMillis()
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        centerDownAt = now
                        longPressFired = false
                    } else if (!longPressFired && now - centerDownAt >= LONG_PRESS_MS) {
                        longPressFired = true
                        openSettings(requirePin = true)
                        return true
                    }
                } else if (event.action == KeyEvent.ACTION_UP && longPressFired) {
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun onBackTap() {
        val now = SystemClock.uptimeMillis()
        backCount = if (now - lastBackAt < BACK_WINDOW_MS) backCount + 1 else 1
        lastBackAt = now
        if (backCount >= BACK_TAPS) {
            backCount = 0
            openSettings(requirePin = true)
        }
    }

    private fun openSettings(requirePin: Boolean) {
        startActivity(
            Intent(this, SettingsActivity::class.java)
                .putExtra(SettingsActivity.EXTRA_REQUIRE_PIN, requirePin)
        )
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    companion object {
        private const val RETRY_MS = 30_000L
        private const val LONG_PRESS_MS = 3_000L
        private const val BACK_WINDOW_MS = 800L
        private const val BACK_TAPS = 5
    }
}
