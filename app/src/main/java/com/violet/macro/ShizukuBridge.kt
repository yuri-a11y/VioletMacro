package com.violet.macro

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

// Talks to the Shizuku app. The injector itself lives in Shizuku's shell process.
object ShizukuBridge {
    private const val REQ = 4711
    private const val SERVICE_VERSION = 2

    @Volatile var injector: IInjector? = null
    @Volatile var info: String = ""
    @Volatile private var binding = false
    private var appCtx: Context? = null
    private var inited = false

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            binding = false
            if (binder == null || !binder.pingBinder()) return
            val i = IInjector.Stub.asInterface(binder)
            info = try { i.info() } catch (t: Throwable) { "info failed: $t" }
            injector = i
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            injector = null
            binding = false
        }
    }

    private val onBinder = Shizuku.OnBinderReceivedListener { appCtx?.let { bind(it) } }
    private val onDead = Shizuku.OnBinderDeadListener { injector = null; binding = false }
    private val onPerm = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQ && result == PackageManager.PERMISSION_GRANTED) appCtx?.let { bind(it) }
    }

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        if (inited) return
        inited = true
        try {
            Shizuku.addBinderReceivedListenerSticky(onBinder)
            Shizuku.addBinderDeadListener(onDead)
            Shizuku.addRequestPermissionResultListener(onPerm)
        } catch (_: Throwable) {}
    }

    fun running(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun granted(): Boolean = try {
        running() && !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun requestPermission() {
        try { Shizuku.requestPermission(REQ) } catch (_: Throwable) {}
    }

    fun bind(ctx: Context) {
        if (injector != null || binding || !granted()) return
        binding = true
        try {
            val args = Shizuku.UserServiceArgs(ComponentName(ctx.packageName, InjectorService::class.java.name))
                .processNameSuffix("injector")
                .daemon(false)
                .debuggable(false)
                .version(SERVICE_VERSION)
            Shizuku.bindUserService(args, conn)
        } catch (_: Throwable) {
            binding = false
        }
    }

    // text for the settings screen + whether it is good to go
    fun status(): Pair<String, Boolean> = when {
        !running() -> Pair("Shizuku is not running. Open the Shizuku app and start it.", false)
        !granted() -> Pair("Shizuku is running. Tap Connect and allow this app.", false)
        injector == null -> Pair("Allowed. Starting the injector... tap Connect again if this stays.", false)
        info.startsWith("ok") -> Pair("Connected: $info", true)
        else -> Pair("Injector error: $info", false)
    }
}
