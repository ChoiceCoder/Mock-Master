package com.govtprep.app.ui.screens.test

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.data.model.Question
import com.govtprep.app.data.model.QuestionOption
import com.govtprep.app.data.model.ReviewQuestion
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TEST SCREEN — Complete exam flow
// Instructions → Active → Submit → Results → Review
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun TestScreen(
    state: TestState,
    onStart: () -> Unit,
    onSelectOption: (questionId: String, optionId: String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onGoTo: (index: Int) -> Unit,
    onToggleMark: (questionId: String) -> Unit,
    onTogglePalette: () -> Unit,
    onShowSubmit: () -> Unit,
    onHideSubmit: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    answeredCount: Int,
    unansweredCount: Int,
    markedCount: Int,
    getStatus: (String) -> QuestionStatus
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuColors.Background)
            .statusBarsPadding()
    ) {
        when (state.phase) {
            TestPhase.LOADING -> LoadingPhase()
            TestPhase.INSTRUCTIONS -> InstructionsPhase(state, onStart, onBack)
            TestPhase.ACTIVE -> ActivePhase(
                state, onSelectOption, onNext, onPrev, onGoTo,
                onToggleMark, onTogglePalette, onShowSubmit, onCancel,
                answeredCount, unansweredCount, markedCount, getStatus
            )
            TestPhase.SUBMITTING -> SubmittingPhase()
            TestPhase.RESULTS -> ResultsPhase(state, onBack, onGoTo)
        }

        // Submit confirmation dialog
        if (state.showSubmitDialog) {
            SubmitDialog(
                answeredCount = answeredCount,
                unansweredCount = unansweredCount,
                markedCount = markedCount,
                totalQuestions = state.questions.size,
                onConfirm = onSubmit,
                onDismiss = onHideSubmit
            )
        }
    }
}

// ━━━━ LOADING ━━━━

@Composable
private fun LoadingPhase() {
    // Skeleton shaped like the test instruction card
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        // Header
        NeuSkeleton(Modifier.fillMaxWidth(0.55f), height = 24.dp, cornerRadius = 12.dp)
        NeuSkeleton(Modifier.fillMaxWidth(0.80f), height = 14.dp, cornerRadius = 8.dp)
        Spacer(Modifier.height(8.dp))
        // Info card
        NeuSkeleton(Modifier.fillMaxWidth(), height = 110.dp, cornerRadius = 20.dp)
        // Stat chips
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeuSkeleton(Modifier.weight(1f), height = 64.dp, cornerRadius = 16.dp)
            NeuSkeleton(Modifier.weight(1f), height = 64.dp, cornerRadius = 16.dp)
            NeuSkeleton(Modifier.weight(1f), height = 64.dp, cornerRadius = 16.dp)
        }
        Spacer(Modifier.height(4.dp))
        // Instructions lines
        repeat(4) { NeuSkeleton(Modifier.fillMaxWidth(), height = 14.dp, cornerRadius = 7.dp) }
        Spacer(Modifier.weight(1f))
        NeuSkeleton(Modifier.fillMaxWidth(), height = 52.dp, cornerRadius = 16.dp)
    }
}

// ━━━━ INSTRUCTIONS ━━━━

@Composable
private fun InstructionsPhase(state: TestState, onStart: () -> Unit, onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        NeuIconButton(icon = Icons.Outlined.ArrowBack, onClick = onBack, size = 44.dp, iconSize = 20.dp)

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(visible = visible, enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { -24 }) {
            Column {
                Text(state.testSet?.title ?: "Test", style = NeuType.displayLarge)
                Spacer(Modifier.height(24.dp))

                // Instructions card
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        InstructionRow(S.questions, "${state.questions.size}")
                        Spacer(Modifier.height(14.dp))
                        NeuDivider()
                        Spacer(Modifier.height(14.dp))
                        InstructionRow(S.duration, "${state.testSet?.durationMinutes ?: 30} ${S.minutes}")
                        Spacer(Modifier.height(14.dp))
                        NeuDivider()
                        Spacer(Modifier.height(14.dp))
                        InstructionRow(S.totalMarks, "${state.testSet?.totalMarks?.toInt() ?: 0}")
                        Spacer(Modifier.height(14.dp))
                        NeuDivider()
                        Spacer(Modifier.height(14.dp))
                        InstructionRow(S.correctAnswer, "+${state.testSet?.totalMarks?.div(state.questions.size.coerceAtLeast(1))?.let { "%.1f".format(it) } ?: "2"} ${S.marks}")
                        Spacer(Modifier.height(14.dp))
                        NeuDivider()
                        Spacer(Modifier.height(14.dp))
                        InstructionRow(S.negativeMarking, "-${state.testSet?.negativeMarking ?: 0.5} ${S.marks}")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Rules
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(S.rules, style = NeuType.label)
                        Spacer(Modifier.height(12.dp))
                        RuleItem(S.rule1)
                        Spacer(Modifier.height(8.dp))
                        RuleItem(S.rule2)
                        Spacer(Modifier.height(8.dp))
                        RuleItem(S.rule3)
                        Spacer(Modifier.height(8.dp))
                        RuleItem(S.rule4)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Language selector for this test ──
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Translate, null,
                            tint = NeuColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Test Language", style = NeuType.body.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))

                        // EN / HI toggle chips
                        val context = LocalContext.current
                        val isHindi by LanguageManager.isHindi.collectAsState()

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LanguageChip("English", !isHindi) { LanguageManager.setHindi(context, false) }
                            LanguageChip("हिन्दी", isHindi) { LanguageManager.setHindi(context, true) }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                NeuButton(text = S.startTest, onClick = onStart)
            }
        }
    }
}

