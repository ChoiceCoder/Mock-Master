package com.govtprep.app.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.data.model.Profile
import com.govtprep.app.data.model.UserStats
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PROFILE TAB
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ProfileScreen(
    profile: Profile?,
    stats: UserStats?,
    isAdmin: Boolean,
    onAdminClick: () -> Unit,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onSubmitFeedback: (String) -> Unit = {},
    feedbackResult: String? = null,
    onClearFeedback: () -> Unit = {}
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var notificationsOn by remember { mutableStateOf(true) }
    val isDark by ThemeManager.isDarkMode.collectAsState()
    val isHindi by LanguageManager.isHindi.collectAsState()
    val avatarIndex by AvatarManager.avatarIndex.collectAsState()
    var showAvatarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuColors.Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { -20 }
        ) {
            Text(
                S.profile, style = NeuType.h2,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ━━━ 1. USER INFO + AVATAR ━━━
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 80)) + slideInVertically(tween(500, 80)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Avatar with edit badge
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(PRESET_AVATARS[avatarIndex].background)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { showAvatarPicker = !showAvatarPicker },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(PRESET_AVATARS[avatarIndex].emoji, fontSize = 30.sp)
                            }
                            // Camera edit badge
                            Box(
                                modifier = Modifier
                                    .offset(x = (-16).dp, y = 20.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(NeuColors.Accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.CameraAlt, null,
                                    tint = NeuColors.Background,
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(profile?.fullName ?: S.student, style = NeuType.h3)
                                profile?.targetExam?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Text("${S.target}: $it", style = NeuType.bodySecondary)
                                }
                                profile?.email?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Text(it, style = NeuType.caption)
                                }
                            }
                        }

                        // ── Avatar picker grid ──
                        AnimatedVisibility(visible = showAvatarPicker) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                NeuDivider()
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Choose your avatar",
                                    style = NeuType.caption,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PRESET_AVATARS.forEachIndexed { i, av ->
                                        val selected = i == avatarIndex
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(CircleShape)
                                                .background(av.background)
                                                .then(
                                                    if (selected) Modifier.border(2.5.dp, NeuColors.TextPrimary, CircleShape)
                                                    else Modifier
                                                )
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    AvatarManager.setAvatar(context, i)
                                                    showAvatarPicker = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(av.emoji, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ━━━ 2. OVERALL STATS ━━━
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 160)) + slideInVertically(tween(500, 160)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(S.stats, style = NeuType.label)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatItem("${stats?.totalAttempts ?: 0}", S.totalTests, Modifier.weight(1f))
                            StatItem("${stats?.averageScore?.toInt() ?: 0}%", S.avgScore, Modifier.weight(1f))
                            StatItem("${stats?.totalTimeMinutes ?: 0}m", S.studyTime, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ━━━ 3. ACHIEVEMENTS ━━━
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 240)) + slideInVertically(tween(500, 240)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(S.achievements, style = NeuType.label)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val streak   = stats?.testsThisWeek ?: 0
                            val accuracy = stats?.averageScore?.toInt() ?: 0
                            val tests    = stats?.totalAttempts ?: 0
                            AchievementBadge("\uD83D\uDD25", "$streak Day Streak", streak > 0, Modifier.weight(1f))
                            AchievementBadge("\uD83C\uDFAF", S.highAccuracy, accuracy >= 70, Modifier.weight(1f))
                            AchievementBadge("⚡", "10+ Tests", tests >= 10, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ━━━ 4. REFERRAL ━━━
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 300)) + slideInVertically(tween(500, 300)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Referral Program", style = NeuType.label)
                        Spacer(Modifier.height(16.dp))

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ReferralStat("🪙", "${profile?.coins ?: 0}", "Coins Earned", Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(40.dp).background(NeuColors.Divider))
                            ReferralStat("👥", "${profile?.referralCount ?: 0}", "Friends Joined", Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(16.dp))
                        NeuDivider()
                        Spacer(Modifier.height(16.dp))

                        Text("Your Referral Code", style = NeuType.caption)
                        Spacer(Modifier.height(8.dp))

                        val clipboardManager = LocalClipboardManager.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeuColors.AccentUltraLight)
                                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                profile?.referralCode ?: "—",
                                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp, color = NeuColors.Accent,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    profile?.referralCode?.let {
                                        clipboardManager.setText(AnnotatedString(it))
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, null,
                                    tint = NeuColors.TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Share your code with friends. You earn 50 coins per successful referral!",
                            style = NeuType.caption
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ━━━ 5. SETTINGS ━━━
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 360)) + slideInVertically(tween(500, 360)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(S.settings, style = NeuType.label)
                        Spacer(Modifier.height(16.dp))

                        NeuToggle(checked = notificationsOn, onCheckedChange = { notificationsOn = it }, label = S.notifications)
                        Spacer(Modifier.height(14.dp))
                        NeuToggle(
                            checked = !isDark,
                            onCheckedChange = { ThemeManager.setDarkMode(context, !it) },
                            label = S.lightMode
                        )
                        Spacer(Modifier.height(14.dp))
                        NeuToggle(
                            checked = isHindi,
                            onCheckedChange = { LanguageManager.setHindi(context, it) },
                            label = "${S.language} — ${S.hindi}"
                        )

                        Spacer(Modifier.height(16.dp))
                        NeuDivider()
                        Spacer(Modifier.height(16.dp))

                        NeuCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, onClick = onEditProfile) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Edit, null, tint = NeuColors.TextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(S.editProfile, style = NeuType.body)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, tint = NeuColors.TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ━━━ 6. FEEDBACK ━━━
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 420)) + slideInVertically(tween(500, 420)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(S.feedback, style = NeuType.label)
                        Spacer(Modifier.height(12.dp))

                        var feedbackText by remember { mutableStateOf("") }
                        var showMsg by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(feedbackResult) {
                            if (feedbackResult == "success") {
                                feedbackText = ""
                                showMsg = "success"
                                kotlinx.coroutines.delay(3000)
                                showMsg = null
                                onClearFeedback()
                            } else if (feedbackResult != null && feedbackResult.startsWith("error")) {
                                showMsg = feedbackResult
                                kotlinx.coroutines.delay(5000)
                                showMsg = null
                                onClearFeedback()
                            }
                        }

                        NeuTextField(value = feedbackText, onValueChange = { feedbackText = it },
                            placeholder = S.feedbackHint, singleLine = false)
                        Spacer(Modifier.height(12.dp))

                        val emptyMsg = S.feedbackEmpty
                        NeuButton(
                            text = S.sendFeedback,
                            onClick = {
                                if (feedbackText.isBlank()) showMsg = "empty"
                                else onSubmitFeedback(feedbackText.trim())
                            },
                            icon = Icons.Outlined.Send
                        )

                        showMsg?.let { msg ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                when {
                                    msg == "success"       -> S.feedbackSent
                                    msg == "empty"         -> emptyMsg
                                    msg.startsWith("error:") -> msg.removePrefix("error:")
                                    else                   -> "Failed to send"
                                },
                                style = NeuType.caption.copy(
                                    color = if (msg == "success") NeuColors.Success
                                    else if (msg == "empty") NeuColors.Warning
                                    else NeuColors.Error
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isAdmin) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, 460))) {
                Box(Modifier.padding(horizontal = 24.dp)) {
                    NeuButton(text = S.adminPanel, onClick = onAdminClick, icon = Icons.Outlined.AdminPanelSettings)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, if (isAdmin) 500 else 460))) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuButton(text = S.signOut, onClick = onSignOut, icon = Icons.Outlined.Logout)
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

// ━━━ HELPERS ━━━

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NeuType.h3.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Text(label, style = NeuType.caption)
    }
}

@Composable
private fun ReferralStat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeuColors.TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, style = NeuType.caption)
    }
}

@Composable
private fun AchievementBadge(emoji: String, title: String, earned: Boolean, modifier: Modifier = Modifier) {
    val alpha  = if (earned) 1f else 0.35f
    val badgeBg = if (NeuColors.isDark) NeuColors.SurfaceDeep else {
        if (earned) NeuColors.AccentSurface else NeuColors.SurfaceAlt
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(badgeBg.copy(alpha = if (earned) 1f else 0.5f))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            title,
            style = NeuType.caption.copy(
                fontWeight = FontWeight.Medium,
                color = NeuColors.TextSecondary.copy(alpha = alpha)
            ),
            maxLines = 1
        )
    }
}
