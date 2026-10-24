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
        status.text = if (on) "●  Serviço ativo" else "●  Serviço desligado"
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

    private fun seek(parent: LinearLayout, name: String, min: Int, max: Int, get: () -> Int, set: (Int) -> Unit) {
        val tv = TextView(this).apply {
            setTextColor(VC.TEXT)
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        }
        fun upd(v: Int) { tv.text = "$name:  $v ms" }
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
        root.addView(label("Roblox 3-Trigger  •  Android", 12f, VC.DIM))

        val c1 = card()
        status = label("", 18f, VC.ORANGE, true)
        c1.addView(status)
        c1.addView(label("O painel flutuante aparece por cima do Roblox / Delta.", 12f, VC.DIM).apply {
            setPadding(0, dp(4), 0, 0)
        })
        enableBtn = button("Ativar serviço de acessibilidade", true) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        c1.addView(enableBtn)
        c1.addView(button("Mostrar / ocultar painel", false) {
            val s = MacroService.instance
            if (s == null) toast("Ative o serviço de acessibilidade primeiro")
            else s.setPanelShown(!s.panelVisible)
        })
        root.addView(c1)

        val c2 = card()
        c2.addView(label("Tempos  (iguais aos do macro de PC)", 14f, VC.TEXT, true))
        seek(c2, "Duração do toque", 1, 100, { cfg.keyHold }, { cfg.keyHold = it })
        seek(c2, "Espera após o slot 3", 0, 100, { cfg.delayAfter3 }, { cfg.delayAfter3 = it })
        seek(c2, "Espera pulo → slot 1", 0, 100, { cfg.delayJumpTo1 }, { cfg.delayJumpTo1 = it })
        seek(c2, "Bloqueio entre disparos", 50, 1000, { cfg.lockout }, { cfg.lockout = it })
        val sw = Switch(this).apply {
            text = "Botão também toca no slot 3"
            setTextColor(VC.TEXT)
            isChecked = cfg.tapSlot3
            setPadding(0, dp(16), 0, 0)
            setOnCheckedChangeListener { _, on -> cfg.tapSlot3 = on }
        }
        c2.addView(sw)
        c2.addView(label("Se mudar essa opção, calibre de novo.", 11f, VC.DIM).apply { setPadding(0, dp(4), 0, 0) })
        root.addView(c2)

        val c3 = card()
        c3.addView(label("Como usar", 14f, VC.TEXT, true))
        c3.addView(label(
            "1. Ative o serviço (botão acima).\n" +
                "2. Abra o Roblox com o Delta, em modo paisagem.\n" +
                "3. No painel, toque em ⚙ calibrar e toque, em ordem, no slot 3, no botão de pulo e no slot 1.\n" +
                "4. Ligue o switch ON. Tocar no botão \"3\" do painel dispara: slot 3 → pulo → slot 1.\n" +
                "5. Arraste o painel pelos três pontinhos pra onde não atrapalhe.",
            12f, VC.DIM
        ).apply { setPadding(0, dp(8), 0, 0); setLineSpacing(0f, 1.2f) })
        root.addView(c3)

        return ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
    }
}
