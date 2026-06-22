package com.govtprep.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Global theme state — persisted via SharedPrefs
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object ThemeManager {
    private const val PREFS_NAME = "govtprep_theme"
    private const val KEY_DARK = "is_dark_mode"

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean(KEY_DARK, true)
    }

    fun toggleTheme(context: Context) {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, newValue).apply()
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        _isDarkMode.value = isDark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, isDark).apply()
    }
}
