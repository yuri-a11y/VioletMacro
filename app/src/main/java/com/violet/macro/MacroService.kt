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

// macro 3: swipe -> kick -> slot 3 -> jump -> slot 1
// macro S (Shidou): camera swipe up -> hold M1 -> slot 3 + jump (tight) -> technique -> slot 3 -> slot 2 + T together
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
    private var fire2View: TextView? = null
    private var counterView: TextView? = null
    private var calib: CalibView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var count = 0
    private var lastFire = 0L
    @Volatile private var busy = false

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
        busy = false
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
        fun roundBtn(t: String) = TextView(this).apply {
            text = t
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(60), dp(60)).apply {
                leftMargin = dp(4); rightMargin = dp(4)
            }
        }
        val fb = roundBtn("3")
        val fb2 = roundBtn("S")
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
            addView(fb)
            addView(fb2)
        }
        val ct = TextView(this).apply {
            text = "0"
            textSize = 11f
            setTextColor(VC.DIM)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        fun gearLink(t: String, onTap: () -> Unit) = TextView(this).apply {
            text = t
            textSize = 11f
            setTextColor(VC.LIGHT)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(2))
            setOnClickListener { onTap() }
        }
        val gearRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(gearLink("⚙ calibrate 3") { startCalibration(false) })
            addView(gearLink("⚙ calibrate S") { startCalibration(true) })
        }
        root.addView(grip)
        root.addView(tg)
        root.addView(btnRow)
        root.addView(ct)
        root.addView(gearRow)

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
        fun bindFire(btn: View, action: () -> Unit) {
            btn.setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60).start()
                    MotionEvent.ACTION_UP -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                        action()
                    }
                    MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                }
                true
            }
        }
        bindFire(fb) { fire() }
        bindFire(fb2) { fireShidou() }

        wm.addView(root, lp)
        panel = root
        toggleView = tg
        fireView = fb
        fire2View = fb2
        counterView = ct
        refreshLook()
    }

    private fun hidePanel() {
        val p = panel ?: return
        try { wm.removeView(p) } catch (_: Exception) {}
        panel = null
        toggleView = null
        fireView = null
        fire2View = null
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
        for (b in listOf(fireView, fire2View)) b?.apply {
            background = oval(if (active) VC.PURPLE else VC.OFF)
            alpha = if (active) 1f else 0.5f
        }
        counterView?.text = count.toString()
    }

    private fun fire() {
        if (!active || busy) return
        val now = SystemClock.uptimeMillis()
        if (now - lastFire < cfg.lockout) return

        val (w, h) = screenSize()
        val cs = cfg.point("camStart")
        val ce = cfg.point("camEnd")
        val kk = cfg.point("kick")
        val s3 = cfg.point("slot3")
        val jp = cfg.point("jump")
        val s1 = cfg.point("slot1")
        if (jp == null || s1 == null || (cfg.tapSlot3 && s3 == null) || (cfg.kickFirst && kk == null) ||
            (cfg.cameraSwipe && (cs == null || ce == null))
        ) {
            toast("Calibrate first (⚙ calibrate)")
            return
        }
        if (cfg.landscape != (w > h)) {
            toast("Orientation changed, recalibrate")
            return
        }
        lastFire = now

        val hold = cfg.keyHold.toLong()
        val acts = ArrayList<Act>()
        if (cfg.cameraSwipe && cs != null && ce != null) acts.add(Act(cs, ce, cfg.swipeMs.toLong(), cfg.delayAfterSwipe.toLong()))
        if (cfg.kickFirst && kk != null) acts.add(Act(kk, null, hold, cfg.delayAfterKick.toLong()))
        if (cfg.tapSlot3 && s3 != null) acts.add(Act(s3, null, hold, cfg.delayAfter3.toLong()))
        acts.add(Act(jp, null, hold, cfg.delayJumpTo1.toLong()))
        acts.add(Act(s1, null, hold, 0L))
        val lead = if (cfg.cameraSwipe || cfg.kickFirst || cfg.tapSlot3) 0L else cfg.delayAfter3.toLong()
        busy = true
        handler.postDelayed({ step(acts, 0, w, h) }, lead)
        count++
        refreshLook()
    }

    // Shidou: camera swipe up -> hold M1 -> slot 3 + jump (tight) -> technique mode -> slot 3 -> slot 2 + T together
    private fun fireShidou() {
        if (!active || busy) return
        val now = SystemClock.uptimeMillis()
        if (now - lastFire < cfg.lockout) return

        val (w, h) = screenSize()
        val cs = cfg.point("shCamStart")
        val ce = cfg.point("shCamEnd")
        val m1 = cfg.point("shM1")
        val s3 = cfg.point("shS3")
        val jp = cfg.point("shJump")
        val tech = cfg.point("shTech")
        val s2 = cfg.point("shS2")
        val tk = cfg.point("shT")
        if (cs == null || ce == null || m1 == null || s3 == null || jp == null || tech == null || s2 == null || tk == null) {
            toast("Calibrate S first (⚙ calibrate S)")
            return
        }
        if (cfg.landscape != (w > h)) {
            toast("Orientation changed, recalibrate")
            return
        }
        lastFire = now

        val tap = cfg.keyHold.toLong()
        val gap = cfg.shGap.toLong()
        val acts = listOf(
            Act(cs, ce, cfg.swipeMs.toLong(), cfg.delayAfterSwipe.toLong()),
            Act(m1, null, cfg.shHold.toLong(), gap),
            Act(s3, null, tap, cfg.shDelay3Jump.toLong()),
            Act(jp, null, tap, gap),
            Act(tech, null, tap, gap),
            Act(s3, null, tap, gap),
            Act(s2, null, tap, 0L, tk)
        )
        busy = true
        handler.post { step(acts, 0, w, h) }
        count++
        refreshLook()
    }

    private fun step(acts: List<Act>, i: Int, w: Int, h: Int) {
        val a = acts[i]
        val end = a.to
        val path = Path().apply {
            moveTo(a.from.x * w, a.from.y * h)
            if (end != null) lineTo(end.x * w, end.y * h)
        }
        val gb = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, maxOf(1L, a.dur)))
        val ex = a.extra
        if (ex != null) {
            val path2 = Path().apply { moveTo(ex.x * w, ex.y * h) }
            gb.addStroke(GestureDescription.StrokeDescription(path2, 0L, maxOf(1L, a.dur)))
        }
        val g = gb.build()
        val ok = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) {
                if (i + 1 < acts.size) handler.postDelayed({ step(acts, i + 1, w, h) }, a.gap)
                else busy = false
            }
            override fun onCancelled(d: GestureDescription?) { busy = false }
        }, null)
        if (!ok) busy = false
    }

    fun startCalibration(shidou: Boolean = false) {
        if (calib != null) return
        val labels = ArrayList<String>()
        val keys = ArrayList<String>()
        if (shidou) {
            labels.add("where the camera swipe (UP) STARTS"); keys.add("shCamStart")
            labels.add("where the camera swipe (UP) ENDS"); keys.add("shCamEnd")
            labels.add("the M1 button"); keys.add("shM1")
            labels.add("SLOT 3"); keys.add("shS3")
            labels.add("the JUMP button"); keys.add("shJump")
            labels.add("the TECHNIQUE MODE button"); keys.add("shTech")
            labels.add("SLOT 2"); keys.add("shS2")
            labels.add("the T button"); keys.add("shT")
        } else {
            if (cfg.cameraSwipe) {
                labels.add("where the camera swipe STARTS"); keys.add("camStart")
                labels.add("where the camera swipe ENDS"); keys.add("camEnd")
            }
            if (cfg.kickFirst) { labels.add("the KICK button"); keys.add("kick") }
            if (cfg.tapSlot3) { labels.add("SLOT 3"); keys.add("slot3") }
            labels.add("the JUMP button"); keys.add("jump")
            labels.add("SLOT 1"); keys.add("slot1")
        }

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

// one tap (to == null) or one swipe; extra = a second finger tapping at the same time
private class Act(val from: PointF, val to: PointF?, val dur: Long, val gap: Long, val extra: PointF? = null)

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
