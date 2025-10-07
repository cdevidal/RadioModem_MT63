package com.radiomodem.mt63.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.radiomodem.mt63.R
import com.radiomodem.mt63.service.ModemForegroundService

class StatisticsActivity : AppCompatActivity() {

    private var tv: TextView? = null
    private val rcv = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ModemForegroundService.ACTION_STATUS == intent.action) {
                val rxOk = intent.getLongExtra("rxOk", 0L)
                val rxErr = intent.getLongExtra("rxErr", 0L)
                val ber = intent.getDoubleExtra("ber", 0.0)
                val snr = intent.getDoubleExtra("snr", 0.0)
                tv?.text = "RX ok=$rxOk err=$rxErr\nBER=%.3f SNR=%.1f".format(ber, snr)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        setTitle(R.string.statistics)
        // ID not in layout
        // tv = findViewById(R.id.txtStats)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(rcv, IntentFilter(ModemForegroundService.ACTION_STATUS))
    }
    override fun onPause() {
        try { unregisterReceiver(rcv) } catch (_: Exception) {}
        super.onPause()
    }
}