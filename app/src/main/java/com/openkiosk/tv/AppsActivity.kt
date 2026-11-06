package com.openkiosk.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Launcher del huésped: una fila sobria de apps de streaming. Si en N minutos no abre ninguna,
 * regresa a la bienvenida. ATRÁS también regresa.
 */
class AppsActivity : Activity() {

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private val backToWelcome = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefs = Prefs(this)
        setContentView(R.layout.activity_apps)
        findViewById<TextView>(R.id.apps_property).text = prefs.propertyName.ifBlank { getString(R.string.app_name) }
        buildTiles()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        restartIdleTimer()
    }

    override fun onPause() {
        handler.removeCallbacks(backToWelcome)
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        restartIdleTimer()
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun restartIdleTimer() {
        handler.removeCallbacks(backToWelcome)
        val minutes = prefs.launcherIdleMinutes
        if (minutes > 0) handler.postDelayed(backToWelcome, minutes * 60_000L)
    }

    private fun buildTiles() {
        val row = findViewById<LinearLayout>(R.id.apps_row)
        row.removeAllViews()
        val apps = AppCatalog.allowed(this, prefs)
        findViewById<View>(R.id.apps_empty).visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        for (app in apps) row.addView(makeTile(app))
        val firstTile = (row.getChildAt(0) as? LinearLayout)?.getChildAt(0)
        firstTile?.post { firstTile.requestFocus() }
    }

    private fun makeTile(app: AppCatalog.Entry): View {
        val w = dp(272)
        val h = dp(153)
        val gold = Color.parseColor("#C9A46A")

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = dp(28) }
        }

        val frameBg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.parseColor("#17302F"))
            setStroke(dp(2), Color.TRANSPARENT)
        }
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(w, h)
            background = frameBg
            isFocusable = true
            isFocusableInTouchMode = true
            clipToOutline = true
            contentDescription = app.label
        }
        val image = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            if (app.banner != null) {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageDrawable(app.banner)
            } else {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(40), dp(32), dp(40), dp(32))
                setImageDrawable(app.icon)
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        frame.addView(image)

        val label = TextView(this).apply {
            text = app.label
            setTextColor(Color.parseColor("#8E9A95"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) }
        }

        frame.setOnFocusChangeListener { _, focused ->
            frame.animate().scaleX(if (focused) 1.08f else 1f).scaleY(if (focused) 1.08f else 1f)
                .setDuration(220).start()
            frameBg.setStroke(dp(2), if (focused) gold else Color.TRANSPARENT)
            label.setTextColor(Color.parseColor(if (focused) "#EFE9DD" else "#8E9A95"))
        }
        frame.setOnClickListener { AppCatalog.launch(this, app.pkg) }

        column.addView(frame)
        column.addView(label)
        return column
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}
