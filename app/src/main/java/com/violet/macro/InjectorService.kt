package com.violet.macro

import android.os.Process
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import java.lang.reflect.Method
import kotlin.system.exitProcess

// Runs in the Shizuku process (shell uid), so it may inject real input events.
// No Context here and nothing from the app UI.
class InjectorService() : IInjector.Stub() {

    private var im: Any? = null
    private var inject: Method? = null
    private var injectArgs = 2
    private var error = ""

    init {
        try {
            val cls = try {
                Class.forName("android.hardware.input.InputManagerGlobal")
            } catch (_: ClassNotFoundException) {
                Class.forName("android.hardware.input.InputManager")
            }
            val inst = cls.getDeclaredMethod("getInstance").invoke(null)
            im = inst
            val two = runCatching {
                inst!!.javaClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            }.getOrNull()
            if (two != null) {
                inject = two
                injectArgs = 2
            } else {
                inject = inst!!.javaClass.getMethod(
                    "injectInputEvent", InputEvent::class.java,
                    Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                )
                injectArgs = 3
            }
        } catch (t: Throwable) {
            error = t.toString()
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun info(): String =
        if (error.isEmpty()) "ok (uid ${Process.myUid()})" else error

    override fun tap(x: Float, y: Float, holdMs: Int, deviceId: Int, pointerId: Int): Boolean {
        val t0 = SystemClock.uptimeMillis()
        val down = send(MotionEvent.ACTION_DOWN, t0, t0, x, y, deviceId, pointerId)
        sleepMs(holdMs.toLong())
        val up = send(MotionEvent.ACTION_UP, t0, SystemClock.uptimeMillis(), x, y, deviceId, pointerId)
        return down && up
    }

    override fun swipe(
        x1: Float, y1: Float, x2: Float, y2: Float, durMs: Int, deviceId: Int, pointerId: Int
    ): Boolean {
        val t0 = SystemClock.uptimeMillis()
        var ok = send(MotionEvent.ACTION_DOWN, t0, t0, x1, y1, deviceId, pointerId)
        val steps = maxOf(1, durMs / 8)
        for (i in 1..steps) {
            sleepMs((durMs / steps).toLong())
            val f = i.toFloat() / steps
            ok = send(
                MotionEvent.ACTION_MOVE, t0, SystemClock.uptimeMillis(),
                x1 + (x2 - x1) * f, y1 + (y2 - y1) * f, deviceId, pointerId
            ) && ok
        }
        ok = send(MotionEvent.ACTION_UP, t0, SystemClock.uptimeMillis(), x2, y2, deviceId, pointerId) && ok
        return ok
    }

    private fun sleepMs(ms: Long) {
        if (ms > 0) try { Thread.sleep(ms) } catch (_: InterruptedException) {}
    }

    private fun send(action: Int, downTime: Long, now: Long, x: Float, y: Float, dev: Int, pid: Int): Boolean {
        val m = inject ?: return false
        val target = im ?: return false
        val props = arrayOf(MotionEvent.PointerProperties().apply {
            id = pid
            toolType = MotionEvent.TOOL_TYPE_FINGER
        })
        val coords = arrayOf(MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = 1f
            size = 1f
        })
        val ev = MotionEvent.obtain(
            downTime, now, action, 1, props, coords, 0, 0, 1f, 1f,
            dev, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        try {
            MotionEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(ev, 0)
        } catch (_: Throwable) {}
        return try {
            val r = if (injectArgs == 2) m.invoke(target, ev, 0) else m.invoke(target, ev, 0, -1)
            r as? Boolean ?: false
        } catch (_: Throwable) {
            false
        } finally {
            ev.recycle()
        }
    }
}
