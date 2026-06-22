package com.govtprep.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SHADOW SYSTEM
// Dark mode  → Neumorphic (dual shadow: dark + light)
// Light mode → Clean elevation (single soft drop shadow)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

fun Modifier.neuShadow(
    cornerRadius: Dp = 20.dp,
    shadowRadius: Dp = 20.dp,
    lightOffset: Dp = (-8).dp,
    darkOffset: Dp = 8.dp,
    lightColor: Color = DarkPalette.ShadowLight,
    darkColor: Color = DarkPalette.ShadowDark,
    surfaceColor: Color = DarkPalette.Surface,
    intensity: Float = 1f,
    isDark: Boolean = true
): Modifier = this.drawBehind {
    val cornerPx = cornerRadius.toPx()

    drawIntoCanvas { canvas ->
        if (isDark) {
            // ── DARK MODE: Neumorphic dual shadow ──
            val blurPx = shadowRadius.toPx()

            // Dark shadow — bottom-right
            val darkPaint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = darkColor.copy(alpha = 0.7f * intensity).toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
            }
            val dOff = darkOffset.toPx()
            canvas.drawRoundRect(
                dOff, dOff,
                size.width + dOff, size.height + dOff,
                cornerPx, cornerPx, darkPaint
            )

            // Light highlight — top-left
            val lightPaint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = lightColor.copy(alpha = 0.25f * intensity).toArgb()
                    maskFilter = BlurMaskFilter(blurPx * 0.7f, BlurMaskFilter.Blur.NORMAL)
                }
            }
            val lOff = lightOffset.toPx()
            canvas.drawRoundRect(
                lOff, lOff,
                size.width + lOff, size.height + lOff,
                cornerPx, cornerPx, lightPaint
            )
        } else {
            // ── LIGHT MODE: Clean single drop shadow ──
            val blurPx = (shadowRadius * 0.6f).toPx()
            val yOffset = 3.dp.toPx()
            val shadowPaint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = darkColor.copy(alpha = 0.08f * intensity).toArgb()
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawRoundRect(
                0f, yOffset,
                size.width, size.height + yOffset,
                cornerPx, cornerPx, shadowPaint
            )
        }
    }

    // Surface fill
    drawRoundRect(
        color = surfaceColor,
        cornerRadius = CornerRadius(cornerPx),
        size = size
    )
}

fun Modifier.neuInnerShadow(
    cornerRadius: Dp = 16.dp,
    surfaceColor: Color = DarkPalette.SurfaceDeep,
    darkColor: Color = DarkPalette.ShadowDark,
    lightColor: Color = DarkPalette.ShadowLight,
    isDark: Boolean = true
): Modifier = this.drawBehind {
    val cornerPx = cornerRadius.toPx()
    val w = size.width
    val h = size.height

    if (isDark) {
        // ── DARK: Neumorphic inner shadow ──
        drawIntoCanvas { canvas ->
            val fillPaint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = surfaceColor.toArgb()
                }
            }
            canvas.drawRoundRect(0f, 0f, w, h, cornerPx, cornerPx, fillPaint)
        }

        drawRoundRect(
            color = darkColor.copy(alpha = 0.35f),
            topLeft = Offset.Zero,
            size = Size(w * 0.5f, h * 0.5f),
            cornerRadius = CornerRadius(cornerPx)
        )
        drawRoundRect(
            color = lightColor.copy(alpha = 0.08f),
            topLeft = Offset(w * 0.4f, h * 0.4f),
            size = Size(w * 0.6f, h * 0.6f),
            cornerRadius = CornerRadius(cornerPx)
        )

        val inset = 3.dp.toPx()
        drawRoundRect(
            color = surfaceColor,
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            cornerRadius = CornerRadius((cornerPx - inset).coerceAtLeast(0f))
        )
    } else {
        // ── LIGHT: Simple inset fill (no inner shadow) ──
        drawRoundRect(
            color = surfaceColor,
            cornerRadius = CornerRadius(cornerPx),
            size = size
        )
    }
}

fun Modifier.neuCircleShadow(
    size: Dp = 60.dp,
    surfaceColor: Color = DarkPalette.SurfaceAlt,
    darkColor: Color = DarkPalette.ShadowDark,
    lightColor: Color = DarkPalette.ShadowLight,
    isDark: Boolean = true
): Modifier = this.drawBehind {
    val radius = this.size.width / 2f

    drawIntoCanvas { canvas ->
        if (isDark) {
            // ── DARK: Neumorphic circle ──
            val darkPaint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = darkColor.copy(alpha = 0.6f).toArgb()
                    maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawCircle(
                Offset(radius + 5.dp.toPx(), radius + 5.dp.toPx()),
                radius, darkPaint
            )

            val lightPaint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = lightColor.copy(alpha = 0.2f).toArgb()
                    maskFilter = BlurMaskFilter(8.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawCircle(
                Offset(radius - 4.dp.toPx(), radius - 4.dp.toPx()),
                radius, lightPaint
            )
        } else {
            // ── LIGHT: Subtle drop shadow circle ──
            val shadowPaint = Paint().also { p ->
                p.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = darkColor.copy(alpha = 0.06f).toArgb()
                    maskFilter = BlurMaskFilter(8.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawCircle(
                Offset(radius, radius + 2.dp.toPx()),
                radius, shadowPaint
            )
        }
    }

    // Circle fill
    drawCircle(color = surfaceColor, radius = radius)
}

@Composable
fun neuPress(
    onClick: () -> Unit
): Pair<Modifier, Boolean> {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150),
        label = "pressScale"
    )
    val modifier = Modifier
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    return modifier to isPressed
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// BOUNCE CLICK — Chainable press-scale modifier
// Use: Modifier.bounceClick { doSomething() }
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

fun Modifier.bounceClick(
    scaleTo: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleTo else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounce"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
