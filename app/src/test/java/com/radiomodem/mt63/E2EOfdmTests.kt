package com.radiomodem.mt63

import com.radiomodem.mt63.modem.MT63Demodulator
import com.radiomodem.mt63.modem.MT63Frame
import com.radiomodem.mt63.modem.MT63Modulator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E2EOfdmTests {
    @Test fun tx_rx_roundtrip_small() {
        val sampleRate = 48000
        val fft = 512
        val cp = 64
        val carriersCount = 64
        val step = (fft/2 - 2) / carriersCount
        val carriers = IntArray(carriersCount) { 1 + it*step + 1 }
        val usePilots = true

        val payload = "hello, mesh!".toByteArray()
        val kiss = MT63Frame.pack(payload)

        val mod = MT63Modulator(sampleRate, fft, cp, carriers, usePilots)
        val pcm = mod.modulate(kiss)

        val dem = MT63Demodulator(fft, cp, carriers, usePilots)
        val frames = dem.demodulate(pcm)
        assertTrue(frames.isNotEmpty())
        assertArrayEquals(payload, frames[0])
    }
}

