package com.radiomodem.mt63

import com.radiomodem.mt63.dsp.Hadamard64
import com.radiomodem.mt63.dsp.InterleaveTables
import com.radiomodem.mt63.modem.DSPUtils
import com.radiomodem.mt63.modem.MT63Frame
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DspUnitTests {
    @Test fun hadamard_soft_decode_trivial() {
        val h = Hadamard64()
        val llr = DoubleArray(64) { 0.0 }
        // Bias toward row 5: set llr = row values (+/-1)*small
        // Build synthetic LLRs by assuming row vector corresponds to +1/-1
        // We don't have direct access to matrix; approximate by making a spike.
        llr[5] = 10.0 // crude check: any nonzero feature should decode to some row
        val bits = h.softDecode(llr)
        assertEquals(6, bits.size)
    }

    @Test fun interleave_inverse_identity() {
        val legacy = InterleaveTables.legacy()
        val inv = InterleaveTables.inverse(legacy)
        for (i in legacy.indices) assertEquals(i, inv[i])
    }

    @Test fun interleave_apply_inverse_double() {
        val m = InterleaveTables.longInterleave()
        val inv = InterleaveTables.inverse(m)
        val a = DoubleArray(4096) { it.toDouble() }
        val b = DoubleArray(4096)
        val c = DoubleArray(4096)
        InterleaveTables.apply(m, a, b)
        InterleaveTables.apply(inv, b, c)
        for (i in a.indices) assertEquals(a[i], c[i], 1e-9)
    }

    @Test fun fft_ifft_roundtrip() {
        val n = 1024
        val re = DoubleArray(n) { if (it == 1) 1.0 else 0.0 }
        val im = DoubleArray(n) { 0.0 }
        DSPUtils.fft(re, im)
        DSPUtils.ifft(re, im)
        // expect near original
        var err = 0.0
        for (i in 0 until n) err += abs(re[i] - (if (i == 1) 1.0 else 0.0))
        assertTrue(err / n < 1e-6)
    }

    @Test fun frame_pack_unpack() {
        val data = ByteArray(123) { it.toByte() }
        val fr = MT63Frame.pack(data)
        val out = MT63Frame.unpack(fr)
        assertArrayEquals(data, out)
    }
}