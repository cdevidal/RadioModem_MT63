package com.radiomodem.mt63.modem

import android.content.Context
import android.media.*
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlin.math.sqrt

class MT63Modem(ctx: Context) {
    private fun erfc(x: Double): Double {
        // Abramowitz-Stegun approximation
        val z = kotlin.math.abs(x)
        val t = 1.0 / (1.0 + 0.5*z)
        val ans = t * kotlin.math.exp(-z*z - 1.26551223 + t*(1.00002368 + t*(0.37409196 + t*(0.09678418 + t*(-0.18628806 + t*(0.27886807 + t*(-1.13520398 + t*(1.48851587 + t*(-0.82215223 + t*0.17087277)))))))))
        return if (x >= 0) ans else 2.0 - ans
    }


    interface KissRxSink { fun onKissBytesFromModem(kiss: ByteArray) }
    interface MetricsSink { fun onMetrics(m: Metrics) }
    class Metrics {
        var running = false
        var sampleRate = 48000
        var fft = 1024
        var cp = 96
        var txQueue = 0; var txCap = 0
        var rxRms = 0.0; var txRms = 0.0
        var rxFramesOk: Long = 0; var rxFramesErr: Long = 0
        var ber = 0.0; var snr = 0.0
        var inputOverload = false
        var rxSpectrum: FloatArray = floatArrayOf()
        var rxScope: ShortArray = ShortArray(0)
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx.applicationContext)
    private val running = AtomicBoolean(false)
    private val exec: ExecutorService = Executors.newFixedThreadPool(2)
    private var rec: AudioRecord? = null
    private var play: AudioTrack? = null

    private var txDelayMs = 150; private var txTailMs = 50; private var slotMs = 20; private var persist = 128

    private var kissSink: KissRxSink? = null
    private var metricsSink: MetricsSink? = null
    private val met = Metrics()

    private val sampleRate = (prefs.getString("pref_sample_rate", "48000") ?: "48000").toInt()
    private val fftSize = (prefs.getString("pref_mt63_fft", "1024") ?: "1024").toInt()
    private val cpLen = (prefs.getString("pref_mt63_cp", "96") ?: "96").toInt()
    private val carriersCount = (prefs.getString("pref_mt63_carriers", "64") ?: "64").toInt().coerceAtMost(fftSize/2-1)
    private val usePilots = prefs.getBoolean("pref_mt63_pilots", true)
    private val fecEnabled = prefs.getBoolean("pref_mt63_fec", false)
    private val interleave = prefs.getString("pref_mt63_interleave", "legacy") ?: "legacy"

    private val carriers: IntArray = kotlin.run {
        // Uniformly spread in (1..fftSize/2-1)
        val step = (fftSize / 2 - 2) / carriersCount
        IntArray(carriersCount) { 1 + it * step + 1 }
    }

    private val txQueue = ArrayBlockingQueue<ByteArray>(64)

    private val mod = MT63Modulator(sampleRate, fftSize, cpLen, carriers, usePilots).apply { setFec(fecEnabled, interleave) }
    private val dem = MT63Demodulator(fftSize, cpLen, carriers, usePilots).apply { setFec(fecEnabled, interleave) }

    fun setKissRxSink(s: KissRxSink?) { kissSink = s }
    fun setMetricsSink(s: MetricsSink?) { metricsSink = s }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        initAudio()
        met.running = true
        met.sampleRate = sampleRate
        met.fft = fftSize
        met.cp = cpLen
        publish()

        exec.execute { rxLoop() }
        exec.execute { txLoop() }
    }

    fun stop() {
        running.set(false)
        try { rec?.stop() } catch (_: Exception) {}
        try { play?.stop() } catch (_: Exception) {}
        try { rec?.release() } catch (_: Exception) {}
        try { play?.release() } catch (_: Exception) {}
        rec = null; play = null
        met.running = false
        publish()
    }

    private fun initAudio() {
        val frame = fftSize + cpLen
        val inBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        var audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
        if (Build.VERSION.SDK_INT >= 24) {
            try { audioSource = MediaRecorder.AudioSource.UNPROCESSED } catch (_: Throwable) {}
        }
        val inFmt = AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()
        rec = AudioRecord.Builder().setAudioSource(audioSource).setAudioFormat(inFmt).setBufferSizeInBytes(maxOf(inBuf, frame * 8)).build()
        try {
            if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                android.media.audiofx.AcousticEchoCanceler.create(rec!!.audioSessionId)?.enabled = false
            }
        } catch (_: Throwable) {}

        val outBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val outFmt = AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
        play = AudioTrack.Builder().setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        ).setAudioFormat(outFmt).setBufferSizeInBytes(maxOf(outBuf, frame * 8)).build()

        rec?.startRecording()
        play?.play()
    }

    private fun rxLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val frame = fftSize + cpLen
        val buf = ShortArray(frame * 8)
        while (running.get()) {
            val r = rec?.read(buf, 0, buf.size) ?: break
            if (r <= 0) continue
            met.rxRms = rms(buf, r)
            val n = kotlin.math.min(r, 512)
            met.rxScope = java.util.Arrays.copyOf(buf, n)
            // Demodulate per frames
            val frames = dem.demodulate(buf.copyOf(r))
            met.rxSpectrum = dem.lastSpectrum()
            val snrDb = dem.lastSnrDb(); met.snr = snrDb
            val snrLin = 10.0.pow(snrDb/10.0)
            met.ber = 0.5 * erfc(sqrt(snrLin))
            if (frames.isNotEmpty()) {
                for (kiss in frames) kissSink?.onKissBytesFromModem(kiss)
                met.rxFramesOk += frames.size
            }
            publish()
        }
    }

    private fun txLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        while (running.get()) {
            try {
                val kiss = txQueue.take()
                if (txDelayMs > 0) Thread.sleep(txDelayMs.toLong())
                val pcm = mod.modulate(kiss)
                met.txRms = rms(pcm, pcm.size)
                play?.write(pcm, 0, pcm.size)
                if (txTailMs > 0) Thread.sleep(txTailMs.toLong())
                publish()
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (t: Throwable) {
                Log.e("MT63Modem", "txLoop error", t)
            }
        }
    }

    private fun rms(samples: ShortArray, n: Int): Double {
        var acc = 0.0
        val m = n.coerceAtMost(samples.size)
        for (i in 0 until m) {
            val v = samples[i] / 32768.0
            acc += v * v
        }
        return kotlin.math.sqrt(acc / m.coerceAtLeast(1))
    }

    fun setTxDelayMs(ms: Int) { txDelayMs = ms.coerceAtLeast(0) }
    fun setTxTailMs(ms: Int) { txTailMs = ms.coerceAtLeast(0) }
    fun setSlotTimeMs(ms: Int) { slotMs = ms.coerceAtLeast(0) }
    fun setPersist(p: Int) { persist = p.coerceIn(0, 255) }

    fun transmitFrame(kiss: ByteArray) {
        if (!txQueue.offer(kiss)) {
            Log.w("MT63Modem", "TX queue full; drop")
        }
        met.txQueue = txQueue.size
        met.txCap = txQueue.remainingCapacity() + txQueue.size
        publish()
    }

    private fun publish() {
        metricsSink?.onMetrics(met)
    }
}