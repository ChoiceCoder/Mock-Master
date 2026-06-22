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
fun LoginScreen(
    authState: AuthState,
    sessionCheck: SessionCheck,
    onSignIn: (email: String, password: String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(sessionCheck) { if (sessionCheck == SessionCheck.LOGGED_IN) onNavigateToHome() }
    LaunchedEffect(authState)   { if (authState is AuthState.Success) onNavigateToHome() }

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible  by remember { mutableStateOf(false) }
    var owlState by remember { mutableStateOf(OwlState.IDLE) }

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

    if (sessionCheck == SessionCheck.LOADING) {
        Box(Modifier.fillMaxSize().background(NeuColors.Background), Alignment.Center) {
            CircularProgressIndicator(color = NeuColors.TextMuted, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
        }
        return
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
            Spacer(Modifier.height(48.dp))

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
                                OwlState.IDLE     -> "👀  Hey there, future topper!"
                                OwlState.EMAIL    -> "📧  Ooh, entering email!"
                                OwlState.PASSWORD -> "🫣  We're NOT looking... seriously!"
                                OwlState.SUCCESS  -> "🎉  YAY! They're back!"
                                OwlState.ERROR    -> "😬  Uh oh... check that again!"
                                else              -> "👀  Hey there, future topper!"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeuColors.AccentPressed
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── TITLE ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 150)) + slideInVertically(tween(500, 150)) { -20 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(S.welcomeBack, style = NeuType.displayLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(S.signInContinue, style = NeuType.bodySecondary, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── EMAIL ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 250)) + slideInVertically(tween(500, 250)) { 30 }
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

            Spacer(Modifier.height(20.dp))

            // ── PASSWORD ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 330)) + slideInVertically(tween(500, 330)) { 30 }
            ) {
                Column {
                    Text(S.password, style = NeuType.label, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    NeuTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        isPassword = true,
                        modifier = Modifier.onFocusChanged {
                            owlState = if (it.isFocused) OwlState.PASSWORD else OwlState.IDLE
                        }
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

            Spacer(Modifier.height(32.dp))

            // ── SIGN IN BUTTON ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 420)) + slideInVertically(tween(500, 420)) { 30 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoading) {
                        CircularProgressIndicator(color = NeuColors.TextMuted, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(24.dp))
                    } else {
                        NeuButton(
                            text = S.signIn,
                            onClick = { onSignIn(email.trim(), password) },
                            enabled = email.isNotBlank() && password.isNotBlank()
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── SIGN UP LINK ──
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, 500))
            ) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(S.noAccount, style = NeuType.small)
                    Text(
                        S.createOne,
                        style = NeuType.small.copy(color = NeuColors.Accent, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            onClick = onNavigateToSignup
                        )
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
