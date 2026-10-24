package com.violet.macro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

// slot 3 -> jump -> slot 1
class MacroService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MacroService? = null
    }

    private lateinit var wm: WindowManager
    private lateinit var cfg: Config

    private var panel: LinearLayout? = null
    private var toggleView: TextView? = null
    private var fireView: TextView? = null
    private var counterView: TextView? = null
    private var calib: CalibView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var count = 0
    private var lastFire = 0L

    val panelVisible: Boolean get() = panel != null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        cfg = Config(this)
        if (cfg.panelShown) showPanel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        handler.removeCallbacksAndMessages(null)
        hidePanel()
        cancelCalibration()
        instance = null
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun pill(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat()
        setColor(color)
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(2), VC.LIGHT)
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): Pair<Int, Int> {
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        return Pair(dm.widthPixels, dm.heightPixels)
    }

    private fun overlayParams(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    fun setPanelShown(show: Boolean) {
        cfg.panelShown = show
        if (show) showPanel() else hidePanel()
    }

    private fun showPanel() {
        if (panel != null) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(2), dp(8), dp(8))
            background = pill(VC.PANEL, 20, VC.PURPLE)
        }
        val grip = TextView(this).apply {
            text = "• • •"
            textSize = 12f
            setTextColor(VC.DIM)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(10), dp(18), dp(8))
        }
        val tg = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(6), dp(20), dp(6))
            setOnClickListener { setActive(!active) }
        }
        val fb = TextView(this).apply {
            text = "3"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(68)).apply { topMargin = dp(8) }
        }
        val ct = TextView(this).apply {
            text = "0"
            textSize = 11f
            setTextColor(VC.DIM)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        val gear = TextView(this).apply {
            text = "⚙ calibrate"
            textSize = 11f
            setTextColor(VC.LIGHT)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(2))
            setOnClickListener { startCalibration() }
        }
        root.addView(grip)
        root.addView(tg)
        root.addView(fb)
        root.addView(ct)
        root.addView(gear)

        val lp = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        ).apply { x = cfg.panelX; y = cfg.panelY }

        // drag
        var dx = 0f
        var dy = 0f
        var ox = 0
        var oy = 0
        grip.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; ox = lp.x; oy = lp.y }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ox + (e.rawX - dx).toInt()
                    lp.y = oy + (e.rawY - dy).toInt()
                    wm.updateViewLayout(root, lp)
                }
                MotionEvent.ACTION_UP -> { cfg.panelX = lp.x; cfg.panelY = lp.y }
            }
            true
        }

        // fire on release
        fb.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60).start()
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                    fire()
                }
                MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }
            true
        }

        wm.addView(root, lp)
        panel = root
        toggleView = tg
        fireView = fb
        counterView = ct
        refreshLook()
    }

    private fun hidePanel() {
        val p = panel ?: return
        try { wm.removeView(p) } catch (_: Exception) {}
        panel = null
        toggleView = null
        fireView = null
        counterView = null
    }

    private fun setActive(on: Boolean) {
        active = on
        refreshLook()
        panel?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun refreshLook() {
        toggleView?.apply {
            text = if (active) "ON" else "OFF"
            setTextColor(if (active) Color.WHITE else VC.DIM)
            background = pill(if (active) VC.DEEP else VC.OFF, 16, if (active) VC.LIGHT else Color.rgb(70, 56, 112))
        }
        fireView?.apply {
            background = oval(if (active) VC.PURPLE else VC.OFF)
            alpha = if (active) 1f else 0.5f
        }
        counterView?.text = count.toString()
    }

    private fun tap(p: PointF, w: Int, h: Int, start: Long, hold: Long) =
        GestureDescription.StrokeDescription(
            Path().apply { moveTo(p.x * w, p.y * h) },
            start,
            maxOf(1L, hold)
        )

    private fun fire() {
        if (!active) return
        val now = SystemClock.uptimeMillis()
        if (now - lastFire < cfg.lockout) return

        val (w, h) = screenSize()
        val s3 = cfg.point("slot3")
        val jp = cfg.point("jump")
        val s1 = cfg.point("slot1")
        if (jp == null || s1 == null || (cfg.tapSlot3 && s3 == null)) {
            toast("Calibrate first (⚙ calibrate)")
            return
        }
        if (cfg.landscape != (w > h)) {
            toast("Orientation changed, recalibrate")
            return
        }
        lastFire = now

        val hold = cfg.keyHold.toLong()
        val pts = ArrayList<PointF>()
        val gaps = ArrayList<Long>()
        if (cfg.tapSlot3 && s3 != null) { pts.add(s3); gaps.add(cfg.delayAfter3.toLong()) }
        pts.add(jp); gaps.add(cfg.delayJumpTo1.toLong())
        pts.add(s1); gaps.add(0L)
        val lead = if (cfg.tapSlot3) 0L else cfg.delayAfter3.toLong()
        handler.postDelayed({ step(pts, gaps, 0, w, h, hold) }, lead)
        count++
        refreshLook()
    }

    private fun step(pts: List<PointF>, gaps: List<Long>, i: Int, w: Int, h: Int, hold: Long) {
        val g = GestureDescription.Builder().addStroke(tap(pts[i], w, h, 0L, hold)).build()
        dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) {
                if (i + 1 < pts.size) handler.postDelayed({ step(pts, gaps, i + 1, w, h, hold) }, gaps[i])
            }
        }, null)
    }

    fun startCalibration() {
        if (calib != null) return
        val labels = ArrayList<String>()
        val keys = ArrayList<String>()
        if (cfg.tapSlot3) { labels.add("SLOT 3"); keys.add("slot3") }
        labels.add("the JUMP button"); keys.add("jump")
        labels.add("SLOT 1"); keys.add("slot1")

        val view = CalibView(
            this, labels,
            onFinished = { pts ->
                val (w, h) = screenSize()
                pts.forEachIndexed { i, p -> cfg.setPoint(keys[i], p.x / w, p.y / h) }
                cfg.landscape = w > h
                cancelCalibration()
                toast("Calibration saved ✓")
            },
            onCancel = { cancelCalibration() }
        )
        val lp = overlayParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        wm.addView(view, lp)
        calib = view
    }

    private fun cancelCalibration() {
        val v = calib ?: return
        calib = null
        try { wm.removeView(v) } catch (_: Exception) {}
    }
}

