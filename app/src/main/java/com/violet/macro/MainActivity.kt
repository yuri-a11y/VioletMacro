package com.violet.macro

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var cfg: Config
    private lateinit var status: TextView
    private lateinit var enableBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cfg = Config(this)
        window.decorView.setBackgroundColor(VC.BG)
        @Suppress("DEPRECATION")
        window.statusBarColor = VC.BG
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val on = MacroService.instance != null
        status.text = if (on) "●  Service on" else "●  Service off"
        status.setTextColor(if (on) VC.GREEN else VC.ORANGE)
        enableBtn.visibility = if (on) View.GONE else View.VISIBLE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(18))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(VC.CARD)
            setStroke(dp(1), Color.rgb(52, 40, 88))
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(14) }
    }

    private fun button(text: String, filled: Boolean, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (filled) setColor(VC.PURPLE) else { setColor(Color.TRANSPARENT); setStroke(dp(1), VC.PURPLE) }
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }
        setOnClickListener { onClick() }
    }

    private fun seek(parent: LinearLayout, name: String, min: Int, max: Int, get: () -> Int, set: (Int) -> Unit, unit: String = " ms") {
        val tv = TextView(this).apply {
            setTextColor(VC.TEXT)
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        }
        fun upd(v: Int) { tv.text = "$name:  $v$unit" }
        upd(get())
        val sb = SeekBar(this).apply {
            this.max = max - min
            progress = get() - min
            progressTintList = ColorStateList.valueOf(VC.PURPLE)
            thumbTintList = ColorStateList.valueOf(VC.LIGHT)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p + min
                    upd(v)
                    if (fromUser) set(v)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        parent.addView(tv)
        parent.addView(sb)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(32))
        }

        root.addView(label("VIOLET MACRO", 24f, VC.TEXT, true))
        root.addView(label("Roblox macros (3 / S)  •  Android", 12f, VC.DIM))

        val c1 = card()
        status = label("", 18f, VC.ORANGE, true)
        c1.addView(status)
        c1.addView(label("The floating panel shows up over the game.", 12f, VC.DIM).apply {
            setPadding(0, dp(4), 0, 0)
        })
        enableBtn = button("Enable accessibility service", true) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        c1.addView(enableBtn)
        c1.addView(button("Show / hide panel", false) {
            val s = MacroService.instance
            if (s == null) toast("Enable the accessibility service first")
            else s.setPanelShown(!s.panelVisible)
        })
        root.addView(c1)

        val c2 = card()
        c2.addView(label("Timings", 14f, VC.TEXT, true))
        seek(c2, "Tap duration", 1, 100, { cfg.keyHold }, { cfg.keyHold = it })
        seek(c2, "Delay after slot 3", 0, 100, { cfg.delayAfter3 }, { cfg.delayAfter3 = it })
        seek(c2, "Delay jump → slot 1", 0, 100, { cfg.delayJumpTo1 }, { cfg.delayJumpTo1 = it })
        seek(c2, "Swipe duration", 20, 500, { cfg.swipeMs }, { cfg.swipeMs = it })
        seek(c2, "Delay after swipe", 0, 500, { cfg.delayAfterSwipe }, { cfg.delayAfterSwipe = it })
        seek(c2, "Delay after kick", 0, 1000, { cfg.delayAfterKick }, { cfg.delayAfterKick = it })
        seek(c2, "Lockout", 50, 1000, { cfg.lockout }, { cfg.lockout = it })
        seek(c2, "Retries when cancelled", 0, 20, { cfg.retries }, { cfg.retries = it }, "")
        c2.addView(label("Android cancels taps while a finger moves on the screen (walking). Retries try the same tap again.", 11f, VC.DIM).apply {
            setPadding(0, dp(4), 0, 0)
        })
        val swCam = Switch(this).apply {
            text = "Camera swipe first"
            setTextColor(VC.TEXT)
            isChecked = cfg.cameraSwipe
            setPadding(0, dp(16), 0, 0)
            setOnCheckedChangeListener { _, on -> cfg.cameraSwipe = on }
        }
        c2.addView(swCam)
        val swKick = Switch(this).apply {
            text = "Kick up first"
            setTextColor(VC.TEXT)
            isChecked = cfg.kickFirst
            setPadding(0, dp(16), 0, 0)
            setOnCheckedChangeListener { _, on -> cfg.kickFirst = on }
        }
        c2.addView(swKick)
        val sw = Switch(this).apply {
            text = "Button also taps slot 3"
            setTextColor(VC.TEXT)
            isChecked = cfg.tapSlot3
            setPadding(0, dp(16), 0, 0)
            setOnCheckedChangeListener { _, on -> cfg.tapSlot3 = on }
        }
        c2.addView(sw)
        c2.addView(label("Recalibrate after changing this.", 11f, VC.DIM).apply { setPadding(0, dp(4), 0, 0) })
        root.addView(c2)

        val cs = card()
        cs.addView(label("Shidou macro (S button)", 14f, VC.TEXT, true))
        cs.addView(label("swipe cam up → hold M1 → slot 3 → jump → technique → slot 3 → slot 2 + T", 11f, VC.DIM).apply {
            setPadding(0, dp(4), 0, 0)
        })
        seek(cs, "Hold M1 (medium kick)", 10, 1000, { cfg.shHold }, { cfg.shHold = it })
        seek(cs, "Delay M1 → slot 3", 0, 300, { cfg.shGap }, { cfg.shGap = it })
        seek(cs, "Delay slot 3 → jump", 0, 100, { cfg.shDelay3Jump }, { cfg.shDelay3Jump = it })
        seek(cs, "Delay jump → technique mode", 0, 100, { cfg.shDelayJumpTech }, { cfg.shDelayJumpTech = it })
        seek(cs, "Delay technique → slot 3 (headbutt)", 0, 200, { cfg.shDelayTech3 }, { cfg.shDelayTech3 = it })
        seek(cs, "Delay slot 3 → 2 + T", 0, 200, { cfg.shDelay3Last }, { cfg.shDelay3Last = it })
        cs.addView(label("S reuses the macro 3 calibration for swipe, kick, slot 3 and jump, plus its tap duration, swipe duration, delay after swipe and lockout (Timings card above). calibrate S only asks for technique mode, slot 2 and T.", 11f, VC.DIM).apply {
            setPadding(0, dp(10), 0, 0)
        })
        root.addView(cs)

        val c3 = card()
        c3.addView(label("How to use", 14f, VC.TEXT, true))
        c3.addView(label(
            "1. Enable the service (button above).\n" +
                "2. Open Roblox in landscape.\n" +
                "3. On the panel tap ⚙ calibrate, then tap where the camera swipe starts and ends, the kick button, slot 3, the jump button and slot 1, in that order.\n" +
                "4. Switch ON. Tapping the \"3\" button runs swipe → kick → slot 3 → jump → slot 1.\n" +
                "5. Drag the panel by the three dots.\n" +
                "6. S button: calibrate 3 first (S reuses its swipe, kick, slot 3 and jump), then tap ⚙ calibrate S and tap technique mode, slot 2 and T, in that order. Tapping \"S\" runs the Shidou combo.",
            12f, VC.DIM
        ).apply { setPadding(0, dp(8), 0, 0); setLineSpacing(0f, 1.2f) })
        root.addView(c3)

        return ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
    }
}
