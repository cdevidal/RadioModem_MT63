package com.radiomodem.mt63.modem

object MT63Profiles {
    data class Params(val fft: Int, val cp: Int, val carriers: Int)
    @JvmStatic fun profile(id: String): Params = when (id) {
        "low_latency" -> Params(512, 64, 48)
        "robust" -> Params(2048, 192, 80)
        "balanced" -> Params(1024, 96, 64)
        "custom" -> Params(1024, 96, 64)
        else -> Params(1024, 96, 64)
    }
}