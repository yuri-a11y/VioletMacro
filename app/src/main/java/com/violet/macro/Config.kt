package com.violet.macro

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PointF

/** Paleta violeta do app (mesma do macro de PC). */
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

/** Configuração persistida. Valores de tempo em ms; posições guardadas como fração da tela (0..1). */
class Config(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("violet_macro", Context.MODE_PRIVATE)

    var keyHold: Int
        get() = sp.getInt("keyHold", 15)
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

    /** true = o botão também toca no slot 3 (o gatilho do PC era apertar o 3). */
    var tapSlot3: Boolean
        get() = sp.getBoolean("tapSlot3", true)
        set(v) { sp.edit().putBoolean("tapSlot3", v).apply() }

    /** Orientação em que as posições foram calibradas. */
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
