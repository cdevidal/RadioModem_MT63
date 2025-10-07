package com.radiomodem.mt63.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.radiomodem.mt63.R
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class SelfTestActivity : AppCompatActivity() {
    @Volatile private var running = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help) // reuse simple layout
        title = getString(R.string.selftest)
        // ID not in layout
        // findViewById<MaterialButton>(R.id.btnOk)?.setOnClickListener {
        //     if (!running) {
        //         running = true
        //         thread { playSine(48000, 1000.0) }
        //     } else running = false
        // }
    }
    private fun playSine(sr: Int, freq: Double) {
        val buf = ShortArray(2048)
        val at = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(buf.size*2)
            .build()
        at.play()
        var t = 0.0
        val dt = 1.0/sr
        while (running) {
            for (i in buf.indices) {
                val v = sin(2*PI*freq*t)
                buf[i] = (v * 30000).toInt().toShort()
                t += dt
            }
            at.write(buf, 0, buf.size)
        }
        at.stop(); at.release()
    }
}