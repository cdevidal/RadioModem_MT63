package com.radiomodem.mt63.dsp
object NativeFFT {
    init {
        try { System.loadLibrary("mt63fft") } catch (t: Throwable) { ok = false }
    }
    @JvmStatic var ok: Boolean = true
        private set
    @JvmStatic external fun forwardFFT(re: FloatArray, im: FloatArray, n: Int)
    @JvmStatic external fun inverseFFT(re: FloatArray, im: FloatArray, n: Int)
}