package com.govtprep.app.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.govtprep.app.ui.components.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.govtprep.app.data.model.*
import com.govtprep.app.data.remote.ImageUploadHelper
import com.govtprep.app.ui.theme.*
import kotlinx.coroutines.launch

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ADMIN PANEL — Dashboard Console
// Exams → Sub-Exams → Subjects → Test Sets → Questions
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val AdminAccent = Color(0xFF059669) // emerald across all levels

@Composable
fun AdminScreen(state: AdminState, onBack: () -> Unit, vm: AdminViewModel) {
    val c = NeuColors
    val accent = AdminAccent

    // Intercept system back button — drill back through levels
    BackHandler {
        when {
            state.showPYQ -> vm.pyqGoBack()
            state.showFeedback -> vm.toggleFeedbackView()
            state.level != AdminLevel.EXAMS -> vm.goBack()
            else -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(c.Background)) {
        Column(Modifier.fillMaxSize()) {
            // ── TOP BAR ──
            TopBar(
                state = state,
                accent = accent,
                onBack = {
                    when {
                        state.showPYQ -> vm.pyqGoBack()
                        state.showFeedback -> vm.toggleFeedbackView()
                        state.level == AdminLevel.EXAMS -> onBack()
                        else -> vm.goBack()
                    }
                },
                onSearchChange = { vm.updateSearch(it) }
            )

            // ── CONTENT ──
            Box(Modifier.weight(1f)) {
                if (state.isLoading) {
                    LoadingSkeleton()
                } else {
                    when (state.level) {
                        AdminLevel.EXAMS -> ExamList(state, vm)
                        AdminLevel.SUB_EXAMS -> SubExamGrid(state, vm)
                        AdminLevel.SUBJECTS -> SubjectList(state, vm)
                        AdminLevel.TEST_SETS -> TestSetList(state, vm)
                        AdminLevel.QUESTIONS -> QuestionList(state, vm)
                    }
                }
            }
        }

        // ── EXTENDED FAB ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(16.dp))
                .background(accent)
                .clickable { vm.showAddDialog() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Add, "Add", tint = Color.White, modifier = Modifier.size(20.dp))
            Text(
                when (state.level) {
                    AdminLevel.EXAMS -> "Exam"
                    AdminLevel.SUB_EXAMS -> "Subject"
                    AdminLevel.SUBJECTS -> "Test"
                    AdminLevel.TEST_SETS -> "Test Set"
                    AdminLevel.QUESTIONS -> "Question"
                },
                color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
            )
        }

        // ── SNACKBAR ──
        val msg = state.snackbarMessage ?: state.error
        if (msg != null) {
            val isError = state.error != null
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
                containerColor = if (isError) c.Error else accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                action = { TextButton(onClick = { vm.clearSnackbar() }) { Text("OK", color = Color.White.copy(alpha = 0.8f)) } }
            ) { Text(msg, fontSize = 13.sp) }
            LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); vm.clearSnackbar() }
        }
    }

    // ── DIALOGS ──
    if (state.showDialog) {
        when (state.dialogType) {
            DialogType.EXAM -> ExamDialog(
                editing = state.editingItem as? Exam,
                onSave = { n, s, ic, d, a, o -> vm.saveExam(if (state.editingItem != null) (state.editingItem as Exam).id else null, n, s, ic, d, a, o) },
                onDismiss = { vm.dismissDialog() })
            DialogType.SUBJECT -> SubjectDialog(
                editing = state.editingItem as? Subject,
                defaultSubExam = state.selectedSubExam,
                onSave = { n, s, ic, se, seh, a, f, o, tq, tm, dur, neg -> vm.saveSubject(if (state.editingItem != null) (state.editingItem as Subject).id else null, n, s, ic, se, seh, a, f, o, tq, tm, dur, neg) },
                onDismiss = { vm.dismissDialog() })
            DialogType.TEST_SET -> TestSetDialog(
                editing = state.editingItem as? TestSet,
                onSave = { t, tt, tq, tm, dm, nm, a, f, d, y, ed, sh -> vm.saveTestSet(if (state.editingItem != null) (state.editingItem as TestSet).id else null, t, tt, tq, tm, dm, nm, a, f, d, y, ed, sh) },
                onDismiss = { vm.dismissDialog() })
            DialogType.QUESTION -> QuestionDialog(
                editing = state.editingItem as? AdminQuestion,
                nextNumber = (state.questions.maxOfOrNull { it.questionNumber } ?: 0) + 1,
                onSave = { qn, qt, qth, opts, cid, exp, exph, m, nm, tp, df, qImg ->
                    vm.saveQuestion(if (state.editingItem != null) (state.editingItem as AdminQuestion).id else null, qn, qt, qth, opts, cid, exp, exph, m, nm, tp, df, qImg)
                },
                generateOptionId = { vm.generateOptionId() },
                onDismiss = { vm.dismissDialog() })
            else -> {}
        }
    }

    if (state.showDeleteConfirm) {
        DeleteConfirmDialog(state.deleteItemName, { vm.confirmDelete() }, { vm.dismissDeleteConfirm() })
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TOP BAR — Level-tinted header with breadcrumb chips + search
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun TopBar(state: AdminState, accent: Color, onBack: () -> Unit, onSearchChange: (String) -> Unit) {
    val c = NeuColors
    var searchExpanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                // Accent strip at bottom
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
        // Row 1: Back + Title + Search icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = c.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Admin Console", style = NeuType.h3.copy(letterSpacing = (-0.3).sp))
                // Level badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when {
                        state.showPYQ -> Color(0xFF7C3AED)
                        state.showFeedback -> Color(0xFFF59E0B)
                        else -> accent
                    }
                    val badgeLabel = when {
                        state.showPYQ && state.pyqSelectedTestSet != null -> "PYQ Questions"
                        state.showPYQ -> "PYQ Papers"
                        state.showFeedback -> "Feedback"
                        else -> state.level.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() }
                    }
                    Box(
                        Modifier.size(6.dp).clip(CircleShape).background(badgeColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        badgeLabel,
                        style = NeuType.caption.copy(
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
            // Search toggle
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (searchExpanded) accent.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { searchExpanded = !searchExpanded; if (!searchExpanded) onSearchChange("") },
                Alignment.Center
            ) {
                Icon(
                    if (searchExpanded) Icons.Filled.Close else Icons.Outlined.Search,
                    null, tint = if (searchExpanded) accent else c.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Row 2: Breadcrumb chips
        if (!searchExpanded) {
            val crumbs = buildBreadcrumbList(state)
            if (crumbs.size > 1) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    crumbs.forEachIndexed { i, name ->
                        if (i > 0) {
                            Text("›", color = c.TextMuted, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 2.dp))
                        }
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

        // Search bar (expandable)
        AnimatedVisibility(
            visible = searchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Spacer(Modifier.height(8.dp))
            val hint = when (state.level) {
                AdminLevel.EXAMS -> "exams"; AdminLevel.SUB_EXAMS -> "sub-exams"
                AdminLevel.SUBJECTS -> "subjects"; AdminLevel.TEST_SETS -> "test sets"; AdminLevel.QUESTIONS -> "questions"
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (c.isDark) c.SurfaceDeep else c.SurfaceAlt)
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = state.searchQuery, onValueChange = onSearchChange, singleLine = true,
                    textStyle = NeuType.small.copy(color = c.TextPrimary), cursorBrush = SolidColor(accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (state.searchQuery.isEmpty()) Text("Search $hint…", style = NeuType.small, color = c.TextMuted)
                        inner()
                    }
                )
                if (state.searchQuery.isNotEmpty()) {
                    Icon(Icons.Filled.Close, null, tint = c.TextMuted,
                        modifier = Modifier.size(16.dp).clickable { onSearchChange("") })
                }
            }
        }
    }
}

private fun buildBreadcrumbList(state: AdminState): List<String> {
    val p = mutableListOf("Exams")
    state.selectedExam?.let { p.add(it.name) }
    state.selectedSubExam?.let { p.add(it) }
    state.selectedSubject?.let { p.add(it.name) }
    state.selectedTestSet?.let { p.add(it.title) }
    return p
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// LOADING
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun LoadingSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeuSkeleton(Modifier.fillMaxWidth(0.4f), height = 14.dp, cornerRadius = 7.dp)
        Spacer(Modifier.height(4.dp))
        repeat(5) {
            NeuSkeleton(Modifier.fillMaxWidth(), height = 72.dp, cornerRadius = 14.dp)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SECTION HEADER — "12 Exams • 8 Active"
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SectionCount(total: Int, activeCount: Int, label: String, accent: Color) {
    val c = NeuColors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$total $label",
            style = NeuType.label.copy(color = c.TextMuted, letterSpacing = 1.sp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.clip(RoundedCornerShape(4.dp))
                .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.1f))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text("$activeCount active", fontSize = 10.sp, color = accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ITEM CARD — Left accent border + vertical layout
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun AdminItemCard(
    icon: String?,
    fallbackIcon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showChevron: Boolean = true
) {
    val c = NeuColors

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (c.isDark) Color.Transparent else c.Divider, RoundedCornerShape(14.dp))
            .drawBehind {
                // Left accent bar
                drawRoundRect(
                    color = if (isActive) accentColor else c.Error.copy(alpha = 0.4f),
                    topLeft = Offset.Zero,
                    size = Size(4.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
            .background(c.Surface)
            .bounceClick(onClick = onClick)
    ) {
        // Main content row
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) accentColor.copy(alpha = if (c.isDark) 0.15f else 0.1f)
                        else c.SurfaceDeep
                    ),
                Alignment.Center
            ) {
                if (!icon.isNullOrBlank()) {
                    Text(icon, fontSize = 18.sp)
                } else {
                    Icon(fallbackIcon, null, tint = if (isActive) accentColor else c.TextMuted, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(12.dp))

            // Title + subtitle
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = NeuType.body.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isActive) c.TextPrimary else c.TextMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(subtitle, style = NeuType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (showChevron) {
                Icon(Icons.Filled.ChevronRight, null, tint = c.TextMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }

        // Action row
        Row(
            Modifier.fillMaxWidth().padding(start = 68.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active status chip
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isActive) c.Success.copy(alpha = if (c.isDark) 0.12f else 0.08f)
                        else c.Error.copy(alpha = if (c.isDark) 0.12f else 0.08f)
                    )
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (isActive) c.Success else c.Error.copy(0.6f)))
                    Text(
                        if (isActive) "Active" else "Inactive",
                        fontSize = 10.sp, fontWeight = FontWeight.Medium,
                        color = if (isActive) c.Success else c.Error.copy(0.7f)
                    )
                }
            }
            Spacer(Modifier.weight(1f))

            // Edit
            Box(
                Modifier
                    .size(30.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEdit),
                Alignment.Center
            ) {
                Icon(Icons.Outlined.Edit, null, tint = c.TextMuted, modifier = Modifier.size(15.dp))
            }
            // Delete
            Box(
                Modifier
                    .size(30.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDelete),
                Alignment.Center
            ) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = c.Error.copy(alpha = 0.5f), modifier = Modifier.size(15.dp))
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EXAM LIST
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ExamList(state: AdminState, vm: AdminViewModel) {
    val c = NeuColors
    val accent = AdminAccent

    when {
        state.showPYQ -> PYQManagementView(state, vm)
        state.showFeedback -> FeedbackListView(state.feedbackList, state.isLoading, vm)
        else -> {
            val f = state.exams.filter { it.name.contains(state.searchQuery, true) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Feedback banner (full span) ──
                item(span = { GridItemSpan(2) }) {
                    val newCount = state.feedbackList.count { it.status == "new" }
                    AdminBannerCard(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "User Feedback",
                        subtitle = "Review ideas & suggestions",
                        badgeText = if (newCount > 0) "$newCount new" else null,
                        onClick = { vm.toggleFeedbackView() }
                    )
                }

                // ── PYQ banner (full span) ──
                item(span = { GridItemSpan(2) }) {
                    val pyqCount = state.pyqTestSets.size
                    AdminBannerCard(
                        icon = Icons.Outlined.HistoryEdu,
                        title = "PYQ Papers",
                        subtitle = "Manage previous year questions",
                        badgeText = if (pyqCount > 0) "$pyqCount papers" else null,
                        badgeColor = Color(0xFF7C3AED),
                        onClick = { vm.togglePYQView() }
                    )
                }

                // Section count
                item(span = { GridItemSpan(2) }) {
                    SectionCount(f.size, f.count { it.isActive }, "Exams", accent)
                }

                if (f.isEmpty()) {
                    item(span = { GridItemSpan(2) }) { EmptyState("No exams found", Icons.Outlined.Inventory2) }
                } else {
                    items(f, key = { it.id }) { item ->
                        AdminExamGridCard(
                            exam = item, accent = accent,
                            onClick = { vm.selectExam(item) },
                            onToggle = { vm.toggleActive(item.id, item.isActive) },
                            onEdit = { vm.showEditDialog(item) },
                            onDelete = { vm.showDeleteConfirmation(item.id, item.name) }
                        )
                    }
                }
                items(2, span = { GridItemSpan(1) }) { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Reusable banner card for Feedback / PYQ ──
@Composable
private fun AdminBannerCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String?,
    badgeColor: Color = Color(0xFF059669),
    onClick: () -> Unit
) {
    val c = NeuColors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (c.isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4))
            .border(
                1.dp,
                if (badgeText != null) badgeColor.copy(alpha = 0.3f) else c.Divider,
                RoundedCornerShape(14.dp)
            )
            .bounceClick(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(badgeColor.copy(alpha = if (c.isDark) 0.2f else 0.12f)),
            Alignment.Center
        ) { Icon(icon, null, tint = badgeColor, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = NeuType.body.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = NeuType.caption)
        }
        if (badgeText != null) {
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(badgeColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp), Alignment.Center
            ) { Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.Filled.ChevronRight, null, tint = c.TextMuted.copy(0.5f), modifier = Modifier.size(16.dp))
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PYQ MANAGEMENT VIEW
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun PYQManagementView(state: AdminState, vm: AdminViewModel) {
    val c = NeuColors
    val accent = Color(0xFF7C3AED) // Purple accent for PYQ

    if (state.pyqSelectedTestSet != null) {
        // ── Drill into questions for a specific shift ──
        PYQQuestionsList(state, vm, accent)
    } else {
        // ── Main PYQ list grouped by Year → Date → Shift ──
        val tests = state.pyqTestSets

        if (tests.isEmpty() && !state.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.HistoryEdu, null, tint = c.TextMuted.copy(0.4f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("No PYQ Papers", style = NeuType.h3, color = c.TextMuted)
                Spacer(Modifier.height(6.dp))
                Text("Add papers via Admin → Exam → Sub-Exam → Subject → New Test Set (type: previous_year)",
                    style = NeuType.caption, color = c.TextMuted, textAlign = TextAlign.Center)
            }
        } else {
            // Group: year → date → shifts
            val byYear = tests.groupBy { it.year ?: 0 }.toSortedMap(compareByDescending { it })

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stats header
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.HistoryEdu, null, tint = accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PYQ Papers", style = NeuType.h3)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${tests.size} papers · ${byYear.size} years",
                            style = NeuType.caption.copy(color = c.TextMuted)
                        )
                    }
                }

                byYear.forEach { (year, yearTests) ->
                    // Year header
                    item(key = "year_$year") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.08f)),
                                Alignment.Center
                            ) {
                                Text("📅", fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("$year", style = NeuType.h3.copy(fontSize = 18.sp))
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${yearTests.size} papers",
                                style = NeuType.caption
                            )
                        }
                    }

                    // Group by date within year
                    val byDate = yearTests.groupBy { it.examDate ?: "Unknown" }
                        .toSortedMap(compareByDescending { it })

                    byDate.forEach { (date, dateTests) ->
                        // Date sub-header
                        item(key = "date_${year}_$date") {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    formatPYQDate(date),
                                    style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${dateTests.size} shifts",
                                    style = NeuType.caption.copy(fontSize = 11.sp)
                                )
                            }
                        }

                        // Shift cards
                        val sortedShifts = dateTests.sortedBy { it.shift ?: 0 }
                        items(sortedShifts, key = { it.id }) { test ->
                            PYQShiftCard(
                                test = test,
                                accent = accent,
                                onClick = { vm.selectPYQTestSet(test) },
                                onEdit = { vm.editPYQTestSet(test) },
                                onDelete = { vm.deletePYQTestSet(test.id, test.title) }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun PYQShiftCard(
    test: TestSet,
    accent: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val c = NeuColors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.Surface)
            .border(1.dp, if (c.isDark) accent.copy(alpha = 0.15f) else c.Divider, RoundedCornerShape(12.dp))
            .bounceClick(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shift badge
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (c.isDark) 0.12f else 0.06f)),
            Alignment.Center
        ) {
            Text("S${test.shift ?: "?"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "Shift ${test.shift ?: "?"}",
                style = NeuType.body.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp)
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${test.totalQuestions}Q", style = NeuType.caption.copy(fontSize = 10.sp))
                Text("·", style = NeuType.caption.copy(fontSize = 10.sp, color = c.TextMuted))
                Text("${test.durationMinutes}min", style = NeuType.caption.copy(fontSize = 10.sp))
                Text("·", style = NeuType.caption.copy(fontSize = 10.sp, color = c.TextMuted))
                Text("${test.totalMarks.toInt()}m", style = NeuType.caption.copy(fontSize = 10.sp))
            }
        }

        // Edit + Delete
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onEdit), Alignment.Center) {
            Icon(Icons.Outlined.Edit, null, tint = c.TextMuted, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onDelete), Alignment.Center) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = c.Error.copy(0.5f), modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Outlined.ChevronRight, null, tint = c.TextMuted.copy(0.4f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PYQQuestionsList(state: AdminState, vm: AdminViewModel, accent: Color) {
    val c = NeuColors
    val test = state.pyqSelectedTestSet ?: return
    val qs = state.pyqQuestions

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(test.title, style = NeuType.h3)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shift ${test.shift ?: "?"}", style = NeuType.caption.copy(color = accent))
                    Text("·", style = NeuType.caption)
                    Text("${qs.size} questions loaded", style = NeuType.caption)
                }
            }
        }

        if (qs.isEmpty() && !state.isLoading) {
            item { EmptyState("No questions yet", Icons.Outlined.Quiz) }
        }

        itemsIndexed(qs, key = { _, q -> q.id }) { idx, q ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.Surface)
                    .border(1.dp, if (c.isDark) Color.Transparent else c.Divider, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Number badge
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = if (c.isDark) 0.12f else 0.06f)),
                    Alignment.Center
                ) {
                    Text("${q.questionNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    val qText = q.questionText.ifBlank { q.questionTextHindi ?: "" }
                    Text(
                        qText.take(120) + if (qText.length > 120) "..." else "",
                        style = NeuType.small.copy(fontSize = 12.sp),
                        maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                    // Show correct answer
                    val opts = q.options
                    val correctOpt = opts.find { it.id == q.correctOptionId }
                    if (correctOpt != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "✓ ${correctOpt.text.take(40)}",
                            style = NeuType.caption.copy(fontSize = 10.sp, color = c.Success),
                            maxLines = 1
                        )
                    }
                }
                // Edit button
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).clickable { vm.showEditDialog(q) }, Alignment.Center) {
                    Icon(Icons.Outlined.Edit, null, tint = c.TextMuted, modifier = Modifier.size(13.dp))
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

private fun formatPYQDate(dateStr: String): String {
    if (dateStr == "Unknown" || dateStr.isBlank()) return dateStr
    return try {
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        val day = parts[2].toIntOrNull() ?: return dateStr
        val monthIdx = (parts[1].toIntOrNull() ?: return dateStr) - 1
        val year = parts[0]
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "$day ${months.getOrElse(monthIdx) { "?" }} $year"
    } catch (_: Exception) { dateStr }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// FEEDBACK VIEW
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun FeedbackListView(feedbackList: List<Feedback>, isLoading: Boolean, vm: AdminViewModel) {
    val c = NeuColors

    Column(Modifier.fillMaxSize()) {
        // Back row
        Row(
            Modifier.fillMaxWidth().clickable { vm.toggleFeedbackView() }.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ArrowBack, null, tint = c.TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text("Feedback", style = NeuType.h3)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(c.SurfaceAlt)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("${feedbackList.size} total", style = NeuType.caption)
            }
        }

        if (feedbackList.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                EmptyState("No feedback yet", Icons.Outlined.ChatBubbleOutline)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(feedbackList, key = { it.id }) { fb ->
                    val statusColor = when (fb.status) {
                        "new" -> Color(0xFF059669); "read" -> Color(0xFFF59E0B); else -> c.TextMuted
                    }
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, if (c.isDark) Color.Transparent else c.Divider, RoundedCornerShape(14.dp))
                            .drawBehind {
                                drawRoundRect(
                                    color = statusColor,
                                    topLeft = Offset.Zero,
                                    size = Size(3.dp.toPx(), size.height),
                                    cornerRadius = CornerRadius(3.dp.toPx())
                                )
                            }
                            .background(c.Surface)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar circle
                            Box(
                                Modifier.size(32.dp).clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.15f)),
                                Alignment.Center
                            ) {
                                Text(
                                    (fb.userName ?: "A").take(1).uppercase(),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusColor
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(fb.userName ?: "Anonymous", style = NeuType.body.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp))
                                Text(fb.userEmail ?: "", style = NeuType.caption)
                            }
                            // Status badge
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(fb.status, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    color = statusColor, letterSpacing = 0.5.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(fb.message, style = NeuType.body.copy(fontSize = 14.sp, lineHeight = 20.sp), color = c.TextPrimary)
                        Spacer(Modifier.height(10.dp))
                        // Date + actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fb.createdAt?.take(10) ?: "", style = NeuType.caption.copy(fontSize = 10.sp))
                            Spacer(Modifier.weight(1f))
                            if (fb.status == "new") {
                                ActionChip("Mark Read", Color(0xFF059669), c) { vm.markFeedback(fb.id, "read") }
                                Spacer(Modifier.width(6.dp))
                            }
                            if (fb.status != "resolved") {
                                ActionChip("Resolve", c.TextSecondary, c) { vm.markFeedback(fb.id, "resolved") }
                                Spacer(Modifier.width(6.dp))
                            }
                            Box(
                                Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                                    .clickable { vm.deleteFeedbackItem(fb.id) },
                                Alignment.Center
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, null, tint = c.Error.copy(0.5f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ActionChip(text: String, color: Color, c: NeuColorPalette, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = if (c.isDark) 0.12f else 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUB-EXAM GRID — 2-column cards
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SubExamGrid(state: AdminState, vm: AdminViewModel) {
    val c = NeuColors
    val accent = AdminAccent
    val f = state.subExamGroups.filter { it.name.contains(state.searchQuery, true) }

    if (f.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { EmptyState("No sub-exams", Icons.Outlined.Category) }
    } else {
        Column {
            SectionCount(f.size, f.count { it.isActive }, "Sub-Exams", accent)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(f, key = { it.name }) { g ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.Surface)
                            .border(
                                1.dp,
                                if (g.isActive) accent.copy(alpha = 0.2f) else c.Divider.copy(0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .bounceClick { vm.selectSubExam(g.name) }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Emoji hero
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.08f)),
                            Alignment.Center
                        ) {
                            Text(g.icon ?: "📚", fontSize = 22.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            g.name,
                            style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = if (g.isActive) c.TextPrimary else c.TextMuted,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (g.isActive) c.Success else c.Error.copy(0.5f)))
                            Text(
                                "${g.subjectCount} subjects",
                                style = NeuType.caption.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }
                // Bottom spacer items
                items(2) { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUBJECT LIST — with full mock section
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SubjectList(state: AdminState, vm: AdminViewModel) {
    val c = NeuColors
    val accent = AdminAccent
    val filteredSubjects = state.subjects.filter { it.name.contains(state.searchQuery, true) }
    val filteredMocks = state.fullMockTests.filter { it.title.contains(state.searchQuery, true) }
    val isEmpty = filteredSubjects.isEmpty() && filteredMocks.isEmpty()

    if (isEmpty) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { EmptyState("No subjects", Icons.Outlined.MenuBook) }
    } else {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SectionCount(filteredSubjects.size, filteredSubjects.count { it.isActive }, "Subjects", accent)
            }
            items(filteredSubjects, key = { it.id }) { subject ->
                AdminItemCard(
                    icon = subject.icon,
                    fallbackIcon = Icons.Outlined.MenuBook,
                    title = subject.name,
                    subtitle = "Order ${subject.displayOrder}",
                    isActive = subject.isActive,
                    accentColor = accent,
                    onClick = { vm.selectSubject(subject) },
                    onToggle = { vm.toggleActive(subject.id, subject.isActive) },
                    onEdit = { vm.showEditDialog(subject) },
                    onDelete = { vm.showDeleteConfirmation(subject.id, subject.name) }
                )
            }

            // Full Mock Tests section
            if (filteredMocks.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f).height(1.dp).background(c.Divider))
                        Text(
                            "  FULL MOCKS  ",
                            style = NeuType.label.copy(fontSize = 10.sp, color = Color(0xFF059669))
                        )
                        Box(Modifier.weight(1f).height(1.dp).background(c.Divider))
                    }
                }
                items(filteredMocks, key = { "mock_${it.id}" }) { mock ->
                    AdminItemCard(
                        icon = null,
                        fallbackIcon = Icons.Outlined.Assignment,
                        title = mock.title,
                        subtitle = "${mock.totalQuestions}Q  •  ${mock.durationMinutes}min  •  ${mock.totalMarks} marks",
                        isActive = mock.isActive,
                        accentColor = Color(0xFF059669),
                        onClick = { vm.selectTestSet(mock) },
                        onToggle = { vm.toggleMockActive(mock.id, mock.isActive) },
                        onEdit = { vm.showEditDialog(mock) },
                        onDelete = { vm.showDeleteMockConfirmation(mock.id, mock.title) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST SET LIST
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun TestSetList(state: AdminState, vm: AdminViewModel) {
    val accent = AdminAccent
    val f = state.testSets.filter { it.title.contains(state.searchQuery, true) }

    if (f.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { EmptyState("No test sets", Icons.Outlined.Quiz) }
    } else {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SectionCount(f.size, f.count { it.isActive }, "Test Sets", accent)
            }
            items(f, key = { it.id }) { ts ->
                AdminItemCard(
                    icon = null,
                    fallbackIcon = Icons.Outlined.Description,
                    title = ts.title,
                    subtitle = "${ts.testType}  •  ${ts.totalQuestions}Q  •  ${ts.durationMinutes}min  •  ${ts.totalMarks}marks",
                    isActive = ts.isActive,
                    accentColor = accent,
                    onClick = { vm.selectTestSet(ts) },
                    onToggle = { vm.toggleActive(ts.id, ts.isActive) },
                    onEdit = { vm.showEditDialog(ts) },
                    onDelete = { vm.showDeleteConfirmation(ts.id, ts.title) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// QUESTION LIST — Accordion cards
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun QuestionList(state: AdminState, vm: AdminViewModel) {
    val c = NeuColors
    val accent = AdminAccent
    val f = state.questions.filter {
        it.questionText.contains(state.searchQuery, true) || it.topic?.contains(state.searchQuery, true) == true
    }

    if (f.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { EmptyState("No questions", Icons.Outlined.HelpOutline) }
    } else {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SectionCount(f.size, f.size, "Questions", accent)
            }
            items(f, key = { it.id }) { q ->
                var expanded by remember { mutableStateOf(false) }
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, if (c.isDark) Color.Transparent else c.Divider, RoundedCornerShape(14.dp))
                        .background(c.Surface)
                        .clickable { expanded = !expanded }
                ) {
                    // Header row
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Number badge
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = if (c.isDark) 0.15f else 0.1f)),
                            Alignment.Center
                        ) {
                            Text(
                                "${q.questionNumber}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            q.questionText,
                            style = NeuType.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                            modifier = Modifier.weight(1f),
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        // Actions
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable { vm.showEditDialog(q) }, Alignment.Center) {
                                Icon(Icons.Outlined.Edit, null, tint = c.TextMuted, modifier = Modifier.size(14.dp))
                            }
                            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable { vm.showDeleteConfirmation(q.id, "Q${q.questionNumber}") }, Alignment.Center) {
                                Icon(Icons.Outlined.DeleteOutline, null, tint = c.Error.copy(0.5f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Expanded: options + meta
                    AnimatedVisibility(visible = expanded) {
                        Column(Modifier.padding(start = 56.dp, end = 14.dp, bottom = 14.dp)) {
                            q.options.forEach { opt ->
                                val isCorrect = opt.id == q.correctOptionId
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isCorrect) c.Success.copy(alpha = if (c.isDark) 0.1f else 0.06f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                                            .background(if (isCorrect) c.Success.copy(0.2f) else c.SurfaceDeep),
                                        Alignment.Center
                                    ) {
                                        Text(
                                            opt.label ?: "", fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCorrect) c.Success else c.TextMuted
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        opt.text,
                                        style = NeuType.small,
                                        color = if (isCorrect) c.Success else c.TextSecondary,
                                        fontWeight = if (isCorrect) FontWeight.Medium else FontWeight.Normal,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis
                                    )
                                    if (isCorrect) {
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Filled.CheckCircle, null, tint = c.Success, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            // Tags
                            if (q.topic != null || q.difficulty != null) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    q.topic?.let { Tag(it, accent, c) }
                                    q.difficulty?.let { Tag(it, accent, c) }
                                }
                            }
                        }
                    }

                    // Collapse indicator
                    Box(
                        Modifier.fillMaxWidth().height(2.dp)
                            .background(if (expanded) accent.copy(alpha = 0.3f) else Color.Transparent)
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun AdminExamGridCard(
    exam: Exam,
    accent: Color,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val c = NeuColors

    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.Surface)
            .border(
                1.dp,
                if (c.isDark) {
                    if (exam.isActive) accent.copy(alpha = 0.2f) else Color.Transparent
                } else c.Divider,
                RoundedCornerShape(16.dp)
            )
            .bounceClick(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (exam.isActive) accent.copy(alpha = if (c.isDark) 0.15f else 0.08f)
                    else c.SurfaceDeep
                ),
            Alignment.Center
        ) {
            val ic = exam.icon
            if (!ic.isNullOrBlank()) Text(ic, fontSize = 22.sp)
            else Icon(Icons.Outlined.Description, null, tint = if (exam.isActive) accent else c.TextMuted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))

        // Name
        Text(
            exam.name,
            style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = if (exam.isActive) c.TextPrimary else c.TextMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Slug
        Text(
            "/${exam.slug}",
            style = NeuType.caption.copy(fontSize = 10.sp),
            color = c.TextMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Action row: status + edit + delete
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active chip
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (exam.isActive) c.Success.copy(alpha = if (c.isDark) 0.12f else 0.08f)
                        else c.Error.copy(alpha = if (c.isDark) 0.12f else 0.08f)
                    )
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (exam.isActive) c.Success else c.Error.copy(0.6f)))
                    Text(
                        if (exam.isActive) "On" else "Off",
                        fontSize = 9.sp, fontWeight = FontWeight.Medium,
                        color = if (exam.isActive) c.Success else c.Error.copy(0.7f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onEdit), Alignment.Center) {
                Icon(Icons.Outlined.Edit, null, tint = c.TextMuted, modifier = Modifier.size(13.dp))
            }
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onDelete), Alignment.Center) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = c.Error.copy(0.5f), modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(text: String, icon: ImageVector) {
    val c = NeuColors
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                .background(c.SurfaceAlt),
            Alignment.Center
        ) {
            Icon(icon, null, tint = c.TextMuted.copy(0.5f), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(text, style = NeuType.bodySecondary)
    }
}

@Composable
private fun Tag(text: String, accent: Color, c: NeuColorPalette) {
    Text(
        text, fontSize = 10.sp, color = accent, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(accent.copy(alpha = if (c.isDark) 0.12f else 0.08f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// DIALOGS — Grouped sections, cleaner layout
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ExamDialog(editing: Exam?, onSave: (String, String, String?, String?, Boolean, Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var slug by remember { mutableStateOf(editing?.slug ?: "") }
    var icon by remember { mutableStateOf(editing?.icon ?: "") }
    var desc by remember { mutableStateOf(editing?.description ?: "") }
    var active by remember { mutableStateOf(editing?.isActive ?: true) }
    var order by remember { mutableStateOf(editing?.displayOrder?.toString() ?: "0") }

    FormDialog(if (editing != null) "Edit Exam" else "New Exam",
        { onSave(name, slug.ifBlank { name.lowercase().replace(" ", "-") }, icon.ifBlank { null }, desc.ifBlank { null }, active, order.toIntOrNull() ?: 0) },
        onDismiss, name.isNotBlank()) {
        Field("Name *", name) { name = it }
        Field("Slug", slug) { slug = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Field("Icon (emoji)", icon) { icon = it } }
            Box(Modifier.weight(1f)) { NumField("Order", order) { order = it } }
        }
        Field("Description", desc, 2) { desc = it }
        Spacer(Modifier.height(4.dp))
        Toggle("Active", active) { active = it }
    }
}

@Composable
private fun SubjectDialog(editing: Subject?, defaultSubExam: String?, onSave: (String, String?, String?, String?, String?, Boolean, Boolean, Int, Int, Double, Int, Double) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var slug by remember { mutableStateOf(editing?.slug ?: "") }
    var icon by remember { mutableStateOf(editing?.icon ?: "") }
    var subExam by remember { mutableStateOf(editing?.subExam ?: defaultSubExam ?: "") }
    var subExamH by remember { mutableStateOf(editing?.subExamHindi ?: "") }
    var active by remember { mutableStateOf(editing?.isActive ?: true) }
    var free by remember { mutableStateOf(editing?.isFree ?: true) }
    var order by remember { mutableStateOf(editing?.displayOrder?.toString() ?: "0") }
    var tq by remember { mutableStateOf(editing?.totalQuestions?.let { if (it > 0) it.toString() else "25" } ?: "25") }
    var tm by remember { mutableStateOf(editing?.totalMarks?.let { if (it > 0) it.toString() else "50" } ?: "50") }
    var dur by remember { mutableStateOf(editing?.durationMinutes?.let { if (it > 0) it.toString() else "30" } ?: "30") }
    var neg by remember { mutableStateOf(editing?.negativeMarking?.let { if (it > 0) it.toString() else "0.5" } ?: "0.5") }

    FormDialog(if (editing != null) "Edit Subject" else "New Subject",
        { onSave(name, slug.ifBlank { null }, icon.ifBlank { null }, subExam.ifBlank { null }, subExamH.ifBlank { null }, active, free, order.toIntOrNull() ?: 0, tq.toIntOrNull() ?: 25, tm.toDoubleOrNull() ?: 50.0, dur.toIntOrNull() ?: 30, neg.toDoubleOrNull() ?: 0.5) },
        onDismiss, name.isNotBlank()) {
        Field("Name *", name) { name = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Field("Slug", slug) { slug = it } }
            Box(Modifier.weight(1f)) { Field("Icon", icon) { icon = it } }
        }
        Field("Sub-Exam Group *", subExam) { subExam = it }
        Field("Sub-Exam Hindi", subExamH) { subExamH = it }
        NumField("Display Order", order) { order = it }
        SectionLabel("CONFIGURATION")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { NumField("Questions", tq) { tq = it } }
            Box(Modifier.weight(1f)) { NumField("Total Marks", tm) { tm = it } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { NumField("Duration (min)", dur) { dur = it } }
            Box(Modifier.weight(1f)) { NumField("Neg. Marks", neg) { neg = it } }
        }
        SectionLabel("STATUS")
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Toggle("Active", active) { active = it }
            Toggle("Free", free) { free = it }
        }
    }
}

@Composable
private fun TestSetDialog(editing: TestSet?, onSave: (String, String, Int, Double, Int, Double, Boolean, Boolean, String?, Int?, String?, Int?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var type by remember { mutableStateOf(editing?.testType ?: "subject") }
    var tq by remember { mutableStateOf(editing?.totalQuestions?.toString() ?: "25") }
    var tm by remember { mutableStateOf(editing?.totalMarks?.toString() ?: "50") }
    var dur by remember { mutableStateOf(editing?.durationMinutes?.toString() ?: "30") }
    var neg by remember { mutableStateOf(editing?.negativeMarking?.toString() ?: "0.5") }
    var active by remember { mutableStateOf(editing?.isActive ?: true) }
    var free by remember { mutableStateOf(editing?.isFree ?: true) }
    var diff by remember { mutableStateOf(editing?.difficulty ?: "") }
    var year by remember { mutableStateOf(editing?.year?.toString() ?: "") }
    var examDate by remember { mutableStateOf(editing?.examDate ?: "") }
    var shift by remember { mutableStateOf(editing?.shift?.toString() ?: "") }

    FormDialog(if (editing != null) "Edit Test Set" else "New Test Set",
        { onSave(title, type, tq.toIntOrNull() ?: 25, tm.toDoubleOrNull() ?: 50.0, dur.toIntOrNull() ?: 30, neg.toDoubleOrNull() ?: 0.5, active, free, diff.ifBlank { null }, year.toIntOrNull(), examDate.ifBlank { null }, shift.toIntOrNull()) },
        onDismiss, title.isNotBlank()) {
        SectionLabel("BASIC")
        Field("Title *", title) { title = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Drop("Type", type, listOf("subject", "full_mock", "chapter", "quick", "previous_year")) { type = it } }
            Box(Modifier.weight(1f)) { Drop("Difficulty", diff.ifBlank { "medium" }, listOf("easy", "medium", "hard")) { diff = it } }
        }
        AnimatedVisibility(visible = type == "previous_year") {
            Column {
                SectionLabel("PYQ DETAILS")
                NumField("Year (e.g. 2024)", year) { year = it }
                Field("Exam Date (e.g. 2024-02-04)", examDate) { examDate = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Drop("Shift", shift.ifBlank { "1" }, listOf("1", "2", "3")) { shift = it } }
                    Box(Modifier.weight(1f)) {} // spacer column
                }
            }
        }
        SectionLabel("CONFIGURATION")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { NumField("Questions", tq) { tq = it } }
            Box(Modifier.weight(1f)) { NumField("Total Marks", tm) { tm = it } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { NumField("Duration (min)", dur) { dur = it } }
            Box(Modifier.weight(1f)) { NumField("Neg. Marks", neg) { neg = it } }
        }
        SectionLabel("STATUS")
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Toggle("Active", active) { active = it }
            Toggle("Free", free) { free = it }
        }
    }
}

@Composable
private fun QuestionDialog(editing: AdminQuestion?, nextNumber: Int, onSave: (Int, String, String?, List<QuestionOption>, String, String?, String?, Double, Double, String?, String?, String?) -> Unit, generateOptionId: () -> String, onDismiss: () -> Unit) {
    var qNum by remember { mutableStateOf(editing?.questionNumber?.toString() ?: nextNumber.toString()) }
    var qText by remember { mutableStateOf(editing?.questionText ?: "") }
    var qTextH by remember { mutableStateOf(editing?.questionTextHindi ?: "") }
    var qImageUrl by remember { mutableStateOf(editing?.questionImageUrl ?: "") }
    var correctId by remember { mutableStateOf(editing?.correctOptionId ?: "") }
    var explanation by remember { mutableStateOf(editing?.explanation ?: "") }
    var expH by remember { mutableStateOf(editing?.explanationHindi ?: "") }
    var marks by remember { mutableStateOf(editing?.marks?.toString() ?: "2") }
    var negM by remember { mutableStateOf(editing?.negativeMarks?.toString() ?: "0.5") }
    var topic by remember { mutableStateOf(editing?.topic ?: "") }
    var diff by remember { mutableStateOf(editing?.difficulty ?: "") }
    val labels = listOf("A", "B", "C", "D")
    var options by remember { mutableStateOf(editing?.options?.toMutableList() ?: labels.map { QuestionOption(id = generateOptionId(), text = "", label = it) }) }
    LaunchedEffect(options) { if (correctId.isBlank() && options.isNotEmpty()) correctId = options.first().id }

    // Upload state
    var uploading by remember { mutableStateOf(false) }
    var uploadTarget by remember { mutableStateOf("") } // "question" or "option_0", "option_1" etc.
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlTarget by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploading = true
            scope.launch {
                val folder = if (uploadTarget == "question") "questions" else "options"
                val url = ImageUploadHelper.uploadImage(context, uri, folder)
                if (url != null) {
                    when {
                        uploadTarget == "question" -> qImageUrl = url
                        uploadTarget.startsWith("option_") -> {
                            val idx = uploadTarget.removePrefix("option_").toIntOrNull() ?: return@launch
                            options = options.toMutableList().also { it[idx] = it[idx].copy(imageUrl = url) }
                        }
                    }
                }
                uploading = false
            }
        }
    }

    val c = NeuColors
    val accent = AdminAccent

    // URL input dialog
    if (showUrlDialog) {
        var urlInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showUrlDialog = false }) {
            Column(
                Modifier.clip(RoundedCornerShape(16.dp)).background(c.Surface).padding(20.dp)
            ) {
                Text("Paste Image URL", style = NeuType.h3)
                Spacer(Modifier.height(12.dp))
                Field("https://...", urlInput) { urlInput = it }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).clickable { showUrlDialog = false }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Cancel", color = c.TextMuted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .clickable {
                                if (urlInput.isNotBlank()) {
                                    when {
                                        urlTarget == "question" -> qImageUrl = urlInput
                                        urlTarget.startsWith("option_") -> {
                                            val idx = urlTarget.removePrefix("option_").toIntOrNull() ?: return@clickable
                                            options = options.toMutableList().also { it[idx] = it[idx].copy(imageUrl = urlInput) }
                                        }
                                    }
                                }
                                showUrlDialog = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Add", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)).background(c.Surface)
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    Alignment.Center
                ) {
                    Text("Q", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                }
                Spacer(Modifier.width(12.dp))
                Text(if (editing != null) "Edit Question" else "New Question", style = NeuType.h3)
                if (uploading) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accent)
                }
            }
            Spacer(Modifier.height(16.dp))

            SectionLabel("QUESTION")
            NumField("Question #", qNum) { qNum = it }
            Spacer(Modifier.height(6.dp))
            Field("Question Text *", qText, 3) { qText = it }
            Spacer(Modifier.height(6.dp))
            Field("Question Hindi", qTextH) { qTextH = it }

            // Question image
            Spacer(Modifier.height(8.dp))
            ImageAttachRow(
                label = "Question Image",
                imageUrl = qImageUrl,
                onGallery = { uploadTarget = "question"; galleryLauncher.launch("image/*") },
                onUrl = { urlTarget = "question"; showUrlDialog = true },
                onRemove = { qImageUrl = "" }
            )

            Spacer(Modifier.height(12.dp))
            SectionLabel("OPTIONS")
            Spacer(Modifier.height(4.dp))
            options.forEachIndexed { idx, opt ->
                val isCorrect = correctId == opt.id
                Column(
                    Modifier.fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isCorrect) c.Success.copy(alpha = if (c.isDark) 0.08f else 0.05f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isCorrect, onClick = { correctId = opt.id },
                            colors = RadioButtonDefaults.colors(selectedColor = c.Success, unselectedColor = c.TextMuted),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            opt.label ?: labels.getOrElse(idx) { "" },
                            fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = if (isCorrect) c.Success else c.TextSecondary
                        )
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(
                            value = opt.text,
                            onValueChange = { t -> options = options.toMutableList().also { it[idx] = opt.copy(text = t) } },
                            singleLine = true, textStyle = NeuType.small.copy(color = c.TextPrimary),
                            cursorBrush = SolidColor(c.Accent),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(c.SurfaceDeep).padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (opt.text.isEmpty()) Text("Option ${opt.label}", style = NeuType.small, color = c.TextMuted)
                                inner()
                            }
                        )
                    }
                    // Option image attach
                    ImageAttachRow(
                        label = null,
                        imageUrl = opt.imageUrl ?: "",
                        onGallery = { uploadTarget = "option_$idx"; galleryLauncher.launch("image/*") },
                        onUrl = { urlTarget = "option_$idx"; showUrlDialog = true },
                        onRemove = { options = options.toMutableList().also { it[idx] = opt.copy(imageUrl = null) } },
                        compact = true
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("EXPLANATION")
            Field("Explanation", explanation, 2) { explanation = it }
            Spacer(Modifier.height(6.dp))
            Field("Explanation Hindi", expH) { expH = it }

            Spacer(Modifier.height(12.dp))
            SectionLabel("SCORING & META")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { NumField("Marks", marks) { marks = it } }
                Box(Modifier.weight(1f)) { NumField("Neg Marks", negM) { negM = it } }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { Field("Topic", topic) { topic = it } }
                Box(Modifier.weight(1f)) { Drop("Difficulty", diff.ifBlank { "medium" }, listOf("easy", "medium", "hard")) { diff = it } }
            }

            Spacer(Modifier.height(20.dp))
            NeuDivider()
            Spacer(Modifier.height(16.dp))
            val canSaveQ = (qText.isNotBlank() || qImageUrl.isNotBlank()) && options.any { it.text.isNotBlank() || !it.imageUrl.isNullOrBlank() }
            DialogButtons(
                canSave = canSaveQ && !uploading,
                onDismiss = onDismiss,
                onSave = {
                    onSave(qNum.toIntOrNull() ?: nextNumber, qText, qTextH.ifBlank { null },
                        options, correctId, explanation.ifBlank { null }, expH.ifBlank { null },
                        marks.toDoubleOrNull() ?: 2.0, negM.toDoubleOrNull() ?: 0.5,
                        topic.ifBlank { null }, diff.ifBlank { null }, qImageUrl.ifBlank { null })
                }
            )
        }
    }
}

// ── Image attach row — gallery + URL + preview + remove ──
@Composable
private fun ImageAttachRow(
    label: String?,
    imageUrl: String,
    onGallery: () -> Unit,
    onUrl: () -> Unit,
    onRemove: () -> Unit,
    compact: Boolean = false
) {
    val c = NeuColors
    val accent = AdminAccent

    if (imageUrl.isNotBlank()) {
        // Show preview + remove
        Row(
            Modifier.fillMaxWidth().padding(start = if (compact) 36.dp else 0.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(if (compact) 48.dp else 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.SurfaceDeep),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
            Text(
                imageUrl.takeLast(30),
                style = NeuType.caption.copy(fontSize = 9.sp, color = c.TextMuted),
                maxLines = 1, modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                    .background(c.Error.copy(alpha = 0.1f))
                    .clickable(onClick = onRemove),
                Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, null, tint = c.Error, modifier = Modifier.size(12.dp))
            }
        }
    } else {
        // Show attach buttons
        Row(
            Modifier.fillMaxWidth().padding(start = if (compact) 36.dp else 0.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label != null) {
                Text(label, style = NeuType.caption.copy(fontSize = 10.sp, color = c.TextMuted))
                Spacer(Modifier.width(8.dp))
            }
            // Gallery button
            Row(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = if (c.isDark) 0.1f else 0.06f))
                    .clickable(onClick = onGallery)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Image, null, tint = accent, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Gallery", fontSize = 10.sp, color = accent, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(6.dp))
            // URL button
            Row(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(c.SurfaceDeep)
                    .clickable(onClick = onUrl)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Link, null, tint = c.TextMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("URL", fontSize = 10.sp, color = c.TextMuted, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// FORM HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun FormDialog(title: String, onSave: () -> Unit, onDismiss: () -> Unit, canSave: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val c = NeuColors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)).background(c.Surface)
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Text(title, style = NeuType.h3)
            Spacer(Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(20.dp))
            NeuDivider()
            Spacer(Modifier.height(16.dp))
            DialogButtons(canSave, onDismiss, onSave)
        }
    }
}

@Composable
private fun DialogButtons(canSave: Boolean, onDismiss: () -> Unit, onSave: () -> Unit) {
    val c = NeuColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(c.SurfaceAlt)
                .clickable { onDismiss() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Cancel", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = c.TextSecondary)
        }
        Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(
                    if (canSave) c.Accent.copy(alpha = if (c.isDark) 0.15f else 0.1f)
                    else c.SurfaceAlt
                )
                .then(
                    if (canSave) Modifier.border(1.dp, c.Accent.copy(0.4f), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .clickable(enabled = canSave) { onSave() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Save", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                color = if (canSave) c.Accent else c.TextMuted
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = NeuColors
    Text(
        text,
        style = NeuType.label.copy(fontSize = 10.sp, letterSpacing = 1.2.sp, color = c.TextMuted),
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun Field(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    val c = NeuColors
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = NeuType.caption.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = minLines == 1, minLines = minLines,
            textStyle = NeuType.small.copy(color = c.TextPrimary), cursorBrush = SolidColor(c.Accent),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(c.SurfaceDeep).padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    val c = NeuColors
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = NeuType.caption.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = NeuType.small.copy(color = c.TextPrimary), cursorBrush = SolidColor(c.Accent),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(c.SurfaceDeep).padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    NeuToggle(checked = value, onCheckedChange = onChange, label = label, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun Drop(label: String, value: String, opts: List<String>, onChange: (String) -> Unit) {
    val c = NeuColors; var exp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = NeuType.caption.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.SurfaceDeep)
                    .clickable { exp = true }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, style = NeuType.small, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, null, tint = c.TextMuted, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                opts.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); exp = false }) }
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = NeuColors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Delete?", style = NeuType.h3) },
        text = { Text("Delete \"$name\"? This cannot be undone.", style = NeuType.body) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = c.Error), shape = RoundedCornerShape(10.dp)) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.TextMuted) } }
    )
}
