package com.radiomodem.mt63.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.radiomodem.mt63.R
import com.radiomodem.mt63.util.RingLog

class LogActivity : AppCompatActivity() {
    private var current: RingLog.Level = RingLog.Level.INFO
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setTitle(R.string.log)
        val tv: TextView = findViewById(R.id.txtLog)
        tv.text = getString(R.string.log_placeholder)
    }
}