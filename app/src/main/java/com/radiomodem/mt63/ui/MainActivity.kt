package com.radiomodem.mt63.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.radiomodem.mt63.R
import com.radiomodem.mt63.service.ModemForegroundService

class MainActivity : AppCompatActivity() {

    private var scopeView: OscilloscopeView? = null
    private var spectrumView: SpectrumView? = null

    private var scopeView: OscilloscopeView? = null

    private lateinit var txtStatus: TextView
    private lateinit var txtInfo1: TextView
    private lateinit var txtInfo2: TextView
    private lateinit var txtInfo3: TextView
    private lateinit var txtInfo4: TextView
    private lateinit var txtInfo5: TextView
    private lateinit var txtWarning: TextView
    private lateinit var barRx: ProgressBar
    private lateinit var barTx: ProgressBar

    private val rcv = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ModemForegroundService.ACTION_STATUS == intent.action) {
                val running = intent.getBooleanExtra("running", false)
                val sr = intent.getIntExtra("sampleRate", 48000)
                val fft = intent.getIntExtra("fft", 512)
                val cp = intent.getIntExtra("cp", 64)
                val txQ = intent.getIntExtra("txQueue", 0)
                val txCap = intent.getIntExtra("txCap", 0)
                val rxRms = intent.getDoubleExtra("rxRms", 0.0)
                val txRms = intent.getDoubleExtra("txRms", 0.0)
                val rxOk = intent.getLongExtra("rxOk", 0L)
                val rxErr = intent.getLongExtra("rxErr", 0L)
                val ber = intent.getDoubleExtra("ber", 0.0)
                val snr = intent.getDoubleExtra("snr", 0.0)
                val overload = intent.getBooleanExtra("overload", false)

                txtStatus.text = if (running) getString(R.string.status_running) else getString(R.string.status_idle)
                txtInfo1.text = "SR=$sr FFT=$fft CP=$cp"
                txtInfo2.text = "TX $txQ/$txCap"
                txtInfo3.text = "RMS RX=%.2f TX=%.2f".format(rxRms, txRms)
                txtInfo4.text = "RX ok=$rxOk err=$rxErr"
                txtInfo5.text = "BER=%.3f SNR=%.1f".format(ber, snr)
                txtWarning.setBackgroundColor(if (overload) 0x44FF0000 else 0x00000000.toInt())
                barRx.progress = (rxRms * 100).toInt().coerceIn(0, 100)
                barTx.progress = (txRms * 100).toInt().coerceIn(0, 100)
                intent.getShortArrayExtra("rxScope")?.let { scopeView?.update(it) }
                intent.getFloatArrayExtra("rxSpectrum")?.let { spectrumView?.update(it) }
            }
        }
    }

    private val reqMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(@Nullable b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)

        scopeView = findViewById(R.id.scopeView)
        spectrumView = findViewById(R.id.spectrumView)

        txtStatus = findViewById(R.id.txtStatus)
        txtInfo1 = findViewById(R.id.txtInfo1)
        txtInfo2 = findViewById(R.id.txtInfo2)
        txtInfo3 = findViewById(R.id.txtInfo3)
        txtInfo4 = findViewById(R.id.txtInfo4)
        txtInfo5 = findViewById(R.id.txtInfo5)
        txtWarning = findViewById(R.id.txtWarning)
        barRx = findViewById(R.id.barRx)
        barTx = findViewById(R.id.barTx)

        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            startForegroundService(Intent(this, ModemForegroundService::class.java).setAction(ModemForegroundService.ACTION_START))
        }
        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener {
            startService(Intent(this, ModemForegroundService::class.java).setAction(ModemForegroundService.ACTION_STOP))
        }
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, AdvancedSettingsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, ConnectionHelpActivity::class.java))
        }
        ensureMicPermission()
    }

    private fun ensureMicPermission() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!granted) reqMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_log -> { startActivity(Intent(this, LogActivity::class.java)); true }
            R.id.menu_stats -> { startActivity(Intent(this, StatisticsActivity::class.java)); true }
            R.id.menu_help -> { startActivity(Intent(this, HelpActivity::class.java)); true }
            R.id.menu_selftest -> { startActivity(android.content.Intent(this, SelfTestActivity::class.java)); true }
            else -> super.onOptionsItemSelected(item)
        }
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