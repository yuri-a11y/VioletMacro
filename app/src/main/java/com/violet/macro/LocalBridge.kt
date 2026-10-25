package com.violet.macro

import android.os.SystemClock
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

// Tiny HTTP server on 127.0.0.1. The Delta script long-polls /poll and runs the macro when the counter moves.
object LocalBridge {
    const val PORT = 38417
    private const val WAIT_MS = 15000L
    const val DEFAULT_DIR = "/storage/emulated/0/Delta/Workspace"

    private val lock = Object()
    private var server: ServerSocket? = null
    @Volatile private var counter = 0
    @Volatile var lastPoll = 0L
        private set

    // file channel: some executors cannot reach localhost, so the app also writes the trigger into the
    // executor's workspace folder, and the script writes a heartbeat file there that the app reads back
    @Volatile var dir: String = DEFAULT_DIR
    private val writer = Executors.newSingleThreadExecutor()

    private val httpAlive: Boolean
        get() = lastPoll != 0L && SystemClock.uptimeMillis() - lastPoll < WAIT_MS + 8000

    val fileAlive: Boolean
        get() = try {
            val f = File(dir, "violet_alive.txt")
            f.exists() && System.currentTimeMillis() - f.lastModified() < 8000
        } catch (_: Throwable) { false }

    val scriptConnected: Boolean
        get() = httpAlive || fileAlive

    @Synchronized
    fun start() {
        if (server != null) return
        try {
            val s = ServerSocket(PORT, 8, InetAddress.getByName("127.0.0.1"))
            s.reuseAddress = true
            server = s
            Thread {
                while (!s.isClosed) {
                    val c = try { s.accept() } catch (_: Throwable) { break }
                    Thread { handle(c) }.start()
                }
            }.start()
        } catch (_: Throwable) {
            server = null
        }
    }

    @Synchronized
    fun stop() {
        try { server?.close() } catch (_: Throwable) {}
        server = null
        synchronized(lock) { lock.notifyAll() }
    }

    val running: Boolean get() = server != null

    fun trigger() {
        synchronized(lock) {
            counter++
            lock.notifyAll()
        }
        val d = dir
        writer.execute {
            try {
                File(d).mkdirs()
                File(d, "violet_trigger.txt").writeText(System.currentTimeMillis().toString())
            } catch (_: Throwable) {}
        }
    }

    private fun handle(c: Socket) {
        try {
            c.soTimeout = 5000
            val line = c.getInputStream().bufferedReader().readLine() ?: return
            val path = line.split(" ").getOrNull(1) ?: "/"
            val body = if (path.startsWith("/poll")) {
                lastPoll = SystemClock.uptimeMillis()
                val since = path.substringAfter("since=", "").takeWhile { it.isDigit() }.toIntOrNull() ?: -1
                synchronized(lock) {
                    val end = SystemClock.uptimeMillis() + WAIT_MS
                    while (counter <= since && server != null) {
                        val left = end - SystemClock.uptimeMillis()
                        if (left <= 0) break
                        try { lock.wait(left) } catch (_: InterruptedException) { break }
                    }
                    counter.toString()
                }.also { lastPoll = SystemClock.uptimeMillis() }
            } else {
                "violet " + counter
            }
            val bytes = body.toByteArray()
            val out = c.getOutputStream()
            out.write(
                ("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ${bytes.size}\r\n" +
                    "Cache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray()
            )
            out.write(bytes)
            out.flush()
        } catch (_: Throwable) {
        } finally {
            try { c.close() } catch (_: Throwable) {}
        }
    }
}
