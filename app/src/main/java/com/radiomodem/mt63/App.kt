package com.radiomodem.mt63
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        // Apply language
        val lang = prefs.getString("pref_app_language","en") ?: "en"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        // Apply theme
        when (prefs.getString("pref_app_theme","dark")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}

