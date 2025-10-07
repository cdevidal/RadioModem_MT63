package com.radiomodem.mt63.settings

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Locale

object ProfileManager {
    const val KEY_PROFILE = "pref_profile_select"
    const val KEY_LAST_APPLIED = "pref_profile_last_applied"
    const val KEY_OVERRIDES = "pref_has_manual_overrides"

    private const val K_FFT = "pref_mt63_fft"
    private const val K_CP = "pref_mt63_cp"
    private const val K_CARRIERS = "pref_mt63_carriers"
    private const val K_PILOTS = "pref_mt63_pilots"
    private const val K_FEC = "pref_mt63_fec"
    private const val K_INTERLEAVE = "pref_mt63_interleave"
    private const val K_SR = "pref_sample_rate"

    private fun loadProfilesJson(ctx: Context): JSONArray? {
        return try {
            val resId = ctx.resources.getIdentifier("profiles", "raw", ctx.packageName)
            if (resId != 0) {
                ctx.resources.openRawResource(resId).use { ins ->
                    val data = ins.readBytes().toString(StandardCharsets.UTF_8)
                    val trimmed = data.trim()
                    if (trimmed.startsWith("{")) {
                        val obj = JSONObject(trimmed)
                        obj.optJSONArray("profiles")
                    } else {
                        JSONArray(trimmed)
                    }
                }
            } else null
        } catch (_: Throwable) { null }
    }

    private fun findProfile(ctx: Context, id: String): JSONObject? {
        val arr = loadProfilesJson(ctx) ?: return null
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (id == o.optString("id")) return o
        }
        return null
    }

    fun applyProfile(ctx: Context, id: String) {
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        val e = sp.edit()
        val p = findProfile(ctx, id)
        if (p != null) {
            val fft = p.optInt("fft", 1024)
            val cp = p.optInt("cp", 96)
            val carriers = p.optInt("carriers", 64)
            val pilots = p.optBoolean("pilots", true)
            val fec = p.optBoolean("fec", false)
            val interleave = p.optString("interleave", "legacy")
            val sr = p.optInt("sample_rate", 48000)
            e.putString(K_FFT, fft.toString())
            e.putString(K_CP, cp.toString())
            e.putString(K_CARRIERS, carriers.toString())
            e.putBoolean(K_PILOTS, pilots)
            e.putBoolean(K_FEC, fec)
            e.putString(K_INTERLEAVE, interleave)
            e.putString(K_SR, sr.toString())
            e.putString(KEY_LAST_APPLIED, id)
            e.putBoolean(KEY_OVERRIDES, false)
        } else {
            e.putString(KEY_LAST_APPLIED, "custom")
        }
        e.apply()
    }

    fun getProfileDisplayName(ctx: Context, id: String?): String {
        if (id == null) return "Custom"
        if (id == "custom") return "Custom"
        val p = findProfile(ctx, id) ?: return id
        val lang = Locale.getDefault().language ?: "en"
        return if (lang.startsWith("ru")) p.optString("name_ru", p.optString("name_en", id))
               else p.optString("name_en", id)
    }

    fun getProfileStatusLine(ctx: Context): String {
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        val last = sp.getString(KEY_LAST_APPLIED, "custom") ?: "custom"
        val mod = sp.getBoolean(KEY_OVERRIDES, false)
        return if (last == "custom") {
            ctx.getString(com.radiomodem.mt63.R.string.profile_status_line_custom)
        } else {
            val name = getProfileDisplayName(ctx, last)
            if (mod) ctx.getString(com.radiomodem.mt63.R.string.profile_status_line_modified, name)
            else ctx.getString(com.radiomodem.mt63.R.string.profile_status_line_active, name)
        }
    }
}