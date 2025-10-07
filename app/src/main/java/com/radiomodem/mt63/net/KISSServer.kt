package com.radiomodem.mt63.net

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.radiomodem.mt63.modem.MT63Modem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Vector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KISSServer(context: Context, private val modem: MT63Modem) : MT63Modem.KissRxSink {
    companion object {
        private const val TAG = "KISSServer"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private var server: ServerSocket? = null
    private var exec: ExecutorService? = null
    private val clients: MutableList<Client> = Vector()
    @Volatile private var running = false

    private data class Client(val s: Socket, val maxFrame: Int) {
        var out: BufferedOutputStream? = null
        fun close() { try { s.close() } catch (_: Exception) {} }
    }

    fun start() {
        if (running) return
        running = true
        val bind = prefs.getString("pref_kiss_bind", "127.0.0.1") ?: "127.0.0.1"
        val port = (prefs.getString("pref_kiss_port", "8100") ?: "8100").toIntOrNull() ?: 8100
        val backlog = (prefs.getString("pref_kiss_backlog", "4") ?: "4").toIntOrNull() ?: 4
        val maxClients = (prefs.getString("pref_kiss_maxclients", "4") ?: "4").toIntOrNull() ?: 4
        val maxFrame = (prefs.getString("pref_kiss_maxframe", "1500") ?: "1500").toIntOrNull() ?: 1500
        val keepalive = prefs.getBoolean("pref_kiss_keepalive", true)
        val nodelay = prefs.getBoolean("pref_kiss_nodelay", true)

        exec = Executors.newCachedThreadPool()
        server = ServerSocket(port, backlog, InetAddress.getByName(bind))

        exec?.execute {
            try {
                while (running) {
                    val s = server?.accept() ?: break
                    if (!running) { s.close(); break }
                    if (clients.size >= maxClients) { try { s.close() } catch (_: Exception) {}; continue }
                    s.tcpNoDelay = nodelay
                    s.keepAlive = keepalive
                    s.soTimeout = 5000
                    val c = Client(s, maxFrame)
                    clients.add(c)
                    exec?.execute { handle(c) }
                }
            } catch (_: Exception) {
                // server closed
            }
        }

        modem.setKissRxSink(this)
        Log.i(TAG, "KISS/TCP listening on $bind:$port")
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        server = null
        clients.forEach { it.close() }
        clients.clear()
        exec?.shutdownNow()
        exec = null
        modem.setKissRxSink(null)
        Log.i(TAG, "KISS/TCP stopped")
    }

    private fun handle(c: Client) {
        Log.i(TAG, "Client: ${c.s.remoteSocketAddress}")
        try {
            val input = BufferedInputStream(c.s.getInputStream())
            c.out = BufferedOutputStream(c.s.getOutputStream())
            val frame = ByteArrayOutputStream(2048)
            var inFrame = false
            while (running && !c.s.isClosed) {
                val b = input.read()
                if (b == -1) break
                val by = b.toByte()
                when (by) {
                    KissCodec.FEND -> {
                        if (inFrame) {
                            val raw = frame.toByteArray()
                            frame.reset()
                            inFrame = false
                            if (raw.isNotEmpty()) processIncoming(raw, c)
                        } else {
                            inFrame = true; frame.reset()
                        }
                    }
                    else -> if (inFrame) frame.write(by.toInt())
                }
            }
        } catch (_: Exception) {
            // ignore
        } finally {
            clients.remove(c)
            c.close()
            Log.i(TAG, "Client disconnected")
        }
    }

    private fun processIncoming(rawEscaped: ByteArray, c: Client) {
        val raw = KissCodec.unescape(rawEscaped)
        if (raw.isEmpty()) return
        val type = raw[0].toInt() and 0xFF
        val port = (type shr 4) and 0x0F
        val cmd = type and 0x0F
        if (cmd == 0x00) { // DATA
            if (port != 0) return
            val kiss = raw.copyOfRange(1, raw.size)
            modem.transmitFrame(kiss)
        } else {
            if (raw.size >= 2) {
                val val8 = raw[1].toInt() and 0xFF
                when (cmd) {
                    0x01 -> modem.setTxDelayMs(val8 * 10) // TXDELAY in 10ms units
                    0x02 -> modem.setPersist(val8)        // PERSIST (0-255)
                    0x03 -> modem.setSlotTimeMs(val8 * 10)// SLOTTIME in 10ms
                    0x04 -> modem.setTxTailMs(val8 * 10)  // TXTAIL in 10ms
                    // others ignored
                }
            }
        }
    }

    override fun onKissBytesFromModem(kiss: ByteArray) {
        // prefix with 0x00 (DATA on port 0), escape and wrap with FEND
        val payload = ByteArray(kiss.size + 1)
        payload[0] = 0x00
        System.arraycopy(kiss, 0, payload, 1, kiss.size)
        val slip = KissCodec.escape(payload)
        val out = ByteArray(slip.size + 2)
        out[0] = KissCodec.FEND
        System.arraycopy(slip, 0, out, 1, slip.size)
        out[out.size - 1] = KissCodec.FEND
        for (cl in clients) {
            try { cl.out?.write(out); cl.out?.flush() } catch (_: Exception) {}
        }
    }
}

