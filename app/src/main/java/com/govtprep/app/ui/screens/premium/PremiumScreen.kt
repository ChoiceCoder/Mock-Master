package com.govtprep.app.ui.screens.premium

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.govtprep.app.ui.components.*
import com.govtprep.app.ui.theme.*

private data class PremiumPlan(
    val duration: String,
    val price: String,
    val pricePerMonth: String,
    val isBestValue: Boolean = false,
    val savings: String? = null,
    val features: List<String>
)

private val PLANS = listOf(
    PremiumPlan(
        duration = "1 Month", price = "₹149", pricePerMonth = "₹149/mo",
        features = listOf("All mock tests", "Subject-wise tests", "Detailed analysis", "Hindi + English")
    ),
    PremiumPlan(
        duration = "3 Months", price = "₹399", pricePerMonth = "₹133/mo",
        savings = "Save ₹48",
        features = listOf("All mock tests", "Subject-wise tests", "Detailed analysis", "Hindi + English")
    ),
    PremiumPlan(
        duration = "6 Months", price = "₹599", pricePerMonth = "₹100/mo",
        savings = "Save ₹295",
        features = listOf("All mock tests", "Subject-wise tests", "Detailed analysis", "Hindi + English")
    ),
    PremiumPlan(
        duration = "1 Year", price = "₹799", pricePerMonth = "₹67/mo",
        isBestValue = true, savings = "Save ₹989",
        features = listOf("All mock tests", "Subject-wise tests", "Detailed analysis", "Hindi + English", "Previous year papers")
    )
)

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val c = NeuColors
    val emerald = Color(0xFF10B981)
    var visible by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) { kotlinx.coroutines.delay(2800); showSnackbar = false }
    }

    Box(Modifier.fillMaxSize().background(c.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = c.TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Text("Go Premium", style = NeuType.h2)
            }

            // ── Hero ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.AccentSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 38.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Unlock Your Full Potential",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = c.TextPrimary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Access all mock tests, analytics,\nand unlimited practice sessions",
                    style = NeuType.bodySecondary, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Plan Cards — inside Column so AnimatedVisibility has ColumnScope ──
            Column(Modifier.fillMaxWidth()) {
                PLANS.forEachIndexed { i, plan ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(350, 80 + i * 70)) + slideInVertically(tween(450, 80 + i * 70)) { 30 }
                    ) {
                        PlanCard(
                            plan = plan,
                            emerald = emerald,
                            onSubscribe = { showSnackbar = true },
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── What's included ──
            Box(Modifier.padding(horizontal = 24.dp)) {
                NeuCard(Modifier.fillMaxWidth()) {
                    Column {
                        Text("What's Included", style = NeuType.label)
                        Spacer(Modifier.height(14.dp))
                        listOf(
                            "✅  Unlimited mock tests across all exams",
                            "✅  Full mock + subject-wise test sets",
                            "✅  In-depth performance analytics",
                            "✅  Hindi + English bilingual support",
                            "✅  Previous year question papers",
                            "✅  Priority question explanations",
                            "✅  Ad-free experience"
                        ).forEach {
                            Text(it, style = NeuType.body, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // ── Snackbar ──
        if (showSnackbar) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                containerColor = emerald,
                contentColor = Color.White
            ) {
                Text("Coming Soon 🚀  Payment integration is on the way!", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    emerald: Color,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = NeuColors
    Box(modifier) {
        NeuCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(plan.duration, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.TextPrimary)
                            if (plan.isBestValue) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(emerald)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "BEST VALUE",
                                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                        color = Color.White, letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(plan.pricePerMonth, fontSize = 13.sp, color = c.TextSecondary)
                            plan.savings?.let {
                                Text(it, fontSize = 13.sp, color = emerald, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Text(
                        plan.price,
                        fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = if (plan.isBestValue) emerald else c.TextPrimary
                    )
                }

                Spacer(Modifier.height(14.dp))
                NeuDivider()
                Spacer(Modifier.height(12.dp))

                plan.features.forEach { f ->
                    Text("• $f", style = NeuType.bodySecondary, modifier = Modifier.padding(vertical = 2.dp))
                }

                Spacer(Modifier.height(16.dp))
                NeuButton(text = "Subscribe  —  ${plan.price}", onClick = onSubscribe)
            }
        }
    }
}
