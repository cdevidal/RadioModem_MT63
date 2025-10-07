package com.radiomodem.mt63.net

import org.junit.Assert.*
import org.junit.Test

class KissCodecTest {
    @Test fun encodeDecodeRoundTrip() {
        val payload = byteArrayOf(0x00, 0x11, KissCodec.FEND, KissCodec.FESC, 0x55, (-1).toByte())
        val esc = KissCodec.escape(payload)
        assertTrue(esc.contains(KissCodec.TFEND) && esc.contains(KissCodec.TFESC))
        val dec = KissCodec.unescape(esc)
        assertArrayEquals(payload, dec)
    }

    @Test fun frameWrapUnwrap() {
        val kissData = "Hello".toByteArray()
        val payload = byteArrayOf(0x00) + kissData
        val slip = KissCodec.escape(payload)
        val frame = byteArrayOf(KissCodec.FEND) + slip + byteArrayOf(KissCodec.FEND)
        // crude unwrap: strip FEND, unescape
        val inner = frame.copyOfRange(1, frame.size-1)
        val decoded = KissCodec.unescape(inner)
        assertEquals(0x00, decoded[0].toInt())
        assertArrayEquals(kissData, decoded.copyOfRange(1, decoded.size))
    }
}

