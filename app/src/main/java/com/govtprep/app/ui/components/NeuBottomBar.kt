package com.govtprep.app.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.ui.theme.*

data class NeuNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
)

@Composable
fun NeuBottomBar(
    items: List<NeuNavItem>,
    currentRoute: String?,
    onItemClick: (NeuNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = NeuColors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!c.isDark) {
                    Modifier.drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().also { p ->
                                p.asFrameworkPaint().apply {
                                    isAntiAlias = true
                                    color = Color.Black.copy(alpha = 0.05f).toArgb()
                                    maskFilter = BlurMaskFilter(16.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                                }
                            }
                            canvas.drawRect(0f, -8.dp.toPx(), size.width, 4.dp.toPx(), paint)
                        }
                    }
                } else Modifier
            )
            .background(c.Background)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NeuNavButton(item = item, isActive = currentRoute == item.route, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun NeuNavButton(item: NeuNavItem, isActive: Boolean, onClick: () -> Unit) {
    val c = NeuColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, tween(150), label = "nav")

    // Animate circle background alpha for smooth active transition (light mode)
    val lightCircleAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(200),
        label = "lightCircle"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (c.isDark) {
            // ── DARK: neumorphic raised circle ──
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(scale)
                    .neuCircleShadow(
                        size = 60.dp,
                        surfaceColor = if (isActive) c.SurfaceAlt else c.Surface,
                        darkColor = c.ShadowDark,
                        lightColor = c.ShadowLight,
                        isDark = true
                    )
                    .clip(CircleShape)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) Box(Modifier.size(36.dp).background(c.AccentGlow, CircleShape))
                Icon(
                    if (isActive) item.activeIcon else item.icon, item.label,
                    tint = if (isActive) c.IconActive else c.IconInactive,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // ── LIGHT: circle with drop shadow for definition ──
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(scale)
                    .neuCircleShadow(
                        size = 60.dp,
                        surfaceColor = if (isActive) c.Accent.copy(alpha = 0.12f) else Color(0xFFF3F4F6),
                        darkColor = c.ShadowDark,
                        lightColor = c.ShadowLight,
                        isDark = false
                    )
                    .clip(CircleShape)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isActive) item.activeIcon else item.icon, item.label,
                    tint = if (isActive) c.Accent else c.IconInactive,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(1.dp))

        Text(
            item.label, fontSize = 10.sp,
            color = if (c.isDark) {
                if (isActive) c.TextPrimary else c.TextMuted
            } else {
                if (isActive) c.Accent else c.TextMuted
            }
        )
    }
}
