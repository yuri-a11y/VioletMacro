package com.violet.macro

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PointF

object VC {
    val BG = Color.rgb(13, 9, 28)
    val CARD = Color.rgb(28, 20, 52)
    val PANEL = Color.argb(240, 24, 17, 44)
    val PURPLE = Color.rgb(139, 92, 246)
    val LIGHT = Color.rgb(167, 139, 250)
    val DEEP = Color.rgb(109, 40, 217)
    val TEXT = Color.rgb(240, 235, 255)
    val DIM = Color.rgb(150, 138, 185)
    val GREEN = Color.rgb(74, 222, 128)
    val ORANGE = Color.rgb(251, 146, 60)
    val OFF = Color.rgb(44, 34, 74)
}

// times in ms, points stored as screen fractions
class Config(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("violet_macro", Context.MODE_PRIVATE)

    var keyHold: Int
        get() = sp.getInt("keyHold", 30)
        set(v) { sp.edit().putInt("keyHold", v).apply() }

    var delayAfter3: Int
        get() = sp.getInt("delayAfter3", 10)
        set(v) { sp.edit().putInt("delayAfter3", v).apply() }

    var delayJumpTo1: Int
        get() = sp.getInt("delayJumpTo1", 0)
        set(v) { sp.edit().putInt("delayJumpTo1", v).apply() }

    var lockout: Int
        get() = sp.getInt("lockout", 250)
        set(v) { sp.edit().putInt("lockout", v).apply() }

    var tapSlot3: Boolean
        get() = sp.getBoolean("tapSlot3", true)
        set(v) { sp.edit().putBoolean("tapSlot3", v).apply() }

    var cameraSwipe: Boolean
        get() = sp.getBoolean("cameraSwipe", true)
        set(v) { sp.edit().putBoolean("cameraSwipe", v).apply() }

    var swipeMs: Int
        get() = sp.getInt("swipeMs", 120)
        set(v) { sp.edit().putInt("swipeMs", v).apply() }

    var delayAfterSwipe: Int
        get() = sp.getInt("delayAfterSwipe", 60)
        set(v) { sp.edit().putInt("delayAfterSwipe", v).apply() }

    var kickFirst: Boolean
        get() = sp.getBoolean("kickFirst", true)
        set(v) { sp.edit().putBoolean("kickFirst", v).apply() }

    var delayAfterKick: Int
        get() = sp.getInt("delayAfterKick", 150)
        set(v) { sp.edit().putInt("delayAfterKick", v).apply() }

    // redo a tap/swipe the system cancelled (a real finger moving on the screen cancels injected ones)
    var retries: Int
        get() = sp.getInt("retries", 8)
        set(v) { sp.edit().putInt("retries", v).apply() }

    // Shidou macro: how long to hold M1, and the pause between steps
    var shHold: Int
        get() = sp.getInt("shHold", 50)
        set(v) { sp.edit().putInt("shHold", v).apply() }

    var shGap: Int
        get() = sp.getInt("shGap", 150)
        set(v) { sp.edit().putInt("shGap", v).apply() }

    // Shidou: pause between slot 3 and the jump tap (keep it tiny, like macro 3)
    var shDelay3Jump: Int
        get() = sp.getInt("shDelay3Jump", 10)
        set(v) { sp.edit().putInt("shDelay3Jump", v).apply() }

    // Shidou: jump -> technique mode, wait for the headbutt, last slot 3 -> 2 + T
    var shDelayJumpTech: Int
        get() = sp.getInt("shDelayJumpTech", 0)
        set(v) { sp.edit().putInt("shDelayJumpTech", v).apply() }

    var shDelayTech3: Int
        get() = sp.getInt("shDelayTech3", 25)
        set(v) { sp.edit().putInt("shDelayTech3", v).apply() }

    var shDelay3Last: Int
        get() = sp.getInt("shDelay3Last", 35)
        set(v) { sp.edit().putInt("shDelay3Last", v).apply() }

    var landscape: Boolean
        get() = sp.getBoolean("landscape", true)
        set(v) { sp.edit().putBoolean("landscape", v).apply() }

    var panelX: Int
        get() = sp.getInt("panelX", 48)
        set(v) { sp.edit().putInt("panelX", v).apply() }

    var panelY: Int
        get() = sp.getInt("panelY", 160)
        set(v) { sp.edit().putInt("panelY", v).apply() }

    var panelShown: Boolean
        get() = sp.getBoolean("panelShown", true)
        set(v) { sp.edit().putBoolean("panelShown", v).apply() }

    fun point(name: String): PointF? {
        if (!sp.contains("$name.x")) return null
        return PointF(sp.getFloat("$name.x", 0f), sp.getFloat("$name.y", 0f))
    }

    fun setPoint(name: String, x: Float, y: Float) {
        sp.edit().putFloat("$name.x", x).putFloat("$name.y", y).apply()
    }
}
