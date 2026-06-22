package com.govtprep.app.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Dynamic Color Palette — Dark / Light
// Accessed via NeuColors anywhere in Compose tree
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class NeuColorPalette(
    val Background: Color,
    val Surface: Color,
    val SurfaceAlt: Color,
    val SurfaceDeep: Color,
    val ShadowDark: Color,
    val ShadowLight: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextMuted: Color,
    val TextDisabled: Color,
    val IconActive: Color,
    val IconInactive: Color,
    val Accent: Color,
    val AccentPressed: Color,
    val AccentGlow: Color,
    val AccentSurface: Color,
    val AccentUltraLight: Color,
    val Success: Color,
    val Warning: Color,
    val Error: Color,
    val ToggleOnTrack: Color,
    val ToggleOffTrack: Color,
    val ToggleKnob: Color,
    val ToggleKnobOn: Color,
    val Divider: Color,
    val isDark: Boolean
)

// ── DARK — Premium neumorphic (unchanged) ──
val DarkPalette = NeuColorPalette(
    Background    = Color(0xFF1F1F21),
    Surface       = Color(0xFF2F2F32),
    SurfaceAlt    = Color(0xFF262629),
    SurfaceDeep   = Color(0xFF1A1A1C),
    ShadowDark    = Color(0xFF0A0A0C),
    ShadowLight   = Color(0xFF3A3A3E),
    TextPrimary   = Color(0xFFF5F5F5),
    TextSecondary = Color(0xFFCCCCCC),
    TextMuted     = Color(0xFF8E8E93),
    TextDisabled  = Color(0xFF5A5A5E),
    IconActive    = Color.White,
    IconInactive  = Color(0xFFB0B0B0),
    Accent        = Color(0xFFE0E0E5),
    AccentPressed = Color(0xFFD0D0D5),
    AccentGlow    = Color(0x22FFFFFF),
    AccentSurface = Color(0xFF2A2A2D),
    AccentUltraLight = Color(0xFF222225),
    Success       = Color(0xFF4ADE80),
    Warning       = Color(0xFFFBBF24),
    Error         = Color(0xFFEF4444),
    ToggleOnTrack = Color(0xFF22C55E),
    ToggleOffTrack = Color(0xFF3A3A3D),
    ToggleKnob    = Color(0xFF2A2A2D),
    ToggleKnobOn  = Color.White,
    Divider       = Color(0xFF3A3A3D),
    isDark        = true
)

// ── LIGHT — Fintech: 70% White / 30% Emerald ──
// Background is WHITE. Emerald only for actions & highlights.
// Cards are white with subtle elevation. Clean, calm, premium.
val LightPalette = NeuColorPalette(
    Background    = Color(0xFFFFFFFF),          // pure white
    Surface       = Color(0xFFFFFFFF),          // white cards
    SurfaceAlt    = Color(0xFFF9FAFB),          // very subtle gray bg
    SurfaceDeep   = Color(0xFFF3F4F6),          // input tracks / insets
    ShadowDark    = Color(0xFF000000),          // drop shadow (low alpha)
    ShadowLight   = Color(0x00000000),          // unused
    TextPrimary   = Color(0xFF111827),          // near-black
    TextSecondary = Color(0xFF6B7280),          // gray-500
    TextMuted     = Color(0xFF9CA3AF),          // gray-400
    TextDisabled  = Color(0xFFD1D5DB),          // gray-300
    IconActive    = Color(0xFF10B981),          // emerald-500
    IconInactive  = Color(0xFF9CA3AF),          // gray-400
    Accent        = Color(0xFF10B981),          // primary emerald
    AccentPressed = Color(0xFF059669),          // pressed emerald
    AccentGlow    = Color(0x1410B981),          // subtle glow
    AccentSurface = Color(0xFFD1FAE5),          // soft emerald surface
    AccentUltraLight = Color(0xFFECFDF5),       // ultra light emerald
    Success       = Color(0xFF10B981),          // emerald
    Warning       = Color(0xFFD97706),          // amber
    Error         = Color(0xFFDC2626),          // red
    ToggleOnTrack = Color(0xFF10B981),          // emerald
    ToggleOffTrack = Color(0xFFE5E7EB),         // gray-200
    ToggleKnob    = Color.White,
    ToggleKnobOn  = Color.White,
    Divider       = Color(0xFFE5E7EB),          // gray-200
    isDark        = false
)

val LocalNeuColors = staticCompositionLocalOf { DarkPalette }

val NeuColors: NeuColorPalette
    @Composable @ReadOnlyComposable
    get() = LocalNeuColors.current
