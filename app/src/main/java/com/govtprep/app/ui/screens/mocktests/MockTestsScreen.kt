package com.govtprep.app.ui.screens.mocktests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.data.model.Exam
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TESTS TAB — 2-column exam grid
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun MockTestsScreen(
    exams: List<Exam>,
    isLoading: Boolean,
    onExamClick: (Exam) -> Unit
) {
    val c = NeuColors
    val accent = Color(0xFF059669)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(c.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Title (full span) ──
        item(span = { GridItemSpan(2) }) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { -20 }
            ) {
                Text(
                    S.allTests,
                    style = NeuType.h2,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                )
            }
        }

        // ── Count (full span) ──
        item(span = { GridItemSpan(2) }) {
            Text(
                "${exams.size} EXAMS",
                style = NeuType.label.copy(color = c.TextMuted, letterSpacing = 1.sp),
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        // ── Loading ──
        if (isLoading) {
            items(6) {
                NeuSkeleton(
                    Modifier.fillMaxWidth().height(120.dp),
                    cornerRadius = 16.dp
                )
            }
            return@LazyVerticalGrid
        }

        // ── Empty ──
        if (exams.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                    Text(S.noTestsAvailable, style = NeuType.bodySecondary)
                }
            }
            return@LazyVerticalGrid
        }

        // ── Exam grid cards ──
        items(exams, key = { it.id }) { exam ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350, 80))
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.Surface)
                        .border(
                            1.dp,
                            if (c.isDark) Color.Transparent else c.Divider,
                            RoundedCornerShape(16.dp)
                        )
                        .bounceClick { onExamClick(exam) }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.08f)),
                        Alignment.Center
                    ) {
                        val ic = exam.icon
                        if (!ic.isNullOrBlank()) {
                            Text(ic, fontSize = 22.sp)
                        } else {
                            Icon(
                                Icons.Outlined.Description, null,
                                tint = accent, modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    // Name
                    Text(
                        exam.name,
                        style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = c.TextPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom spacer
        items(2) { Spacer(Modifier.height(100.dp)) }
    }
}
