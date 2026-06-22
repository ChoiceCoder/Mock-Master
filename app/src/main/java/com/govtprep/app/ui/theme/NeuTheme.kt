package com.govtprep.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Dynamic Typography — reads from current palette
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class NeuTypography(
    val displayLarge: TextStyle,
    val metric: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val body: TextStyle,
    val bodySecondary: TextStyle,
    val small: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val caption: TextStyle
)

val LocalNeuType = staticCompositionLocalOf {
    buildNeuType(DarkPalette)
}

fun buildNeuType(c: NeuColorPalette) = NeuTypography(
    displayLarge = TextStyle(
        fontSize = 34.sp, fontWeight = FontWeight.SemiBold,
        color = c.TextPrimary, letterSpacing = (-0.5).sp, lineHeight = 40.sp
    ),
    metric = TextStyle(
        fontSize = 38.sp, fontWeight = FontWeight.SemiBold,
        color = c.TextPrimary, letterSpacing = (-0.8).sp, lineHeight = 44.sp
    ),
    h2 = TextStyle(
        fontSize = 24.sp, fontWeight = FontWeight.Medium,
        color = c.TextPrimary, letterSpacing = (-0.3).sp, lineHeight = 30.sp
    ),
    h3 = TextStyle(
        fontSize = 19.sp, fontWeight = FontWeight.Medium,
        color = c.TextPrimary, letterSpacing = (-0.2).sp, lineHeight = 26.sp
    ),
    body = TextStyle(
        fontSize = 16.sp, fontWeight = FontWeight.Normal,
        color = c.TextPrimary, lineHeight = 24.sp
    ),
    bodySecondary = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.Normal,
        color = c.TextSecondary, lineHeight = 22.sp
    ),
    small = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal,
        color = c.TextSecondary, lineHeight = 20.sp
    ),
    label = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Medium,
        color = c.TextMuted, letterSpacing = 1.2.sp, lineHeight = 18.sp
    ),
    button = TextStyle(
        fontSize = 16.sp, fontWeight = FontWeight.Medium,
        color = c.TextPrimary, letterSpacing = 0.sp
    ),
    caption = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Normal,
        color = c.TextMuted, lineHeight = 16.sp
    )
)

// Global accessor
val NeuType: NeuTypography
    @Composable @ReadOnlyComposable
    get() = LocalNeuType.current

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Theme wrapper
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun GovtPrepTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (isDarkTheme) DarkPalette else LightPalette
    val typography = remember(isDarkTheme) { buildNeuType(palette) }

    val materialScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = palette.Accent, onPrimary = palette.TextPrimary,
            background = palette.Background, onBackground = palette.TextPrimary,
            surface = palette.Surface, onSurface = palette.TextPrimary,
            surfaceVariant = palette.SurfaceAlt, onSurfaceVariant = palette.TextSecondary,
            error = palette.Error, onError = palette.TextPrimary,
            outline = palette.Divider, surfaceTint = Color.Transparent,
        )
    } else {
        lightColorScheme(
            primary = palette.Accent, onPrimary = Color.White,
            background = palette.Background, onBackground = palette.TextPrimary,
            surface = palette.Surface, onSurface = palette.TextPrimary,
            surfaceVariant = palette.SurfaceAlt, onSurfaceVariant = palette.TextSecondary,
            error = palette.Error, onError = Color.White,
            outline = palette.Divider, surfaceTint = Color.Transparent,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.Background.toArgb()
            window.navigationBarColor = palette.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    CompositionLocalProvider(
        LocalNeuColors provides palette,
        LocalNeuType provides typography
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            content = content
        )
    }
}
