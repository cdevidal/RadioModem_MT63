package com.radiomodem.mt63

import com.radiomodem.mt63.modem.MT63Demodulator
import com.radiomodem.mt63.modem.MT63Frame
import com.radiomodem.mt63.modem.MT63Modulator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E2EFecTests {
    @Test fun tx_rx_roundtrip_fec_long() {
        val sr = 48000
        val fft = 1024
        val cp = 96
        val carriersCount = 64
        val step = (fft/2 - 2) / carriersCount
        val carriers = IntArray(carriersCount) { 1 + it*step + 1 }

        val payload = ByteArray(64) { (it*3).toByte() }
        val kiss = MT63Frame.pack(payload)

        val mod = MT63Modulator(sr, fft, cp, carriers, true).apply { setFec(true, "long") }
        val pcm = mod.modulate(kiss)
        val dem = MT63Demodulator(fft, cp, carriers, true).apply { setFec(true, "long") }
        val frames = dem.demodulate(pcm)
        assertTrue(frames.isNotEmpty())
        assertArrayEquals(payload, frames[0])
    }
}

