package com.radiomodem.mt63.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.radiomodem.mt63.R
import com.radiomodem.mt63.modem.MT63Modem
import com.radiomodem.mt63.net.KISSServer

class ModemForegroundService : Service(), MT63Modem.MetricsSink {
    companion object {
        const val ACTION_START = "com.radiomodem.mt63.ACTION_START"
        const val ACTION_STOP = "com.radiomodem.mt63.ACTION_STOP"
        const val ACTION_STATUS = "com.radiomodem.mt63.ACTION_STATUS"
    }

    private lateinit var modem: MT63Modem
    private lateinit var kiss: KISSServer
    private lateinit var wake: PowerManager.WakeLock

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("modem", getString(R.string.notif_title), NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mt63:modem").apply { setReferenceCounted(false) }
        modem = MT63Modem(applicationContext).also { it.setMetricsSink(this) }
        kiss = KISSServer(applicationContext, modem)
        startForeground(1, buildNotification(false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!wake.isHeld) wake.acquire()
                modem.start()
                kiss.start()
                update(true)
            }
            ACTION_STOP -> {
                kiss.stop()
                modem.stop()
                if (wake.isHeld) wake.release()
                update(false)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(running: Boolean): Notification {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val bind = prefs.getString("pref_kiss_bind", "127.0.0.1")
        val port = prefs.getString("pref_kiss_port", "8100")
        val text = if (running) getString(R.string.notif_running, bind, port) else getString(R.string.status_idle)
        val open = PendingIntent.getActivity(this, 0, Intent(this, com.radiomodem.mt63.ui.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "modem")
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun update(running: Boolean) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, buildNotification(running))
    }

    override fun onDestroy() {
        try { kiss.stop() } catch (_: Exception) {}
        try { modem.stop() } catch (_: Exception) {}
        try { if (wake.isHeld) wake.release() } catch (_: Exception) {}
        super.onDestroy()
    }

    // MetricsSink
    override fun onMetrics(m: MT63Modem.Metrics) {
        val i = Intent(ACTION_STATUS)
        i.putExtra("running", m.running)
        i.putExtra("sampleRate", m.sampleRate)
        i.putExtra("fft", m.fft)
        i.putExtra("cp", m.cp)
        i.putExtra("txQueue", m.txQueue)
        i.putExtra("txCap", m.txCap)
        i.putExtra("rxRms", m.rxRms)
        i.putExtra("txRms", m.txRms)
        i.putExtra("rxOk", m.rxFramesOk)
        i.putExtra("rxErr", m.rxFramesErr)
        i.putExtra("ber", m.ber)
        i.putExtra("snr", m.snr)
            i.putExtra("rxScope", m.rxScope)
            i.putExtra("rxSpectrum", m.rxSpectrum)
        i.putExtra("overload", m.inputOverload)
        sendBroadcast(i)
    }
}

