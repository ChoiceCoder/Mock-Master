package com.govtprep.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.govtprep.app.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// NEU CARD
// Dark → raised neumorphic dual shadow
// Light → white card with soft drop shadow
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val c = NeuColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.95f else 1f,
        animationSpec = tween(150), label = "card"
    )
    val shadowIntensity by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.3f else 1f,
        animationSpec = tween(150), label = "shadow"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .neuShadow(
                cornerRadius = cornerRadius,
                intensity = shadowIntensity,
                lightColor = c.ShadowLight,
                darkColor = c.ShadowDark,
                surfaceColor = c.Surface,
                isDark = c.isDark
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(16.dp),
        content = content
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// NEU BUTTON
// Dark → raised neumorphic
// Light → emerald filled, white text
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val c = NeuColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150), label = "btn"
    )

    if (c.isDark) {
        // ── DARK: neumorphic raised button ──
        val shadowIntensity by animateFloatAsState(
            targetValue = if (isPressed) 0.2f else 1f,
            animationSpec = tween(150), label = "btnShadow"
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(scale)
                .neuShadow(
                    cornerRadius = 16.dp, intensity = shadowIntensity,
                    lightColor = c.ShadowLight, darkColor = c.ShadowDark,
                    surfaceColor = c.Surface, isDark = true
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = interactionSource, indication = null,
                    enabled = enabled && !isLoading, onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null && !isLoading) {
                    Icon(icon, null, tint = c.TextPrimary.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, style = NeuType.button, color = c.TextPrimary.copy(alpha = if (enabled) 1f else 0.4f))
            }
        }
    } else {
        // ── LIGHT: Emerald filled button, white text ──
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) c.Accent else c.Accent.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = interactionSource, indication = null,
                    enabled = enabled && !isLoading, onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null && !isLoading) {
                    Icon(icon, null, tint = Color.White.copy(alpha = if (enabled) 1f else 0.6f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, style = NeuType.button, color = Color.White.copy(alpha = if (enabled) 1f else 0.6f))
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// NEU ICON BUTTON
// Dark → neumorphic circle
// Light → subtle circle with soft shadow
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NeuIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    iconSize: Dp = 24.dp,
    isActive: Boolean = false,
    contentDescription: String? = null
) {
    val c = NeuColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(150), label = "ico"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .neuCircleShadow(
                size = size,
                surfaceColor = if (c.isDark) {
                    if (isActive) c.SurfaceAlt else c.Surface
                } else {
                    if (isActive) c.AccentSurface else c.SurfaceAlt
                },
                darkColor = c.ShadowDark,
                lightColor = c.ShadowLight,
                isDark = c.isDark
            )
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isActive && c.isDark) {
            Box(modifier = Modifier.size(size * 0.6f).background(c.AccentGlow, CircleShape))
        }
        Icon(
            icon, contentDescription,
            tint = if (isActive) c.IconActive else c.IconInactive,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// NEU TEXT FIELD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    singleLine: Boolean = true
) {
    val c = NeuColors
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .neuInnerShadow(
                cornerRadius = 16.dp,
                surfaceColor = c.SurfaceDeep,
                darkColor = c.ShadowDark,
                lightColor = c.ShadowLight,
                isDark = c.isDark
            )
            .clip(RoundedCornerShape(16.dp))
            .padding(start = 20.dp, end = if (isPassword) 8.dp else 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = NeuType.body, color = c.TextMuted)
                }
                BasicTextField(
                    value = value, onValueChange = onValueChange, singleLine = singleLine,
                    textStyle = NeuType.body.copy(color = c.TextPrimary),
                    cursorBrush = SolidColor(c.Accent),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = c.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// NEU PROGRESS RING
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NeuProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 6.dp,
    trackColor: Color = NeuColors.SurfaceDeep,
    progressColor: Color = NeuColors.Accent,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing), label = "ring"
    )
    Box(
        modifier = modifier.size(size).drawBehind {
            val s = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = s)
            drawArc(color = progressColor, startAngle = -90f, sweepAngle = animatedProgress * 360f, useCenter = false, style = s)
        },
        contentAlignment = Alignment.Center, content = content
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// NEU PROGRESS BAR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NeuProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = NeuColors.SurfaceDeep,
    progressColor: Color = NeuColors.Accent
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "bar"
    )
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(trackColor)) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(height / 2)).background(progressColor))
    }
}

// ── Divider ──
@Composable
fun NeuDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(NeuColors.Divider))
}

// ── Section label ──
@Composable
fun NeuSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = NeuType.label, modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp))
}

// ── Skeleton ──
@Composable
fun NeuSkeleton(modifier: Modifier = Modifier, height: Dp = 16.dp, cornerRadius: Dp = 8.dp) {
    val c = NeuColors

    // Base colour: subtle surface tint
    val baseColor  = if (c.isDark) Color(0xFF2A2A2D) else Color(0xFFE5E7EB)
    // Shimmer highlight colour
    val shineColor = if (c.isDark) Color(0xFF3A3A3E) else Color(0xFFF3F4F6)

    val transition = rememberInfiniteTransition(label = "skel")
    // Sweep from -1× width to +2× width so the glint fully crosses the element
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing, delayMillis = 300),
            repeatMode = RepeatMode.Restart
        ),
        label = "skelX"
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithContent {
                // Solid base
                drawRect(baseColor)
                // Shimmer sweep
                val w = size.width
                val sweepW = w * 0.55f
                val startX = shimmerX * w - sweepW / 2
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            shineColor.copy(alpha = 0.80f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(startX, 0f),
                        end   = androidx.compose.ui.geometry.Offset(startX + sweepW, size.height)
                    )
                )
            }
    )
}
