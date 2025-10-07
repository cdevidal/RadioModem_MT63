package com.radiomodem.mt63.net

object KissCodec {
    const val FEND: Byte = 0xC0.toByte()
    const val FESC: Byte = 0xDB.toByte()
    const val TFEND: Byte = 0xDC.toByte()
    const val TFESC: Byte = 0xDD.toByte()

    fun escape(payload: ByteArray): ByteArray {
        val out = ArrayList<Byte>(payload.size + 8)
        for (b in payload) {
            when (b) {
                FEND -> { out += FESC; out += TFEND }
                FESC -> { out += FESC; out += TFESC }
                else -> out += b
            }
        }
        return out.toByteArray()
    }

    fun unescape(slip: ByteArray): ByteArray {
        val out = ArrayList<Byte>(slip.size)
        var esc = false
        for (b in slip) {
            if (!esc) {
                if (b == FESC) { esc = true; continue }
                out += b
            } else {
                out += when (b) {
                    TFEND -> FEND
                    TFESC -> FESC
                    else -> b
                }
                esc = false
            }
        }
        return out.toByteArray()
    }
}

