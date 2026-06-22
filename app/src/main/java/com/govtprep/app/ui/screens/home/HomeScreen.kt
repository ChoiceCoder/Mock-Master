package com.govtprep.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.govtprep.app.data.model.Attempt
import com.govtprep.app.data.model.Profile
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HOME — Fintech Dashboard + Hero Actions
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun HomeScreen(
    state: HomeState,
    onRefresh: () -> Unit,
    onStartQuickTest: () -> Unit,
    onResumeTest: (() -> Unit)? = null,
    onAnalytics: () -> Unit,
    onDailyQuiz: () -> Unit = {},
    onPYQ: () -> Unit = {},
    onPremiumClick: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val c = NeuColors

    // Subscription detail dialog state
    var showSubDetail by remember { mutableStateOf(false) }

    if (showSubDetail) {
        SubscriptionDetailDialog(
            profile = state.profile,
            onDismiss = { showSubDetail = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(c.Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        if (state.isLoading) {
            item { HomeLoadingSkeleton() }
            return@LazyColumn
        }

        // ━━━ 1. GREETING + PREMIUM / KING BUTTON ━━━
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { -20 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            when {
                                state.greetingHour < 12 -> S.goodMorning
                                state.greetingHour < 17 -> S.goodAfternoon
                                else -> S.goodEvening
                            },
                            fontSize = 14.sp, color = c.TextSecondary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            state.profile?.fullName ?: S.student,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.TextPrimary
                        )
                    }

                    // Dynamically pick button based on subscription status
                    val daysLeft = daysRemainingInSubscription(state.profile)
                    if (state.profile?.isPremium == true && daysLeft != null && daysLeft > 0) {
                        ActivePremiumButton(
                            daysLeft = daysLeft,
                            onClick = { showSubDetail = true }
                        )
                    } else {
                        GoldenPremiumButton(onClick = onPremiumClick)
                    }
                }
            }
        }

        // ━━━ 2. HERO QUICK ACTIONS (2×2 Grid) ━━━
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 80)) + slideInVertically(tween(500, 80)) { 30 }
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HeroActionItem(Icons.Outlined.Bolt, S.startQuickTest, onResumeTest ?: onStartQuickTest, Modifier.weight(1f))
                        HeroActionItem(Icons.Outlined.CalendarMonth, S.dailyQuiz, onDailyQuiz, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HeroActionItem(Icons.Outlined.Insights, S.analytics, onAnalytics, Modifier.weight(1f))
                        HeroActionItem(Icons.Outlined.Description, S.previousYearPapers, onPYQ, Modifier.weight(1f))
                    }
                }
            }
        }

        // ━━━ 3. TODAY'S PROGRESS (streak inside) ━━━
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 160)) + slideInVertically(tween(500, 160)) { 30 }
            ) {
                Box(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    val todayTests = state.stats?.testsThisWeek ?: 0
                    val todayTime  = state.stats?.totalTimeMinutes ?: 0
                    val streak     = state.stats?.testsThisWeek ?: 0
                    val goal       = 3
                    val progress   = (todayTests.toFloat() / goal).coerceIn(0f, 1f)

                    NeuCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(S.todaysProgress, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                    color = NeuColors.TextMuted, letterSpacing = 1.sp)
                                if (streak > 0) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50.dp))
                                            .background(NeuColors.AccentUltraLight)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("\uD83D\uDD25 $streak Day ${S.streak}", fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium, color = NeuColors.AccentPressed)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("$todayTests", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeuColors.TextPrimary)
                                    Text(S.testsToday, fontSize = 13.sp, color = NeuColors.TextSecondary)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("${todayTime}m", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NeuColors.TextPrimary)
                                    Text(S.studyTime, fontSize = 13.sp, color = NeuColors.TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            NeuProgressBar(progress = progress, height = 6.dp,
                                trackColor = NeuColors.AccentSurface, progressColor = NeuColors.Accent)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (todayTests >= goal) S.dailyGoalReached else "${goal - todayTests} ${S.moreToGoal}",
                                fontSize = 13.sp, color = NeuColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ━━━ 4. PERFORMANCE ━━━
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 240)) + slideInVertically(tween(500, 240)) { 30 }
            ) {
                Box(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    val stats = state.stats
                    NeuCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column {
                            Text(S.performance, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                color = NeuColors.TextMuted, letterSpacing = 1.sp)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                                MetricColumn("${stats?.averageScore?.toInt() ?: 0}%", S.accuracy, Modifier.weight(1f))
                                Box(Modifier.width(1.dp).height(40.dp).background(NeuColors.Divider))
                                MetricColumn("${stats?.totalAttempts ?: 0}", S.totalTests, Modifier.weight(1f))
                                Box(Modifier.width(1.dp).height(40.dp).background(NeuColors.Divider))
                                MetricColumn("${"%.1f".format(stats?.averageScore ?: 0.0)}", S.avgScore, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // ━━━ 5. RECENT TESTS ━━━
        val recentTests = state.recentAttempts.take(3)
        if (recentTests.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(300, 320))) {
                    Text(S.recentTests, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = NeuColors.TextMuted, letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp))
                }
            }
            recentTests.forEachIndexed { i, attempt ->
                item(key = attempt.id) {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(300, 360 + i * 50)) + slideInVertically(tween(400, 360 + i * 50)) { 20 }
                    ) {
                        RecentTestCard(attempt, Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUBSCRIPTION HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/** Returns days remaining, null if no valid subscription date */
fun daysRemainingInSubscription(profile: Profile?): Long? {
    val until = profile?.subscribedUntil ?: return null
    return try {
        val expiry = LocalDate.parse(until, DateTimeFormatter.ISO_LOCAL_DATE)
        val today  = LocalDate.now()
        val days   = ChronoUnit.DAYS.between(today, expiry)
        if (days >= 0) days else null
    } catch (_: Exception) { null }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PREMIUM SHIMMER BUTTON (non-premium)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun GoldenPremiumButton(onClick: () -> Unit) {
    val c = NeuColors
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -300f, targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = LinearEasing, delayMillis = 800),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerOffset"
    )
    // Dark → charcoal bg + light shimmer, Light → emerald bg + white shimmer
    val bgGradient = if (c.isDark)
        Brush.horizontalGradient(listOf(Color(0xFF2A2A2D), Color(0xFF1F1F21)))
    else
        Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF047857)))
    val shimmerColor = if (c.isDark) Color.White else Color.White
    val iconTextColor = if (c.isDark) c.TextPrimary else Color.White

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgGradient)
            .drawWithContent {
                drawContent()
                val h = size.height
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent, shimmerColor.copy(alpha = 0f),
                            shimmerColor.copy(alpha = 0.18f), shimmerColor.copy(alpha = 0.30f),
                            shimmerColor.copy(alpha = 0.18f), shimmerColor.copy(alpha = 0f), Color.Transparent
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end   = Offset(shimmerOffset + 220f, h)
                    )
                )
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.WorkspacePremium, null, tint = iconTextColor, modifier = Modifier.size(18.dp))
            Text("Go Premium", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = iconTextColor, letterSpacing = 0.3.sp)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ACTIVE PREMIUM BUTTON (crown + days)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun ActivePremiumButton(daysLeft: Long, onClick: () -> Unit) {
    val c = NeuColors
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.08f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val bgColor = if (c.isDark) Color(0xFF2A2A2D) else Color(0xFF059669)
    val glowColor = if (c.isDark) Color.White else Color.White
    val textColor = if (c.isDark) c.TextPrimary else Color.White

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .drawWithContent {
                drawRect(Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height / 2f),
                    radius = size.height * 1.8f
                ))
                drawContent()
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("👑", fontSize = 18.sp)
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "${daysLeft}d left",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = textColor, letterSpacing = 0.2.sp
                )
                Text(
                    "Premium",
                    fontSize = 9.sp, fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.7f), letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUBSCRIPTION DETAIL DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun SubscriptionDetailDialog(profile: Profile?, onDismiss: () -> Unit) {
    val c    = NeuColors
    val gold = Color(0xFFFFD700)
    val daysLeft   = daysRemainingInSubscription(profile) ?: 0
    val planName   = profile?.planName ?: "Premium Plan"
    val expiryDate = profile?.subscribedUntil ?: "—"

    // Format expiry date nicely e.g. "14 Jun 2025"
    val expiryFormatted = try {
        val d = LocalDate.parse(expiryDate, DateTimeFormatter.ISO_LOCAL_DATE)
        d.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (_: Exception) { expiryDate }

    // Progress: if we know plan duration, else rough estimate from name
    val totalDays = when {
        planName.contains("1 Month", true)  ->  30L
        planName.contains("3 Month", true)  ->  90L
        planName.contains("6 Month", true)  -> 180L
        planName.contains("1 Year",  true)  -> 365L
        else                                ->  30L
    }
    val progress = (daysLeft.toFloat() / totalDays).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.Surface)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Crown + title
                    Text("👑", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Active Premium",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = gold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(planName, style = NeuType.bodySecondary)

                    Spacer(Modifier.height(24.dp))

                    // Days remaining ring-style stat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.AccentUltraLight)
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$daysLeft",
                                fontSize = 48.sp, fontWeight = FontWeight.Bold, color = gold
                            )
                            Text("days remaining", style = NeuType.bodySecondary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Progress bar
                    NeuProgressBar(
                        progress = progress, height = 8.dp,
                        trackColor = c.SurfaceDeep,
                        progressColor = gold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Today", style = NeuType.caption)
                        Text("Expires $expiryFormatted", style = NeuType.caption)
                    }

                    Spacer(Modifier.height(20.dp))
                    NeuDivider()
                    Spacer(Modifier.height(20.dp))

                    // Plan perks
                    Text("Your Plan Includes", style = NeuType.label)
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        "✅  Unlimited mock tests",
                        "✅  Full mock + subject-wise sets",
                        "✅  In-depth analytics",
                        "✅  Hindi + English support",
                        if (planName.contains("1 Year", true))
                            "✅  Previous year question papers" else "—  Previous year papers (Yearly)"
                    ).forEach { perk ->
                        val muted = perk.startsWith("—")
                        Text(
                            perk,
                            style = NeuType.body.copy(
                                color = if (muted) c.TextMuted else c.TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Close button
                    NeuButton(text = "Close", onClick = onDismiss)
                }
            }
        }
    }
}

