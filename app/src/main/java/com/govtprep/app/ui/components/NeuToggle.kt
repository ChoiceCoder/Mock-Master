package com.govtprep.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.ui.theme.NeuColors
import com.govtprep.app.ui.theme.NeuType
import kotlin.math.roundToInt

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// NEUMORPHIC TOGGLE — Clean & Reliable
// Concave track + raised knob + ON/OFF text
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NeuToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 60.dp,
    height: Dp = 30.dp,
    label: String? = null
) {
    val colors = NeuColors
    val density = LocalDensity.current
    val knobSize = height - 6.dp
    val travelPx = with(density) { (width - knobSize - 6.dp).toPx() }
    val restPx   = with(density) { 3.dp.toPx() }

    val knobOffsetPx by animateFloatAsState(
        targetValue = if (checked) travelPx else restPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "knob"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.ToggleOnTrack else colors.ToggleOffTrack,
        animationSpec = tween(200), label = "track"
    )

    val knobColor by animateColorAsState(
        targetValue = if (checked) colors.ToggleKnobOn else colors.ToggleKnob,
        animationSpec = tween(200), label = "knobC"
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = NeuType.body.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
        }

        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(trackColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onCheckedChange(!checked) }
                )
        ) {
            // ON/OFF label — sits in the opposite half from the knob
            if (checked) {
                Text(
                    "ON",
                    style = NeuType.caption.copy(
                        fontWeight = FontWeight.Bold, fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.9f), letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 7.dp)
                )
            } else {
                Text(
                    "OFF",
                    style = NeuType.caption.copy(
                        fontWeight = FontWeight.Bold, fontSize = 9.sp,
                        color = colors.TextMuted.copy(alpha = 0.7f), letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
                )
            }

            // Knob
            Box(
                modifier = Modifier
                    .offset { IntOffset(knobOffsetPx.roundToInt(), 0) }
                    .padding(vertical = 3.dp)
                    .size(knobSize)
                    .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(knobColor)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) {
                        Box(
                            Modifier.width(1.dp).height(10.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (checked) colors.TextMuted.copy(alpha = 0.3f)
                                    else colors.TextMuted.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            }
        }
    }
}
