package com.govtprep.app.ui.screens.pyq

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.data.model.*
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

@Composable
fun PYQScreen(
    state: PYQState,
    vm: PYQViewModel,
    onBack: () -> Unit,
    onStartTest: (String) -> Unit
) {
    val c = NeuColors
    val accent = Color(0xFF059669)

    BackHandler {
        when (state.level) {
            PYQLevel.EXAMS -> onBack()
            else -> vm.goBack()
        }
    }

    Column(Modifier.fillMaxSize().background(c.Background)) {
        // ── HEADER ──
        PYQHeader(state = state, accent = accent, onBack = {
            if (state.level == PYQLevel.EXAMS) onBack() else vm.goBack()
        })

        // ── CONTENT ──
        if (state.isLoading) {
            LoadingSkeleton()
        } else {
            when (state.level) {
                PYQLevel.EXAMS -> ExamPicker(state.exams, accent) { vm.selectExam(it) }
                PYQLevel.SUB_EXAMS -> SubExamPicker(state, accent) { vm.selectSubExam(it) }
                PYQLevel.YEARS -> YearPicker(state, accent) { vm.selectYear(it) }
                PYQLevel.TESTS -> TestList(state, accent, onStartTest)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HEADER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun PYQHeader(state: PYQState, accent: Color, onBack: () -> Unit) {
    val c = NeuColors
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = accent.copy(alpha = if (c.isDark) 0.3f else 0.12f),
                    topLeft = Offset(0f, size.height - 3.dp.toPx()),
                    size = Size(size.width, 3.dp.toPx())
                )
            }
            .background(c.Surface)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = c.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("PYQ Papers", style = NeuType.h3.copy(letterSpacing = (-0.3).sp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when (state.level) {
                            PYQLevel.EXAMS -> "Select Exam"
                            PYQLevel.SUB_EXAMS -> state.selectedExam?.name ?: "Select Sub-Exam"
                            PYQLevel.YEARS -> "${state.selectedSubExam ?: state.selectedExam?.name} • Select Year"
                            PYQLevel.TESTS -> "${state.selectedSubExam ?: state.selectedExam?.name} • ${state.selectedYear}"
                        },
                        style = NeuType.caption.copy(color = accent, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Breadcrumb
        val crumbs = mutableListOf("Exams")
        state.selectedExam?.let { crumbs.add(it.name) }
        state.selectedSubExam?.let { crumbs.add(it) }
        state.selectedYear?.let { crumbs.add(it.toString()) }
        if (crumbs.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                crumbs.forEachIndexed { i, name ->
                    if (i > 0) Text("›", color = c.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 2.dp))
                    val isLast = i == crumbs.lastIndex
                    Text(
                        name,
                        fontSize = 11.sp,
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isLast) accent else c.TextMuted,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (isLast) Modifier.background(accent.copy(alpha = if (c.isDark) 0.15f else 0.08f))
                                else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LEVEL 1 — EXAM PICKER (2-column grid)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ExamPicker(exams: List<Exam>, accent: Color, onSelect: (Exam) -> Unit) {
    val c = NeuColors

    if (exams.isEmpty()) {
        EmptyState("No exams available", Icons.Outlined.Inventory2)
    } else {
        Column {
            CountLabel(exams.size, "Exams")
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exams, key = { it.id }) { exam ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.Surface)
                            .border(1.dp, if (c.isDark) Color.Transparent else c.Divider, RoundedCornerShape(16.dp))
                            .bounceClick { onSelect(exam) }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.08f)),
                            Alignment.Center
                        ) {
                            val ic = exam.icon
                            if (!ic.isNullOrBlank()) Text(ic, fontSize = 22.sp)
                            else Icon(Icons.Outlined.Description, null, tint = accent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            exam.name,
                            style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            color = c.TextPrimary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                        )
                    }
                }
                items(2) { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LEVEL 2 — SUB-EXAM PICKER (list cards like ExamScreen)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SubExamPicker(state: PYQState, accent: Color, onSelect: (String) -> Unit) {
    val c = NeuColors
    val s = S

    if (state.subExams.isEmpty()) {
        EmptyState("No sub-exams found", Icons.Outlined.Category)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CountLabel(state.subExams.size, "Sub-Exams")
            }
            items(state.subExams, key = { it.name }) { subExam ->
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    onClick = if (subExam.isActive) ({ onSelect(subExam.name) }) else null
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (c.isDark) c.SurfaceDeep else c.AccentSurface),
                            Alignment.Center
                        ) {
                            Text(subExam.icon ?: "📝", fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(14.dp))

                        // Name + hindi + subject count
                        Column(Modifier.weight(1f)) {
                            val alpha = if (subExam.isActive) 1f else 0.5f
                            Text(
                                subExam.name,
                                style = NeuType.body.copy(fontWeight = FontWeight.Medium),
                                color = c.TextPrimary.copy(alpha = alpha),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            subExam.nameHindi?.let {
                                val isHindi by LanguageManager.isHindi.collectAsState()
                                if (isHindi) Text(it, style = NeuType.caption.copy(color = c.TextMuted.copy(alpha = alpha)))
                            }
                            if (subExam.isActive) {
                                Spacer(Modifier.height(2.dp))
                                Text("${subExam.paperCount} papers", style = NeuType.caption)
                            }
                        }

                        // Chevron or Coming Soon
                        if (subExam.isActive) {
                            Icon(Icons.Outlined.ChevronRight, null, tint = c.TextMuted, modifier = Modifier.size(20.dp))
                        } else {
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(c.SurfaceDeep)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    s.comingSoon,
                                    style = NeuType.caption.copy(
                                        fontSize = 9.sp, letterSpacing = 0.8.sp, color = c.TextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LEVEL 3 — YEAR PICKER (grid of year cards)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun YearPicker(state: PYQState, accent: Color, onSelect: (Int) -> Unit) {
    val c = NeuColors
    val years = state.years
    val label = state.selectedSubExam ?: state.selectedExam?.name ?: "this exam"

    if (years.isEmpty()) {
        EmptyState("No PYQ papers available for $label yet", Icons.Outlined.EventBusy)
    } else {
        Column {
            CountLabel(years.size, "Years • $label")
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(years) { year ->
                    val testCount = state.filteredTests.count { it.year == year }
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.Surface)
                            .border(1.dp, if (c.isDark) accent.copy(alpha = 0.2f) else c.Divider, RoundedCornerShape(14.dp))
                            .bounceClick { onSelect(year) }
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$year", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$testCount ${if (testCount == 1) "paper" else "papers"}",
                            style = NeuType.caption.copy(fontSize = 11.sp), color = c.TextMuted
                        )
                    }
                }
                items(3) { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LEVEL 4 — DATE + SHIFT LIST
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun TestList(state: PYQState, accent: Color, onStartTest: (String) -> Unit) {
    val c = NeuColors
    val tests = state.testsForYear

    if (tests.isEmpty()) {
        EmptyState("No papers for ${state.selectedYear}", Icons.Outlined.EventBusy)
    } else {
        // Group by exam_date, sort dates descending
        val grouped = tests
            .groupBy { it.examDate ?: "Unknown" }
            .toSortedMap(compareByDescending { it })

        val totalShifts = tests.size
        Column {
            CountLabel(totalShifts, "Papers • ${state.selectedYear}")
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                grouped.forEach { (date, dateTests) ->
                    // Date header
                    item(key = "date_$date") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.08f)),
                                Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = accent, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                formatExamDate(date),
                                style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                                color = c.TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${dateTests.size} ${if (dateTests.size == 1) "shift" else "shifts"}",
                                style = NeuType.caption.copy(fontSize = 11.sp)
                            )
                        }
                    }

                    // Shift cards for this date
                    val sortedShifts = dateTests.sortedBy { it.shift ?: 0 }
                    items(sortedShifts, key = { it.id }) { test ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 42.dp) // indent under date header
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (c.isDark) Color.Transparent else c.Divider, RoundedCornerShape(12.dp))
                                .background(c.Surface)
                                .bounceClick { onStartTest(test.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shift badge
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = if (c.isDark) 0.12f else 0.06f)),
                                Alignment.Center
                            ) {
                                Text(
                                    "S${test.shift ?: "?"}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent
                                )
                            }
                            Spacer(Modifier.width(12.dp))

                            // Shift info
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Shift ${test.shift ?: "?"}",
                                    style = NeuType.body.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
                                    color = c.TextPrimary
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MetaChip("${test.totalQuestions}Q", c)
                                    MetaChip("${test.durationMinutes} min", c)
                                    MetaChip("${test.totalMarks} marks", c)
                                }
                                if (!test.isFree) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                        color = accent, letterSpacing = 1.sp,
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                            .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.08f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // Play icon
                            Icon(
                                Icons.Outlined.PlayCircleOutline, null,
                                tint = accent, modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

private fun formatExamDate(dateStr: String): String {
    if (dateStr == "Unknown" || dateStr.isBlank()) return dateStr
    return try {
        val parts = dateStr.split("-") // "2024-02-04"
        if (parts.size != 3) return dateStr
        val day = parts[2].toIntOrNull() ?: return dateStr
        val monthIdx = (parts[1].toIntOrNull() ?: return dateStr) - 1
        val year = parts[0]
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "$day ${months.getOrElse(monthIdx) { "?" }} $year"
    } catch (_: Exception) { dateStr }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun LoadingSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeuSkeleton(Modifier.fillMaxWidth(0.4f), height = 14.dp, cornerRadius = 7.dp)
        Spacer(Modifier.height(4.dp))
        repeat(5) { NeuSkeleton(Modifier.fillMaxWidth(), height = 72.dp, cornerRadius = 14.dp) }
    }
}

@Composable
private fun CountLabel(count: Int, label: String) {
    val c = NeuColors
    Text(
        "$count $label",
        style = NeuType.label.copy(color = c.TextMuted, letterSpacing = 1.sp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun MetaChip(text: String, c: NeuColorPalette) {
    Text(
        text, fontSize = 10.sp, color = c.TextMuted, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.SurfaceDeep)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun EmptyState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val c = NeuColors
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(Modifier.padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(c.SurfaceAlt),
                Alignment.Center
            ) {
                Icon(icon, null, tint = c.TextMuted.copy(0.5f), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(text, style = NeuType.bodySecondary, textAlign = TextAlign.Center)
        }
    }
}
