package com.radiomodem.mt63.modem

import java.io.ByteArrayOutputStream
import kotlin.math.min

object MT63Frame {
    @JvmStatic fun pack(payload: ByteArray): ByteArray {
        val len = payload.size
        require(len <= 0xFFFF) { "payload too large" }
        val out = ByteArrayOutputStream(len + 2)
        out.write((len ushr 8) and 0xFF)
        out.write(len and 0xFF)
        out.write(payload, 0, len)
        return out.toByteArray()
    }
    @JvmStatic fun unpack(frame: ByteArray): ByteArray {
        if (frame.size < 2) return ByteArray(0)
        val len = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        if (frame.size < 2 + len) return ByteArray(0)
        return frame.copyOfRange(2, 2 + len)
    }
}