@Composable
private fun InstructionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = NeuType.bodySecondary)
        Text(value, style = NeuType.body.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun RuleItem(text: String) {
    Row {
        Text("•", style = NeuType.bodySecondary, modifier = Modifier.width(16.dp))
        Text(text, style = NeuType.bodySecondary)
    }
}

@Composable
private fun LanguageChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    val c = NeuColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) {
                if (c.isDark) c.Accent.copy(alpha = 0.2f) else c.AccentSurface
            } else c.SurfaceDeep)
            .then(if (isActive) Modifier.border(1.dp, c.Accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp)) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) c.Accent else c.TextSecondary
        )
    }
}

// ━━━━ ACTIVE TEST ━━━━

@Composable
private fun ActivePhase(
    state: TestState,
    onSelectOption: (String, String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onGoTo: (Int) -> Unit,
    onToggleMark: (String) -> Unit,
    onTogglePalette: () -> Unit,
    onShowSubmit: () -> Unit,
    onCancel: () -> Unit,
    answeredCount: Int,
    unansweredCount: Int,
    markedCount: Int,
    getStatus: (String) -> QuestionStatus
) {
    val q = state.questions.getOrNull(state.currentIndex) ?: return
    val selectedOpt = state.answers[q.id]
    val isMarked = state.markedForReview.contains(q.id)

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar: timer + progress + palette toggle ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeuColors.SurfaceAlt)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer
            val mins = state.timeRemainingSeconds / 60
            val secs = state.timeRemainingSeconds % 60
            val isLow = state.timeRemainingSeconds < 60
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Timer, null,
                    tint = if (isLow) NeuColors.Error else NeuColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "%02d:%02d".format(mins, secs),
                    style = NeuType.body.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (isLow) NeuColors.Error else NeuColors.TextPrimary
                    )
                )
            }

            Spacer(Modifier.weight(1f))

            // Progress
            Text(
                "${state.currentIndex + 1}/${state.questions.size}",
                style = NeuType.small.copy(color = NeuColors.TextSecondary)
            )

            Spacer(Modifier.width(16.dp))

            // Palette toggle
            NeuIconButton(
                icon = Icons.Outlined.GridView,
                onClick = onTogglePalette,
                size = 36.dp,
                iconSize = 16.dp,
                isActive = state.showPalette
            )
        }

        // ── Progress bar ──
        NeuProgressBar(
            progress = (state.currentIndex + 1).toFloat() / state.questions.size,
            height = 3.dp,
            trackColor = NeuColors.SurfaceDeep,
            progressColor = NeuColors.Accent
        )

        // ── Content: Question or Palette ──
        if (state.showPalette) {
            QuestionPalette(
                questions = state.questions,
                currentIndex = state.currentIndex,
                getStatus = getStatus,
                onGoTo = { onGoTo(it); onTogglePalette() },
                answeredCount = answeredCount,
                unansweredCount = unansweredCount,
                markedCount = markedCount,
                onSubmit = onShowSubmit,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Question body + nav buttons all in ONE scrollable area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
            ) {
                // Question number + mark
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${S.question} ${q.questionNumber}",
                        style = NeuType.label
                    )
                    NeuIconButton(
                        icon = if (isMarked) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                        onClick = { onToggleMark(q.id) },
                        size = 36.dp,
                        iconSize = 16.dp,
                        isActive = isMarked
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Question text
                Text(q.questionText, style = NeuType.body.copy(lineHeight = 24.sp))

                // Question image (if present)
                if (!q.questionImageUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    coil.compose.AsyncImage(
                        model = q.questionImageUrl,
                        contentDescription = "Question image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeuColors.SurfaceDeep),
                        contentScale = ContentScale.FillWidth
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Options
                q.options.forEachIndexed { index, opt ->
                    val isSelected = selectedOpt == opt.id
                    OptionCard(
                        option = opt,
                        label = ('A' + index).toString(),
                        isSelected = isSelected,
                        onClick = { onSelectOption(q.id, opt.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // ── Nav arrows — centered, close together, bigger ──
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev
                    if (state.currentIndex > 0) {
                        NeuIconButton(
                            icon = Icons.Outlined.ChevronLeft,
                            onClick = onPrev,
                            size = 52.dp,
                            iconSize = 24.dp
                        )
                    } else {
                        Spacer(Modifier.size(52.dp))
                    }

                    Spacer(Modifier.width(24.dp))

                    // Next
                    if (state.currentIndex < state.questions.size - 1) {
                        NeuIconButton(
                            icon = Icons.Outlined.ChevronRight,
                            onClick = onNext,
                            size = 52.dp,
                            iconSize = 24.dp
                        )
                    } else {
                        Spacer(Modifier.size(52.dp))
                    }
                }

                // Bottom breathing room for fixed bottom bar
                Spacer(Modifier.height(100.dp).navigationBarsPadding())
            }
        }
    } // end Column

    // ── Fixed bottom bar: Submit + Cancel side by side ──
    if (!state.showPalette) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(NeuColors.Background)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeuColors.SurfaceAlt)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancel
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        S.cancelExit,
                        style = NeuType.body.copy(
                            color = NeuColors.Error.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Submit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (NeuColors.isDark) NeuColors.Surface else NeuColors.Accent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onShowSubmit
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        S.submit,
                        style = NeuType.body.copy(
                            color = if (NeuColors.isDark) NeuColors.TextPrimary else Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
    } // end Box
}

// ━━━━ OPTION CARD ━━━━

@Composable
private fun OptionCard(
    option: QuestionOption,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val c = NeuColors
    val interactionSource = remember { MutableInteractionSource() }

    val selectedBg = if (c.isDark) c.Accent.copy(alpha = 0.15f) else c.AccentSurface
    val selectedBorder = if (c.isDark) c.Accent.copy(alpha = 0.6f) else c.Accent.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier
                        .border(1.5.dp, selectedBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(selectedBg)
                } else {
                    Modifier
                        .neuShadow(cornerRadius = 14.dp, intensity = 1f,
                            lightColor = c.ShadowLight, darkColor = c.ShadowDark,
                            surfaceColor = c.Surface, isDark = c.isDark)
                        .clip(RoundedCornerShape(14.dp))
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) c.Accent.copy(alpha = 0.3f) else c.SurfaceDeep),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = NeuType.small.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) c.Accent else c.TextSecondary
                )
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (option.text.isNotBlank()) {
                Text(
                    option.text,
                    style = NeuType.body.copy(
                        color = if (isSelected) {
                            if (c.isDark) Color.White else c.TextPrimary
                        } else c.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                )
            }
            if (!option.imageUrl.isNullOrBlank()) {
                if (option.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                coil.compose.AsyncImage(
                    model = option.imageUrl,
                    contentDescription = "Option image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

// ━━━━ QUESTION PALETTE ━━━━

@Composable
private fun QuestionPalette(
    questions: List<Question>,
    currentIndex: Int,
    getStatus: (String) -> QuestionStatus,
    onGoTo: (Int) -> Unit,
    answeredCount: Int,
    unansweredCount: Int,
    markedCount: Int,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(24.dp)) {
        // Stats row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PaletteStatBadge(S.answered, answeredCount, NeuColors.Success)
            PaletteStatBadge(S.unanswered, unansweredCount, NeuColors.TextMuted)
            PaletteStatBadge(S.marked, markedCount, NeuColors.Warning)
        }

        Spacer(Modifier.height(20.dp))

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(questions) { index, q ->
                val status = getStatus(q.id)
                val isCurrent = index == currentIndex
                val bgColor = when (status) {
                    QuestionStatus.ANSWERED -> NeuColors.Success.copy(alpha = 0.3f)
                    QuestionStatus.MARKED -> NeuColors.Warning.copy(alpha = 0.3f)
                    QuestionStatus.ANSWERED_MARKED -> NeuColors.Success.copy(alpha = 0.4f)
                    QuestionStatus.UNANSWERED -> NeuColors.SurfaceDeep
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .then(if (isCurrent) Modifier.border(1.5.dp, NeuColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp)) else Modifier)
                        .clickable { onGoTo(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        style = NeuType.small.copy(
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            color = NeuColors.TextPrimary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        NeuButton(text = S.submitTest, onClick = onSubmit)
    }
}

@Composable
private fun PaletteStatBadge(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$count", style = NeuType.body.copy(fontWeight = FontWeight.SemiBold, color = color))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = NeuType.caption)
    }
}

// ━━━━ SUBMIT DIALOG ━━━━

@Composable
private fun SubmitDialog(
    answeredCount: Int,
    unansweredCount: Int,
    markedCount: Int,
    totalQuestions: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuColors.Background.copy(alpha = 0.85f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        NeuCard(
            modifier = Modifier
                .padding(32.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* consume */ }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(S.submitTestQ, style = NeuType.h2)
                Spacer(Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$answeredCount", style = NeuType.h3.copy(color = NeuColors.Success))
                        Text(S.answered, style = NeuType.caption)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$unansweredCount", style = NeuType.h3.copy(color = NeuColors.TextMuted))
                        Text(S.skipped, style = NeuType.caption)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$markedCount", style = NeuType.h3.copy(color = NeuColors.Warning))
                        Text(S.marked, style = NeuType.caption)
                    }
                }

                if (unansweredCount > 0) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "$unansweredCount questions unanswered",
                        style = NeuType.small.copy(color = NeuColors.Warning)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeuButton(
                        text = S.cancel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    NeuButton(
                        text = S.submit,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ━━━━ SUBMITTING ━━━━

@Composable
private fun SubmittingPhase() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        // Score ring placeholder
        NeuSkeleton(Modifier.size(140.dp), height = 140.dp, cornerRadius = 70.dp)
        Spacer(Modifier.height(8.dp))
        NeuSkeleton(Modifier.fillMaxWidth(0.45f), height = 20.dp, cornerRadius = 10.dp)
        NeuSkeleton(Modifier.fillMaxWidth(0.65f), height = 14.dp, cornerRadius = 7.dp)
        Spacer(Modifier.height(4.dp))
        // Stat row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeuSkeleton(Modifier.weight(1f), height = 72.dp, cornerRadius = 16.dp)
            NeuSkeleton(Modifier.weight(1f), height = 72.dp, cornerRadius = 16.dp)
            NeuSkeleton(Modifier.weight(1f), height = 72.dp, cornerRadius = 16.dp)
        }
        NeuSkeleton(Modifier.fillMaxWidth(), height = 90.dp, cornerRadius = 20.dp)
        NeuSkeleton(Modifier.fillMaxWidth(), height = 52.dp, cornerRadius = 16.dp)
        Text(S.scoring, style = NeuType.caption)
    }
}

// ━━━━ RESULTS ━━━━

@Composable
private fun ResultsPhase(state: TestState, onBack: () -> Unit, onGoTo: (Int) -> Unit) {
    val result = state.submitResult ?: return
    var showReview by remember { mutableStateOf(false) }

    if (showReview) {
        ReviewPhase(
            reviewQuestions = state.reviewQuestions,
            answers = state.answers,
            onBack = { showReview = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Score ring
        val pct = (result.percentage / 100.0).toFloat().coerceIn(0f, 1f)
        NeuProgressRing(
            progress = pct,
            size = 120.dp,
            strokeWidth = 8.dp,
            progressColor = when {
                result.percentage >= 70 -> NeuColors.Success
                result.percentage >= 40 -> NeuColors.Warning
                else -> NeuColors.Error
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${"%.0f".format(result.percentage)}%",
                    style = NeuType.h2.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(S.score, style = NeuType.caption)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            when {
                result.percentage >= 80 -> S.excellent
                result.percentage >= 60 -> S.goodJob
                result.percentage >= 40 -> S.keepPracticing
                else -> S.needMorePractice
            },
            style = NeuType.h3
        )

        Spacer(Modifier.height(24.dp))

        // Stats grid
        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ResultRow(S.score, "${"%.1f".format(result.score)} / ${result.totalMarks.toInt()}")
                Spacer(Modifier.height(12.dp)); NeuDivider(); Spacer(Modifier.height(12.dp))
                ResultRow(S.correct, "${result.correct}", NeuColors.Success)
                Spacer(Modifier.height(12.dp)); NeuDivider(); Spacer(Modifier.height(12.dp))
                ResultRow(S.incorrect, "${result.incorrect}", NeuColors.Error)
                Spacer(Modifier.height(12.dp)); NeuDivider(); Spacer(Modifier.height(12.dp))
                ResultRow(S.unattempted, "${result.unattempted}", NeuColors.TextMuted)
                Spacer(Modifier.height(12.dp)); NeuDivider(); Spacer(Modifier.height(12.dp))
                val mins = result.timeTaken / 60
                val secs = result.timeTaken % 60
                ResultRow(S.timeTaken, "${mins}m ${secs}s")
            }
        }

        // Error message
        state.error?.let { err ->
            Spacer(Modifier.height(12.dp))
            Text(err, style = NeuType.small.copy(color = NeuColors.Error))
        }

        Spacer(Modifier.height(24.dp))

        // Actions
        if (state.reviewQuestions.isNotEmpty()) {
            NeuButton(text = S.reviewAnswers, onClick = { showReview = true })
            Spacer(Modifier.height(12.dp))
        }

        NeuButton(text = S.goBack, onClick = onBack)
    }
}

@Composable
private fun ResultRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = NeuColors.TextPrimary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = NeuType.bodySecondary)
        Text(value, style = NeuType.body.copy(fontWeight = FontWeight.Medium, color = valueColor))
    }
}

// ━━━━ REVIEW ━━━━

@Composable
private fun ReviewPhase(
    reviewQuestions: List<ReviewQuestion>,
    answers: Map<String, String?>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeuColors.SurfaceAlt)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuIconButton(icon = Icons.Outlined.ArrowBack, onClick = onBack, size = 36.dp, iconSize = 18.dp)
            Spacer(Modifier.width(12.dp))
            Text(S.reviewAnswers, style = NeuType.h3)
        }

        // Questions list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            reviewQuestions.forEach { q ->
                val userAns = answers[q.id]
                val isCorrect = userAns == q.correctOptionId
                val isSkipped = userAns == null

                NeuCard(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column {
                        // Q number + status
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Q${q.questionNumber}", style = NeuType.label)
                            Text(
                                when {
                                    isSkipped -> S.skippedLabel
                                    isCorrect -> S.correctLabel
                                    else -> S.wrongLabel
                                },
                                style = NeuType.caption.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isSkipped -> NeuColors.TextMuted
                                        isCorrect -> NeuColors.Success
                                        else -> NeuColors.Error
                                    }
                                )
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(q.questionText, style = NeuType.body)
                        // Question image in review
                        if (!q.questionImageUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            coil.compose.AsyncImage(
                                model = q.questionImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        // Options with correct/wrong highlighting
                        q.options.forEachIndexed { index, opt ->
                            val isUserAnswer = opt.id == userAns
                            val isCorrectOpt = opt.id == q.correctOptionId
                            val bgColor = when {
                                isCorrectOpt -> NeuColors.Success.copy(alpha = 0.15f)
                                isUserAnswer && !isCorrect -> NeuColors.Error.copy(alpha = 0.15f)
                                else -> NeuColors.SurfaceDeep
                            }
                            val textColor = when {
                                isCorrectOpt -> NeuColors.Success
                                isUserAnswer && !isCorrect -> NeuColors.Error
                                else -> NeuColors.TextSecondary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgColor)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    ('A' + index).toString(),
                                    style = NeuType.small.copy(fontWeight = FontWeight.SemiBold, color = textColor),
                                    modifier = Modifier.width(24.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    if (opt.text.isNotBlank()) {
                                        Text(opt.text, style = NeuType.small.copy(color = textColor))
                                    }
                                    if (!opt.imageUrl.isNullOrBlank()) {
                                        if (opt.text.isNotBlank()) Spacer(Modifier.height(6.dp))
                                        coil.compose.AsyncImage(
                                            model = opt.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp).clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        // Explanation
                        q.explanation?.let { expl ->
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeuColors.SurfaceDeep)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(S.explanation, style = NeuType.label)
                                    Spacer(Modifier.height(4.dp))
                                    Text(expl, style = NeuType.small.copy(color = NeuColors.TextSecondary))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
