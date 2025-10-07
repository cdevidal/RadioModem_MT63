package com.radiomodem.mt63.dsp

/** Interleave tables for MT63-like block (64x64).
 *  legacy: identity mapping.
 *  short : column-major order (good time/freq spread, simple).
 *  long  : diagonal stride with step coprime to 64.
 *  apply(): out[i] = in[map[i]]
 */
object InterleaveTables {
    const val SIZE = 64*64

    fun legacy(): IntArray = IntArray(SIZE) { it }

    fun shortInterleave(): IntArray {
        // 64 rows x 64 cols; flatten by columns
        val map = IntArray(SIZE)
        var idx = 0
        for (c in 0 until 64)
            for (r in 0 until 64) {
                val src = r * 64 + c
                map[idx++] = src
            }
        return map
    }

    fun longInterleave(): IntArray {
        // Diagonal/stride permutation in columns with stride 17 (coprime with 64)
        val stride = 17
        val map = IntArray(SIZE)
        var i = 0
        for (r in 0 until 64) {
            for (c in 0 until 64) {
                val cc = (c + r * stride) and 63
                val src = r * 64 + cc
                map[i++] = src
            }
        }
        return map
    }

    fun inverse(map: IntArray): IntArray {
        val inv = IntArray(map.size)
        for (i in map.indices) inv[map[i]] = i
        return inv
    }

    fun apply(map: IntArray, input: DoubleArray, output: DoubleArray) {
        require(map.size == input.size && input.size == output.size)
        for (i in map.indices) output[i] = input[map[i]]
    }
    fun applyInt(map: IntArray, input: IntArray, output: IntArray) {
        require(map.size == input.size && input.size == output.size)
        for (i in map.indices) output[i] = input[map[i]]
    }
}