package com.radiomodem.mt63.util

class RingLog(private val cap: Int = 2000) {
    enum class Level { DEBUG, INFO, WARN, ERROR }
    private val lines = ArrayDeque<String>(cap)
    @Synchronized fun log(level: Level, msg: String) {
        if (lines.size >= cap) lines.removeFirst()
        lines.addLast("${level.name}: $msg")
    }
    @Synchronized fun dump(): String = lines.joinToString("\n")
}