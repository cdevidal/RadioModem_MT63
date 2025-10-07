package com.radiomodem.mt63.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.radiomodem.mt63.R

class ConnectionHelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help) // reuse simple help layout
        setTitle(R.string.connection_help)
    }
}