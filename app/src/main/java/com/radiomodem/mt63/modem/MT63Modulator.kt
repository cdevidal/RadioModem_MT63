package com.radiomodem.mt63.modem

import com.radiomodem.mt63.dsp.InterleaveTables
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/** MT63-like OFDM/QPSK модulator с опциональным FEC (Hadamard 64x64 блоки). */
class MT63Modulator(
    private val sampleRate: Int,
    private val fftSize: Int,
    private val cpLen: Int,
    private val carriers: IntArray,
    private val usePilots: Boolean
) {
    private val prng = Random(0x53494E47)
    private var fecEnabled = false
    private var interleaveMode = "legacy"

    fun setFec(enabled: Boolean, mode: String) { fecEnabled = enabled; interleaveMode = mode }

    private fun interleaveMap(): IntArray = when (interleaveMode) {
        "short" -> InterleaveTables.shortInterleave()
        "long"  -> InterleaveTables.longInterleave()
        else    -> InterleaveTables.legacy()
    }

    /** Модуляция KISS-пакета в PCM 16-bit (моно). */
    fun modulate(kiss: ByteArray): ShortArray {
        // Формируем внутренний кадр с длиной
        val frame = MT63Frame.pack(kiss)
        val bits = bytesToBits(frame)

        // FEC-блок 64x64 требует минимум 64 несущих
        if (fecEnabled && carriers.size >= 64) {
            val bitsPerBlock = 64 * 6 // 6 бит на строку
            val blockCount = kotlin.math.max(1, (bits.size + bitsPerBlock - 1) / bitsPerBlock)
            val totalSymbols = blockCount * 64
            val out = ShortArray(totalSymbols * (fftSize + cpLen))

            val map = interleaveMap()
            var bitIdx = 0
            var outIdx = 0
            val re = DoubleArray(fftSize)
            val im = DoubleArray(fftSize)
            val inBlock = DoubleArray(64 * 64)
            val outBlock = DoubleArray(64 * 64)

            repeat(blockCount) {
                // 1) соберём 64 строки по 6 бит → Адамар 64
                var p = 0
                for (row in 0 until 64) {
                    var idx6 = 0
                    repeat(6) {
                        val b = if (bitIdx < bits.size) bits[bitIdx++] else 0
                        idx6 = (idx6 shl 1) or b
                    }
                    val chips = hadRow(idx6)
                    for (c in 0 until 64) inBlock[p++] = chips[c].toDouble()
                }
                // 2) интерливинг 64x64
                for (i in 0 until 4096) outBlock[i] = inBlock[map[i]]

                // 3) 64 OFDM-символа
                var base = 0
                for (sym in 0 until 64) {
                    java.util.Arrays.fill(re, 0.0); java.util.Arrays.fill(im, 0.0)
                    val carriersUsed = 64
                    for (ci in 0 until carriersUsed) {
                        val k = carriers[ci]
                        if (k in 1 until fftSize/2) {
                            val chip = outBlock[base++]
                            re[k] = chip * 0.7
                            im[k] = 0.0
                            re[fftSize - k] = re[k]
                            im[fftSize - k] = -im[k]
                        } else {
                            base++ // пропустить если несущая вне диапазона
                        }
                    }
                    // пилоты
                    if (usePilots) {
                        fun pilot(k: Int) {
                            if (k in 1 until fftSize/2) {
                                re[k] += 0.4
                                re[fftSize - k] += 0.4
                            }
                        }
                        pilot(1); pilot(fftSize/4)
                    }
                    // лёгкое зернение для разнесения утечек
                    repeat(4) {
                        val k = 1 + prng.nextInt(fftSize/2 - 2)
                        val d = (prng.nextDouble() - 0.5) * 0.02
                        re[k] += d; re[fftSize - k] += d
                    }
                    DSPUtils.ifft(re, im)
                    // CP + символ
                    for (i in fftSize - cpLen until fftSize) out[outIdx++] = DSPUtils.sat16(re[i] * 32767.0)
                    for (i in 0 until fftSize) out[outIdx++] = DSPUtils.sat16(re[i] * 32767.0)
                }
            }
            return out
        } else {
            // Обычный QPSK по несущим (2 бита на поднесущую)
            val symbols = (bits.size + 1) / 2
            val blocks = (symbols + carriers.size - 1) / carriers.size
            val out = ShortArray(blocks * (fftSize + cpLen))
            var bitIdx = 0
            var outIdx = 0
            val re = DoubleArray(fftSize)
            val im = DoubleArray(fftSize)

            repeat(blocks) {
                java.util.Arrays.fill(re, 0.0); java.util.Arrays.fill(im, 0.0)
                for (i in carriers.indices) {
                    if (bitIdx >= bits.size) break
                    val b0 = bits[bitIdx++]
                    val b1 = if (bitIdx < bits.size) bits[bitIdx++] else 0
                    val xr = when {
                        b0 == 0 && b1 == 0 -> 1.0
                        b0 == 1 && b1 == 1 -> -1.0
                        else -> 0.0
                    }
                    val xi = when {
                        b0 == 0 && b1 == 1 -> 1.0
                        b0 == 1 && b1 == 0 -> -1.0
                        else -> 0.0
                    }
                    val k = carriers[i]
                    if (k in 1 until fftSize/2) {
                        re[k] = xr * 0.75; im[k] = xi * 0.75
                        re[fftSize - k] = re[k]; im[fftSize - k] = -im[k]
                    }
                }
                if (usePilots) {
                    fun pilot(k: Int) {
                        if (k in 1 until fftSize/2) {
                            re[k] += 0.5; re[fftSize - k] += 0.5
                        }
                    }
                    pilot(1); pilot(fftSize/4)
                }
                repeat(8) {
                    val k = 1 + prng.nextInt(fftSize/2 - 2)
                    val d = (prng.nextDouble() - 0.5) * 0.03
                    re[k] += d; re[fftSize - k] += d
                }
                DSPUtils.ifft(re, im)
                for (i in fftSize - cpLen until fftSize) out[outIdx++] = DSPUtils.sat16(re[i] * 32767.0)
                for (i in 0 until fftSize) out[outIdx++] = DSPUtils.sat16(re[i] * 32767.0)
            }
            return out
        }
    }

    private fun bytesToBits(data: ByteArray): IntArray {
        val bits = IntArray(data.size * 8)
        var idx = 0
        for (b in data) {
            for (i in 7 downTo 0) {
                bits[idx++] = (b.toInt() ushr i) and 1
            }
        }
        return bits
    }

    private fun hadRow(index: Int): IntArray {
        val N = 64
        val H = Array(N) { IntArray(N) }
        H[0][0] = 1
        var n = 1
        while (n < N) {
            for (i in 0 until n) {
                for (j in 0 until n) {
                    val v = H[i][j]
                    H[i][j] = v
                    H[i + n][j] = v
                    H[i][j + n] = v
                    H[i + n][j + n] = -v
                }
            }
            n = n shl 1
        }
        return H[index and 63]
    }
}

