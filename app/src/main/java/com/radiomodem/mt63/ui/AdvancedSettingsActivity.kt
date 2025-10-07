package com.radiomodem.mt63.ui

import android.os.Bundle
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.radiomodem.mt63.R

class AdvancedSettingsActivity : AppCompatActivity() {
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, Prefs())
            .commit()
    }

    class Prefs : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }
}