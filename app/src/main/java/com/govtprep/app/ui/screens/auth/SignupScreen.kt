package com.govtprep.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

@Composable
fun SignupScreen(
    authState: AuthState,
    onSignUp: (email: String, password: String, fullName: String, referralCode: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(authState) { if (authState is AuthState.Success) onNavigateToHome() }

    var name         by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var visible      by remember { mutableStateOf(false) }
    var owlState     by remember { mutableStateOf(OwlState.IDLE) }

    LaunchedEffect(Unit) { visible = true }

    val isLoading = authState is AuthState.Loading
    val error     = (authState as? AuthState.Error)?.message
    val isDark    by ThemeManager.isDarkMode.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> owlState = OwlState.SUCCESS
            is AuthState.Error   -> owlState = OwlState.ERROR
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize().background(NeuColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // ── OWL MASCOT ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(600)) { -50 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OwlMascot(state = owlState, isDark = isDark)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NeuColors.AccentUltraLight)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = when (owlState) {
                                OwlState.IDLE     -> "👀  New crew member incoming!"
                                OwlState.NAME     -> "✏️  Ooh what do we call you?"
                                OwlState.EMAIL    -> "📧  Drop that email right here!"
                                OwlState.PASSWORD -> "🫣  We avert our eyes... promise!"
                                OwlState.SUCCESS  -> "🎉  WOOHOO! Welcome to the crew!"
                                OwlState.ERROR    -> "😬  Sus... something went wrong!"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeuColors.AccentPressed
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── TITLE ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 150)) + slideInVertically(tween(500, 150)) { -20 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(S.createAccount, style = NeuType.displayLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(S.startJourney, style = NeuType.bodySecondary, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── FULL NAME ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 220)) + slideInVertically(tween(500, 220)) { 30 }
            ) {
                Column {
                    Text(S.fullName, style = NeuType.label, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    NeuTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = S.namePlaceholder,
                        modifier = Modifier.onFocusChanged {
                            owlState = if (it.isFocused) OwlState.NAME else OwlState.IDLE
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── EMAIL ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 290)) + slideInVertically(tween(500, 290)) { 30 }
            ) {
                Column {
                    Text(S.email, style = NeuType.label, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    NeuTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = S.emailPlaceholder,
                        modifier = Modifier.onFocusChanged {
                            owlState = if (it.isFocused) OwlState.EMAIL else OwlState.IDLE
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── PASSWORD ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 360)) + slideInVertically(tween(500, 360)) { 30 }
            ) {
                Column {
                    Text(S.password, style = NeuType.label, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    NeuTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = S.minChars,
                        isPassword = true,
                        modifier = Modifier.onFocusChanged {
                            owlState = if (it.isFocused) OwlState.PASSWORD else OwlState.IDLE
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── REFERRAL CODE ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 430)) + slideInVertically(tween(500, 430)) { 30 }
            ) {
                Column {
                    Text(
                        "Referral Code (Optional)",
                        style = NeuType.label,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )
                    NeuTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it.uppercase().take(12) },
                        placeholder = "e.g. ANURAG7842",
                        modifier = Modifier.onFocusChanged {
                            if (it.isFocused) owlState = OwlState.IDLE
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Have a friend's code? Enter it and they earn coins!",
                        style = NeuType.caption,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ── ERROR ──
            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeuColors.Error.copy(alpha = 0.13f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(error, style = NeuType.small.copy(color = NeuColors.Error.copy(alpha = 0.9f)))
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── CREATE BUTTON ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 510)) + slideInVertically(tween(500, 510)) { 30 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoading) {
                        CircularProgressIndicator(color = NeuColors.TextMuted, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(24.dp))
                    } else {
                        NeuButton(
                            text = S.createAccount,
                            onClick = { onSignUp(email.trim(), password, name.trim(), referralCode.trim()) },
                            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── LOGIN LINK ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 580))
            ) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(S.alreadyHaveAccount, style = NeuType.small)
                    Text(
                        S.signInLink,
                        style = NeuType.small.copy(color = NeuColors.Accent, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            onClick = onNavigateToLogin
                        )
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
