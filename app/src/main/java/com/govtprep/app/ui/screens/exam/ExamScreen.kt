package com.govtprep.app.ui.screens.exam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.govtprep.app.data.model.Subject
import com.govtprep.app.data.model.TestSet
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EXAM DETAIL SCREEN
// Handles: sub-exam picker (grid) → subjects (grid) → test sets (list)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ExamScreen(
    state: ExamState,
    onBack: () -> Unit,
    onSubExamSelect: (String) -> Unit,
    onSubExamBack: () -> Unit,
    onStartTest: (testSetId: String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val c = NeuColors
    val accent = Color(0xFF059669)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(c.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Header ──
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { -24 }
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    NeuIconButton(
                        icon = Icons.Outlined.ArrowBack,
                        onClick = {
                            if (state.selectedSubExam != null) onSubExamBack()
                            else onBack()
                        },
                        size = 44.dp,
                        iconSize = 20.dp
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(state.exam?.name ?: "Exam", style = NeuType.displayLarge)

                    state.exam?.description?.let { desc ->
                        Spacer(Modifier.height(6.dp))
                        Text(desc, style = NeuType.bodySecondary, maxLines = 2)
                    }

                    if (state.selectedSubExam != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "▸ ${state.selectedSubExam}",
                            style = NeuType.small.copy(color = c.Accent)
                        )
                    }
                }
            }
        }

        // ── Loading ──
        if (state.isLoading) {
            items(4) {
                NeuSkeleton(
                    Modifier.fillMaxWidth().height(80.dp)
                        .padding(horizontal = 24.dp, vertical = 5.dp),
                    cornerRadius = 16.dp
                )
            }
            return@LazyColumn
        }

        // ── Error ──
        state.error?.let { err ->
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.Error.copy(alpha = 0.15f))
                        .padding(16.dp)
                ) {
                    Text(err, style = NeuType.small.copy(color = c.Error.copy(alpha = 0.9f)))
                }
            }
            return@LazyColumn
        }

        // ━━━ PHASE 1: Sub-exam picker — 2-column GRID ━━━
        if (!state.isFlat && state.selectedSubExam == null && state.subExams.isNotEmpty()) {
            item {
                NeuSectionLabel(S.selectSubExam)
                Spacer(Modifier.height(4.dp))
            }

            // Chunk sub-exams into rows of 2
            val rows = state.subExams.chunked(2)
            rows.forEachIndexed { rowIndex, rowItems ->
                item(key = "subexam_row_$rowIndex") {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300, 100 + rowIndex * 80)) +
                                slideInVertically(tween(400, 100 + rowIndex * 80)) { 24 }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { subExam ->
                                SubExamGridCard(
                                    name = subExam.name,
                                    nameHindi = subExam.nameHindi,
                                    icon = subExam.icon,
                                    isActive = subExam.isActive,
                                    subjectCount = subExam.subjectCount,
                                    accent = accent,
                                    onClick = { if (subExam.isActive) onSubExamSelect(subExam.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Spacer if odd number of items
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            return@LazyColumn
        }

        // ━━━ PHASE 2: Subjects (grid) + Test Sets (list) ━━━

        // Mock tests section (list — these are tests)
        if (state.mockTests.isNotEmpty()) {
            item {
                NeuSectionLabel(S.fullMockTests)
                Spacer(Modifier.height(4.dp))
            }
            itemsIndexed(state.mockTests) { index, test ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(300, index * 50)) +
                            slideInVertically(tween(400, index * 50)) { 24 }
                ) {
                    TestSetRow(
                        testSet = test,
                        onClick = { onStartTest(test.id) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Subject-wise tests
        val subjects = state.activeSubjects
        if (subjects.isNotEmpty()) {
            subjects.forEachIndexed { sIndex, subject ->
                val tests = state.testSetsBySubject[subject.id] ?: emptyList()

                item(key = "subject_header_${subject.id}") {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300, 100 + sIndex * 80))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            if (sIndex > 0) {
                                NeuDivider(Modifier.padding(vertical = 16.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(subject.icon ?: "📘", fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(subject.name, style = NeuType.h3)
                                Spacer(Modifier.weight(1f))
                                Text("${tests.size} tests", style = NeuType.caption)
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                if (tests.isEmpty()) {
                    item(key = "empty_${subject.id}") {
                        Box(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(S.noTestsAvailable, style = NeuType.caption)
                        }
                    }
                }

                itemsIndexed(tests, key = { _, t -> t.id }) { tIndex, test ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300, 150 + sIndex * 80 + tIndex * 50)) +
                                slideInVertically(tween(400, 150 + sIndex * 80 + tIndex * 50)) { 24 }
                    ) {
                        TestSetRow(
                            testSet = test,
                            onClick = { onStartTest(test.id) },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        } else if (state.mockTests.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Info, null,
                            tint = c.TextMuted, modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(S.noTestsAvailable, style = NeuType.bodySecondary)
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUB-EXAM GRID CARD (2-column)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SubExamGridCard(
    name: String,
    nameHindi: String?,
    icon: String?,
    isActive: Boolean,
    subjectCount: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = NeuColors
    val alpha = if (isActive) 1f else 0.5f

    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.Surface)
            .border(
                1.dp,
                if (c.isDark) {
                    if (isActive) accent.copy(alpha = 0.2f) else Color.Transparent
                } else c.Divider,
                RoundedCornerShape(16.dp)
            )
            .then(if (isActive) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji icon
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isActive) accent.copy(alpha = if (c.isDark) 0.15f else 0.08f)
                    else c.SurfaceDeep
                ),
            Alignment.Center
        ) {
            Text(icon ?: "📝", fontSize = 22.sp)
        }
        Spacer(Modifier.height(10.dp))

        // Name
        Text(
            name,
            style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = c.TextPrimary.copy(alpha = alpha),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Hindi name (only in Hindi mode)
        val isHindi by LanguageManager.isHindi.collectAsState()
        if (isHindi && nameHindi != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                nameHindi,
                style = NeuType.caption.copy(fontSize = 10.sp, color = c.TextMuted.copy(alpha = alpha)),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(6.dp))

        // Bottom: subject count or coming soon
        if (isActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
                Text(
                    "$subjectCount ${S.subjects}",
                    style = NeuType.caption.copy(fontSize = 10.sp)
                )
            }
        } else {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.SurfaceDeep)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    S.comingSoon,
                    style = NeuType.caption.copy(
                        fontSize = 8.sp, letterSpacing = 0.8.sp, color = c.TextMuted
                    )
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST SET ROW (stays as list)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun TestSetRow(
    testSet: TestSet,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeuCard(modifier = modifier, cornerRadius = 16.dp, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    testSet.title,
                    style = NeuType.body.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${testSet.totalQuestions}Q", style = NeuType.caption)
                    DotSep()
                    Text("${testSet.durationMinutes}min", style = NeuType.caption)
                    DotSep()
                    Text("${testSet.totalMarks.toInt()} marks", style = NeuType.caption)
                    if (!testSet.isFree) {
                        DotSep()
                        Text(
                            S.pro,
                            style = NeuType.caption.copy(
                                color = NeuColors.Warning,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            NeuIconButton(
                icon = Icons.Outlined.PlayArrow,
                onClick = onClick,
                size = 40.dp,
                iconSize = 18.dp
            )
        }
    }
}

@Composable
private fun DotSep() {
    Text(" · ", style = NeuType.caption.copy(color = NeuColors.TextMuted))
}
