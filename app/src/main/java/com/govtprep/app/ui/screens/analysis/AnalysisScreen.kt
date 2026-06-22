package com.govtprep.app.ui.screens.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.govtprep.app.data.model.Attempt
import com.govtprep.app.data.model.UserStats
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ANALYSIS TAB — Performance Tracking
// Clean · Readable · No complex analytics
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun AnalysisScreen(
    stats: UserStats?,
    attempts: List<Attempt>,
    isLoading: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuColors.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Title
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { -20 }
            ) {
                Text(
                    S.analytics,
                    style = NeuType.h2,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }
        }

        if (isLoading) {
            item { AnalysisLoadingSkeleton() }
            return@LazyColumn
        }

        // ━━━ 1. OVERALL STATS CARD ━━━
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 80)) + slideInVertically(tween(500, 80)) { 30 }
            ) {
                Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    NeuCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(S.overallStats, style = NeuType.label)
                            Spacer(Modifier.height(16.dp))

                            Row(Modifier.fillMaxWidth()) {
                                StatColumn(
                                    value = "${stats?.averageScore?.toInt() ?: 0}%",
                                    label = S.accuracy,
                                    modifier = Modifier.weight(1f)
                                )
                                StatColumn(
                                    value = "${stats?.totalAttempts ?: 0}",
                                    label = S.totalTests,
                                    modifier = Modifier.weight(1f)
                                )
                                StatColumn(
                                    value = "${"%.1f".format(stats?.averageScore ?: 0.0)}",
                                    label = S.avgScore,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ━━━ 2. SCORE TREND (line graph) ━━━
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 160)) + slideInVertically(tween(500, 160)) { 30 }
            ) {
                Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    NeuCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(S.scoreTrend, style = NeuType.label)
                            Spacer(Modifier.height(16.dp))

                            if (attempts.size >= 2) {
                                ScoreTrendGraph(
                                    attempts = attempts.reversed().takeLast(10),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        S.takeMoreTests,
                                        style = NeuType.bodySecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ━━━ 3. SUBJECT-WISE PERFORMANCE (from attempts) ━━━
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 240)) + slideInVertically(tween(500, 240)) { 30 }
            ) {
                val subjectPerf = deriveSubjectPerformance(attempts)

                Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    NeuCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(S.subjectPerformance, style = NeuType.label)
                            Spacer(Modifier.height(16.dp))

                            if (subjectPerf.isEmpty()) {
                                Text(
                                    S.noSubjectData,
                                    style = NeuType.bodySecondary
                                )
                            } else {
                                subjectPerf.forEachIndexed { index, (name, pct) ->
                                    if (index > 0) Spacer(Modifier.height(14.dp))
                                    SubjectBar(name = name, percentage = pct)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ━━━ 4. MISTAKE REVIEW ━━━
        val wrongAttempts = attempts.filter { (it.incorrectCount ?: 0) > 0 }.take(5)

        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, 320))
            ) {
                NeuSectionLabel(
                    S.mistakeReview,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (wrongAttempts.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(S.noMistakes, style = NeuType.bodySecondary)
                }
            }
        }

        itemsIndexed(wrongAttempts) { index, attempt ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, 360 + index * 50)) +
                        slideInVertically(tween(400, 360 + index * 50)) { 20 }
            ) {
                MistakeRow(
                    attempt = attempt,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Stat column ──
@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NeuType.h3.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Text(label, style = NeuType.caption)
    }
}

