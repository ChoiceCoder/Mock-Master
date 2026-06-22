package com.govtprep.app.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.govtprep.app.data.model.Profile
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EDIT PROFILE — Name, Phone, Password
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun EditProfileScreen(
    profile: Profile?,
    onSaveProfile: (name: String, phone: String) -> Unit,
    onChangePassword: (newPassword: String) -> Unit,
    onBack: () -> Unit,
    isSaving: Boolean = false,
    saveMessage: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var name by remember(profile) { mutableStateOf(profile?.fullName ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuColors.Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        // Header
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { -20 }
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                NeuIconButton(
                    icon = Icons.Outlined.ArrowBack,
                    onClick = onBack,
                    size = 44.dp,
                    iconSize = 20.dp
                )
                Spacer(Modifier.height(20.dp))
                Text(S.editProfile, style = NeuType.h2)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Personal Info ──
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 80)) + slideInVertically(tween(500, 80)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(S.personalInfo, style = NeuType.label)
                        Spacer(Modifier.height(16.dp))

                        Text(S.fullName, style = NeuType.caption)
                        Spacer(Modifier.height(6.dp))
                        NeuTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = S.namePlaceholder
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(S.phoneNumber, style = NeuType.caption)
                        Spacer(Modifier.height(6.dp))
                        NeuTextField(
                            value = phone,
                            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                            placeholder = S.phonePlaceholder
                        )

                        Spacer(Modifier.height(20.dp))

                        NeuButton(
                            text = if (isSaving) S.saving else S.saveChanges,
                            onClick = { onSaveProfile(name.trim(), phone.trim()) },
                            enabled = !isSaving && name.isNotBlank(),
                            icon = Icons.Outlined.Check
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Change Password ──
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, 160)) + slideInVertically(tween(500, 160)) { 30 }
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(S.changePassword, style = NeuType.label)
                        Spacer(Modifier.height(16.dp))

                        Text(S.newPassword, style = NeuType.caption)
                        Spacer(Modifier.height(6.dp))
                        NeuTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                passwordError = null
                            },
                            placeholder = S.minChars,
                            isPassword = true
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(S.confirmPassword, style = NeuType.caption)
                        Spacer(Modifier.height(6.dp))
                        NeuTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                passwordError = null
                            },
                            placeholder = S.reEnterPassword,
                            isPassword = true
                        )

                        passwordError?.let { err ->
                            Spacer(Modifier.height(8.dp))
                            Text(err, style = NeuType.caption.copy(color = NeuColors.Error))
                        }

                        Spacer(Modifier.height(20.dp))

                        val pwdMinErr = S.passwordMinError
                        val pwdMismatchErr = S.passwordMismatch
                        NeuButton(
                            text = S.updatePassword,
                            onClick = {
                                when {
                                    newPassword.length < 6 -> passwordError = pwdMinErr
                                    newPassword != confirmPassword -> passwordError = pwdMismatchErr
                                    else -> {
                                        onChangePassword(newPassword)
                                        newPassword = ""
                                        confirmPassword = ""
                                    }
                                }
                            },
                            enabled = !isSaving && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
                        )
                    }
                }
            }
        }

        // ── Status message ──
        saveMessage?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (msg.contains("fail", ignoreCase = true) || msg.contains("error", ignoreCase = true))
                            NeuColors.Error.copy(alpha = 0.15f)
                        else NeuColors.Success.copy(alpha = 0.15f)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    msg,
                    style = NeuType.small.copy(
                        color = if (msg.contains("fail", ignoreCase = true) || msg.contains("error", ignoreCase = true))
                            NeuColors.Error else NeuColors.Success
                    )
                )
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}
