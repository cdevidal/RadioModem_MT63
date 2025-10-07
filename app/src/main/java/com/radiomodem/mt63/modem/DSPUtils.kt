package com.radiomodem.mt63.modem

import com.radiomodem.mt63.dsp.NativeFFT
import kotlin.math.*

object DSPUtils {
    /** FFT with fallback to Java implementation; in-place on re/im. */
    @JvmStatic fun fft(re: DoubleArray, im: DoubleArray) {
        if (tryNativeFFT(re, im)) return
        val n = re.size
        require(n == im.size && (n and (n - 1)) == 0) { "len must be power of two" }
        // bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var size = 2
        while (size <= n) {
            val half = size shr 1
            val theta = -2.0 * Math.PI / size
            val wpr = cos(theta)
            val wpi = sin(theta)
            var wr = 1.0
            var wi = 0.0
            for (m in 0 until half) {
                var k = m
                while (k < n) {
                    val j2 = k + half
                    val tr = wr * re[j2] - wi * im[j2]
                    val ti = wr * im[j2] + wi * re[j2]
                    re[j2] = re[k] - tr; im[j2] = im[k] - ti
                    re[k] += tr; im[k] += ti
                    k += size
                }
                val wtmp = wr
                wr = wtmp * wpr - wi * wpi
                wi = wtmp * wpi + wi * wpr
            }
            size = size shl 1
        }
    }

    @JvmStatic fun ifft(re: DoubleArray, im: DoubleArray) {
        // Conjugate, fft, conjugate, scale
        for (i in re.indices) im[i] = -im[i]
        fft(re, im)
        val invN = 1.0 / re.size
        for (i in re.indices) { re[i] *= invN; im[i] = -im[i] * invN }
    }

    @JvmStatic fun hann(arr: DoubleArray) {
        val n = arr.size
        for (i in 0 until n) arr[i] = 0.5 - 0.5 * cos(2.0 * Math.PI * i / (n - 1).coerceAtLeast(1))
    }

    @JvmStatic fun sat16(v: Double): Short {
        return when {
            v > 32767.0 -> 32767
            v < -32768.0 -> -32768
            else -> v.toInt()
        }.toShort()
    }

    private fun tryNativeFFT(re: DoubleArray, im: DoubleArray): Boolean {
        return try {
            if (!NativeFFT.ok) return false
            val n = re.size
            val r = FloatArray(n) { re[it].toFloat() }
            val ii = FloatArray(n) { im[it].toFloat() }
            NativeFFT.forwardFFT(r, ii, n)
            for (k in 0 until n) { re[k] = r[k].toDouble(); im[k] = ii[k].toDouble() }
            true
        } catch (t: Throwable) { false }
    }
}