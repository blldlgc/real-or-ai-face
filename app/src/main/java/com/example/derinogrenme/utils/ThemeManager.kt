package com.example.derinogrenme.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class ThemeManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "ThemePrefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }

    // Tema modunu kaydet
    fun saveThemeMode(isDarkMode: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_THEME_MODE, isDarkMode).apply()
    }

    // Kaydedilmiş tema modunu al
    fun getThemeMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_THEME_MODE, false)
    }

    // Tema modunu uygula
    fun applyThemeMode(isDarkMode: Boolean) {
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
} 