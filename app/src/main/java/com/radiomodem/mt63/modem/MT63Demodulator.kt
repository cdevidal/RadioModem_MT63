package com.radiomodem.mt63.modem

import com.radiomodem.mt63.dsp.Hadamard64
import com.radiomodem.mt63.dsp.InterleaveTables
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Minimal MT63-like demodulator skeleton.
 *  Implements OFDM symbol slicing via FFT, extracts QPSK symbols from known carriers.
 *  A full implementation would include timing recovery, PLL, soft FEC, interleave depth, etc. */
class MT63Demodulator(



    private val fftSize: Int,
    private val cpLen: Int,
    private val carriers: IntArray,
    private val usePilots: Boolean
) {
    private val re = DoubleArray(fftSize)
    private val lastSpec = FloatArray(carriers.size)
    private val im = DoubleArray(fftSize)
    private val had = Hadamard64()
    private var blockSym = 0
    private val blockLlr = DoubleArray(64*64)
    private var lastSnr = 0.0
    fun lastSnrDb(): Double = lastSnr

    private var fecEnabled = false
    private var interleaveMode = "legacy"
    fun setFec(enabled: Boolean, mode: String) { fecEnabled = enabled; interleaveMode = mode }
    private fun permuteIndex(i: Int): Int = when (interleaveMode) { "short" -> (i * 5) and 63, "long" -> (i * 17 + 13) and 63, else -> i and 63 }
    private fun invPermuteIndex(i: Int): Int = when (interleaveMode) {
        "short" -> ((i * 13) and 63) // inverse of *5 mod 64 is *13
        "long" -> (( (i - 13) and 63) * 49) and 63 // inverse of *17 mod 64 is *49
        else -> i and 63
    }

    private fun computeLlr(xr: Double, xi: Double): Pair<Double, Double> {
        // Proportional to projection on I/Q axes; noise-norm not known -> relative weights
        return Pair(xr, xi)
    }
    // --- Pilot-aided PLL/CFO ---
    private var pllPhase = 0.0
    private var pllFreq = 0.0
    private val pllKp = 0.02
    private val pllKi = 0.001

    private fun correctCfoPhase() {
        if (!usePilots) return
        // estimate instantaneous phase at pilot bins (1 and fftSize/4)
        var phases = 0.0
        var count = 0
        fun phaseAt(k: Int): Double {
            val xr = re[k]; val xi = im[k]
            return kotlin.math.atan2(xi, xr)
        }
        val p1 = 1; val p2 = fftSize/4
        if (p1 in 1 until fftSize/2) { phases += phaseAt(p1); count++ }
        if (p2 in 1 until fftSize/2) { phases += phaseAt(p2); count++ }
        if (count == 0) return
        val avg = phases / count
        // PLL: error is desired 0 - measured phase
        val err = -avg
        pllFreq += pllKi * err
        pllPhase += pllKp * err + pllFreq

        // rotate all bins by -pllPhase to correct
        val c = kotlin.math.cos(pllPhase); val s = kotlin.math.sin(pllPhase)
        for (k in 0 until fftSize) {
            val xr = re[k]; val xi = im[k]
            re[k] = xr*c - xi*s
            im[k] = xr*s + xi*c
        }
    }


    /** Feed a PCM block (one or more OFDM symbols). Returns zero or more recovered KISS frames. */
    fun demodulate(pcm: ShortArray): List<ByteArray> {
        val symLen = fftSize + cpLen
        if (pcm.size < symLen) return emptyList()
        val out = ArrayList<ByteArray>()

        var off = 0
        val bitBuf = mutableListOf<Int>()
        while (off + symLen <= pcm.size) {
            // Strip CP
            for (i in 0 until fftSize) {
                re[i] = pcm[off + cpLen + i].toDouble() / 32768.0
                im[i] = 0.0
            }
            off += symLen

            // FFT
            DSPUtils.fft(re, im)

            correctCfoPhase()
            correctCfoPhase()
            // Carrier extraction after CFO/PLL correction
// Estimate SNR using carriers vs non-carriers (rough)
run {
    var sig = 0.0; var noise = 1e-9
    val used = kotlin.math.min(carriers.size, fftSize/2-1)
    val usedSet = java.util.HashSet<Int>()
    for (i in 0 until used) usedSet.add(carriers[i])
    for (k in 1 until fftSize/2) {
        val p = re[k]*re[k] + im[k]*im[k]
        if (usedSet.contains(k)) sig += p else noise += p
    }
    val snrLin = (sig / used).coerceAtLeast(1e-9) / (noise / ((fftSize/2-1)-used).coerceAtLeast(1))
    lastSnr = 10.0 * kotlin.math.log10(snrLin.coerceAtLeast(1e-9))
}
val carriersUsed = kotlin.math.min(64, carriers.size)
if (fecEnabled && carriersUsed >= 64) {
    // accumulate block
    for (ci in 0 until 64) {
        val k = carriers[ci]
        if (k in 1 until fftSize/2) {
            val xr = re[k]
            blockLlr[blockSym*64 + ci] = xr
        }
    }
    blockSym++
    if (blockSym >= 64) {
        // deinterleave
        val map = when (interleaveMode) {
            "short" -> InterleaveTables.shortInterleave()
            "long" -> InterleaveTables.longInterleave()
            else -> InterleaveTables.legacy()
        }
        val inv = InterleaveTables.inverse(map)
        val deintl = DoubleArray(64*64)
        // deintl[i] = blockLlr[inv[i]]
        for (i in 0 until 4096) deintl[i] = blockLlr[inv[i]]
        // Hadamard decode rows -> 6 bits each
        for (row in 0 until 64) {
            val rowVec = deintl.copyOfRange(row*64, row*64+64)
            val bits6 = had.softDecode(rowVec) // LSB-first
            for (b in 5 downTo 0) bitBuf += (bits6[b] and 1)
        }
        blockSym = 0
    }
} else {
    for (k in carriers) {
        if (k in 1 until fftSize / 2) {
            val xr = re[k]
            val xi = im[k]
            val b0 = if (xr >= 0) 0 else 1
            val b1 = if (xi >= 0) 0 else 1
            bitBuf += b0
            bitBuf += b1
        }
    }
}

// Attempt to parse frames from accumulated bits
            val bytes = bitsToBytes(bitBuf)
            var idx = 0
            while (idx + 2 <= bytes.size) {
                val len = ((bytes[idx].toInt() and 0xFF) shl 8) or (bytes[idx + 1].toInt() and 0xFF)
                if (len < 0 || len > 4096) break
                if (idx + 2 + len <= bytes.size) {
                    val payload = bytes.copyOfRange(idx + 2, idx + 2 + len)
                    out += payload
                    idx += 2 + len
                } else break
            }
            if (idx > 0) {
                // remove consumed
                bitBuf.clear()
                bitBuf += bytes.copyOfRange(idx, bytes.size).flatMap { b ->
                    (7 downTo 0).map { bi -> ((b.toInt() shr bi) and 1) }
                }
            }
        }
        return out
    }

    private fun bitsToBytes(bits: List<Int>): ByteArray {
        val nbytes = bits.size / 8
        val out = ByteArray(nbytes)
        var bi = 0
        for (i in 0 until nbytes) {
            var v = 0
            for (b in 7 downTo 0) {
                v = v or ((bits[bi++] and 1) shl b)
            }
            out[i] = v.toByte()
        }
        return out
    }
}
    fun lastSpectrum(): FloatArray = lastSpec.copyOf()

