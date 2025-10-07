package com.radiomodem.mt63.dsp

/** Soft-decision Hadamard(64) decoder.
 *  Walsh–Hadamard matrix (+1/-1) via Sylvester construction.
 *  Returns 6 bits (LSB-first) of the best-correlated row index.
 */
class Hadamard64 {
    private val N = 64
    private val H: Array<IntArray> = Array(N) { IntArray(N) }

    init {
        // Sylvester construction
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
    }

    /** llr.size must be 64 */
    fun softDecode(llr: DoubleArray): IntArray {
        require(llr.size == N) { "Hadamard64 expects 64 soft symbols" }
        var best = Double.NEGATIVE_INFINITY
        var idx = 0
        for (r in 0 until N) {
            val row = H[r]
            var s = 0.0
            for (k in 0 until N) s += row[k] * llr[k]
            if (s > best) { best = s; idx = r }
        }
        val bits = IntArray(6)
        for (b in 0 until 6) bits[b] = (idx shr b) and 1
        return bits
    }
}