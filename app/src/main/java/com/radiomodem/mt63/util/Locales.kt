package com.radiomodem.mt63.util
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
object Locales {
    @JvmStatic
    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}

