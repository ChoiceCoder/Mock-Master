package com.govtprep.app.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Preset avatar system — stored locally via SharedPrefs
// Each avatar is an emoji + background color pair
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class AvatarOption(
    val emoji: String,
    val background: Color,
    val label: String
)

val PRESET_AVATARS = listOf(
    AvatarOption("🦁", Color(0xFFF59E0B), "Lion"),
    AvatarOption("🐯", Color(0xFFEF4444), "Tiger"),
    AvatarOption("🦊", Color(0xFFFF6B35), "Fox"),
    AvatarOption("🐻", Color(0xFF8B5E3C), "Bear"),
    AvatarOption("🦅", Color(0xFF3B82F6), "Eagle"),
    AvatarOption("🐉", Color(0xFF10B981), "Dragon"),
    AvatarOption("🦋", Color(0xFFA855F7), "Butterfly"),
    AvatarOption("⚡", Color(0xFF6366F1), "Thunder"),
)

object AvatarManager {
    private const val PREFS_NAME = "govtprep_avatar"
    private const val KEY_INDEX  = "avatar_index"

    private val _avatarIndex = MutableStateFlow(0)
    val avatarIndex = _avatarIndex.asStateFlow()

    val current: AvatarOption
        get() = PRESET_AVATARS[_avatarIndex.value.coerceIn(0, PRESET_AVATARS.lastIndex)]

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _avatarIndex.value = prefs.getInt(KEY_INDEX, 0)
    }

    fun setAvatar(context: Context, index: Int) {
        val clamped = index.coerceIn(0, PRESET_AVATARS.lastIndex)
        _avatarIndex.value = clamped
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_INDEX, clamped).apply()
    }
}