// ━━━ HERO ACTION ITEM ━━━
@Composable
private fun HeroActionItem(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = NeuColors
    NeuCard(modifier = modifier, cornerRadius = 16.dp, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.AccentSurface), Alignment.Center) {
                Icon(icon, null, tint = c.AccentPressed, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.TextPrimary, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ━━━ METRIC COLUMN ━━━
@Composable
private fun MetricColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = NeuColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = NeuColors.TextSecondary)
    }
}

// ━━━ RECENT TEST CARD ━━━
@Composable
private fun RecentTestCard(attempt: Attempt, modifier: Modifier = Modifier) {
    val c   = NeuColors
    val pct = if (attempt.totalMarks != null && attempt.totalMarks > 0)
        ((attempt.score ?: 0.0) / attempt.totalMarks * 100).toInt() else 0
    val dateStr    = attempt.createdAt?.take(10) ?: ""
    val scoreBg    = when { pct >= 70 -> c.AccentSurface; pct >= 40 -> c.Warning.copy(alpha = 0.12f); else -> c.Error.copy(alpha = 0.1f) }
    val scoreColor = when { pct >= 70 -> c.AccentPressed; pct >= 40 -> c.Warning; else -> c.Error }

    NeuCard(modifier = modifier, cornerRadius = 20.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(50.dp)).background(scoreBg), Alignment.Center) {
                Text("${pct}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scoreColor)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(attempt.testSet?.title ?: "Test", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.TextPrimary, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(dateStr, fontSize = 12.sp, color = c.TextSecondary)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = c.TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// ━━━ LOADING SKELETON ━━━
@Composable
private fun HomeLoadingSkeleton() {
    Column(Modifier.fillMaxWidth().padding(24.dp).statusBarsPadding()) {
        Spacer(Modifier.height(24.dp))
        NeuSkeleton(Modifier.width(100.dp), height = 12.dp)
        Spacer(Modifier.height(6.dp))
        NeuSkeleton(Modifier.width(160.dp), height = 22.dp)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
            NeuSkeleton(Modifier.weight(1f).height(100.dp), cornerRadius = 16.dp)
            NeuSkeleton(Modifier.weight(1f).height(100.dp), cornerRadius = 16.dp)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
            NeuSkeleton(Modifier.weight(1f).height(100.dp), cornerRadius = 16.dp)
            NeuSkeleton(Modifier.weight(1f).height(100.dp), cornerRadius = 16.dp)
        }
        Spacer(Modifier.height(24.dp))
        NeuSkeleton(Modifier.fillMaxWidth().height(130.dp), cornerRadius = 20.dp)
        Spacer(Modifier.height(16.dp))
        NeuSkeleton(Modifier.fillMaxWidth().height(90.dp), cornerRadius = 20.dp)
    }
}
