package com.govtprep.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LanguageManager {
    private const val PREFS = "govtprep_lang"
    private const val KEY = "is_hindi"

    private val _isHindi = MutableStateFlow(false)
    val isHindi = _isHindi.asStateFlow()

    fun init(context: Context) {
        _isHindi.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    }

    fun setHindi(context: Context, hindi: Boolean) {
        _isHindi.value = hindi
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, hindi).apply()
    }
}