// calibration overlay
private class CalibView(
    ctx: Context,
    private val labels: List<String>,
    private val onFinished: (List<PointF>) -> Unit,
    private val onCancel: () -> Unit
) : View(ctx) {

    private val d = ctx.resources.displayMetrics.density
    private val pts = ArrayList<PointF>()
    private val loc = IntArray(2)
    private val cancelRect = RectF()

    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 20f * d; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VC.LIGHT; textSize = 14f * d; textAlign = Paint.Align.CENTER
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f * d; color = VC.LIGHT
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VC.PURPLE }
    private val tag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 12f * d; textAlign = Paint.Align.CENTER
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.argb(175, 8, 6, 18))
        getLocationOnScreen(loc)
        val cx = width / 2f
        val step = pts.size

        c.drawText("Step ${minOf(step + 1, labels.size)} of ${labels.size}", cx, 52f * d, sub)
        labels.getOrNull(step)?.let { c.drawText("Tap on $it", cx, 84f * d, title) }

        for ((i, p) in pts.withIndex()) {
            val x = p.x - loc[0]
            val y = p.y - loc[1]
            c.drawCircle(x, y, 20f * d, ring)
            c.drawCircle(x, y, 4f * d, dot)
            c.drawText("${i + 1}", x, y - 28f * d, tag)
        }

        cancelRect.set(cx - 80f * d, height - 72f * d, cx + 80f * d, height - 28f * d)
        c.drawRoundRect(cancelRect, 14f * d, 14f * d, ring)
        c.drawText("CANCEL", cx, cancelRect.centerY() + 5f * d, tag)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return true
        if (cancelRect.contains(e.x, e.y)) { onCancel(); return true }
        if (pts.size >= labels.size) return true
        pts.add(PointF(e.rawX, e.rawY))
        invalidate()
        if (pts.size == labels.size) {
            val done = ArrayList(pts)
            postDelayed({ onFinished(done) }, 350)
        }
        return true
    }
}