// ── Score trend line graph ──
@Composable
private fun ScoreTrendGraph(attempts: List<Attempt>, modifier: Modifier = Modifier) {
    val scores = attempts.map { a ->
        if (a.totalMarks != null && a.totalMarks > 0)
            ((a.score ?: 0.0) / a.totalMarks * 100).toFloat()
        else 0f
    }

    var animProgress by remember { mutableStateOf(0f) }
    val animValue by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(800),
        label = "graph"
    )
    LaunchedEffect(scores) { animProgress = 1f }

    val gridColor = NeuColors.Divider
    val lineColor = NeuColors.Accent
    val dotCenter = NeuColors.Background

    Canvas(modifier = modifier) {
        if (scores.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val padding = 8.dp.toPx()
        val graphW = w - padding * 2
        val graphH = h - padding * 2
        val maxScore = 100f
        val stepX = graphW / (scores.size - 1)

        // Grid lines
        for (i in 0..4) {
            val y = padding + graphH * (1 - i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(w - padding, y),
                strokeWidth = 0.5.dp.toPx()
            )
        }

        // Line path
        val pointCount = (scores.size * animValue).toInt().coerceAtLeast(2)
        val path = Path()
        val points = mutableListOf<Offset>()

        for (i in 0 until pointCount) {
            val x = padding + i * stepX
            val y = padding + graphH * (1 - scores[i] / maxScore)
            points.add(Offset(x, y))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw dots
        points.forEach { pt ->
            drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = pt)
            drawCircle(color = dotCenter, radius = 1.5.dp.toPx(), center = pt)
        }
    }
}

// ── Subject performance bar ──
@Composable
private fun SubjectBar(name: String, percentage: Int) {
    var animTarget by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (animTarget) percentage / 100f else 0f,
        animationSpec = tween(600),
        label = "bar"
    )
    LaunchedEffect(Unit) { animTarget = true }

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = NeuType.body)
            Text(
                "$percentage%",
                style = NeuType.body.copy(
                    fontWeight = FontWeight.Medium,
                    color = when {
                        percentage >= 70 -> NeuColors.Success
                        percentage >= 40 -> NeuColors.Warning
                        else -> NeuColors.Error
                    }
                )
            )
        }
        Spacer(Modifier.height(6.dp))
        NeuProgressBar(
            progress = animProgress,
            height = 6.dp,
            trackColor = NeuColors.SurfaceDeep,
            progressColor = when {
                percentage >= 70 -> NeuColors.Success
                percentage >= 40 -> NeuColors.Warning
                else -> NeuColors.Error
            }
        )
    }
}

// ── Mistake review row ──
@Composable
private fun MistakeRow(attempt: Attempt, modifier: Modifier = Modifier) {
    val wrongCount = attempt.incorrectCount ?: 0

    NeuCard(modifier = modifier, cornerRadius = 16.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wrong count badge
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeuColors.Error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$wrongCount",
                    style = NeuType.body.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = NeuColors.Error
                    )
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    attempt.testSet?.title ?: "Test",
                    style = NeuType.body.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$wrongCount wrong answers",
                    style = NeuType.caption
                )
            }

            Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Review",
                tint = NeuColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Derive subject performance from attempts ──
private fun deriveSubjectPerformance(attempts: List<Attempt>): List<Pair<String, Int>> {
    val bySubject = mutableMapOf<String, MutableList<Double>>()

    attempts.forEach { a ->
        val name = a.testSet?.title?.split(" - ")?.firstOrNull()
            ?: a.testSet?.title ?: return@forEach
        val pct = if (a.totalMarks != null && a.totalMarks > 0)
            (a.score ?: 0.0) / a.totalMarks * 100 else 0.0
        bySubject.getOrPut(name) { mutableListOf() }.add(pct)
    }

    return bySubject.map { (name, scores) ->
        name to scores.average().toInt()
    }.sortedByDescending { it.second }.take(6)
}

// ── Loading ──
@Composable
private fun AnalysisLoadingSkeleton() {
    Column(Modifier.padding(24.dp)) {
        NeuSkeleton(Modifier.fillMaxWidth().height(100.dp), cornerRadius = 20.dp)
        Spacer(Modifier.height(16.dp))
        NeuSkeleton(Modifier.fillMaxWidth().height(160.dp), cornerRadius = 20.dp)
        Spacer(Modifier.height(16.dp))
        NeuSkeleton(Modifier.fillMaxWidth().height(140.dp), cornerRadius = 20.dp)
        Spacer(Modifier.height(24.dp))
        repeat(3) {
            NeuSkeleton(Modifier.fillMaxWidth().height(64.dp), cornerRadius = 16.dp)
            Spacer(Modifier.height(8.dp))
        }
    }
}
