package com.govtprep.app.ui.screens.splash

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.govtprep.app.ui.theme.S
import kotlinx.coroutines.delay

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SPLASH — Static Stacked Books + Tagline
// No animation. Shows for 2s then navigates.
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val BG_TOP    = Color(0xFFFDB94E)
private val BG_BOTTOM = Color(0xFFF28B30)

private val BOOK_1 = Color(0xFFE8443A)
private val BOOK_2 = Color(0xFF2E86C1)
private val BOOK_3 = Color(0xFF28A745)
private val BOOK_4 = Color(0xFF6C3483)
private val BOOK_5 = Color(0xFF1A1A2E)

private val PAGE_CREAM = Color(0xFFFFF8E8)
private val PAGE_LINE  = Color(0xFFE0D5C0)
private val SHADOW_COL = Color(0x30000000)
private val TAG_DARK   = Color(0xFF2D1B06)
private val SUB_DARK   = Color(0xFF7A5C38)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val density = LocalDensity.current
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = BG_TOP.toArgb()
        window.navigationBarColor = BG_BOTTOM.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    // Just wait then navigate — no animation
    LaunchedEffect(Unit) {
        delay(2000)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BG_TOP, BG_BOTTOM))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── BOOK STACK ──
            Canvas(
                modifier = Modifier
                    .width(260.dp)
                    .height(260.dp)
            ) {
                val w = size.width
                val h = size.height
                val dp = density.density

                // Ground shadow
                drawOval(
                    color = SHADOW_COL,
                    topLeft = Offset(w * 0.15f, h * 0.88f),
                    size = Size(w * 0.7f, h * 0.08f)
                )

                data class BookData(
                    val color: Color, val bw: Float, val bh: Float,
                    val rot: Float, val yOff: Float, val xOff: Float = 0f
                )

                val baseY = h * 0.82f
                val books = listOf(
                    BookData(BOOK_1, w * 0.72f, 38f * dp, 3f,    0f,        -4f * dp),
                    BookData(BOOK_2, w * 0.68f, 36f * dp, -2f,   -36f * dp,  6f * dp),
                    BookData(BOOK_3, w * 0.74f, 40f * dp, 1.5f,  -72f * dp, -2f * dp),
                    BookData(BOOK_4, w * 0.66f, 34f * dp, -3f,   -108f * dp, 8f * dp),
                    BookData(BOOK_5, w * 0.70f, 42f * dp, 0.5f,  -142f * dp, 0f),
                )

                books.forEach { book ->
                    val cx = w / 2f + book.xOff
                    val cy = baseY + book.yOff

                    rotate(degrees = book.rot, pivot = Offset(cx, cy)) {
                        val left = cx - book.bw / 2f
                        val top = cy - book.bh

                        // Shadow
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.12f),
                            topLeft = Offset(left + 3 * dp, top + 3 * dp),
                            size = Size(book.bw, book.bh),
                            cornerRadius = CornerRadius(4 * dp)
                        )
                        // Body
                        drawRoundRect(
                            color = book.color,
                            topLeft = Offset(left, top),
                            size = Size(book.bw, book.bh),
                            cornerRadius = CornerRadius(4 * dp)
                        )
                        // Spine
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.2f),
                            topLeft = Offset(left, top),
                            size = Size(8 * dp, book.bh),
                            cornerRadius = CornerRadius(4 * dp)
                        )
                        // Page edges
                        drawRoundRect(
                            color = PAGE_CREAM,
                            topLeft = Offset(left + book.bw - 6 * dp, top + 3 * dp),
                            size = Size(5 * dp, book.bh - 6 * dp),
                            cornerRadius = CornerRadius(1 * dp)
                        )
                        // Top highlight
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(left + 10 * dp, top + 2 * dp),
                            end = Offset(left + book.bw - 10 * dp, top + 2 * dp),
                            strokeWidth = 1.5f * dp, cap = StrokeCap.Round
                        )
                    }
                }

                // ── OPEN BOOK on top ──
                val openCx = w * 0.5f
                val openCy = baseY - 170f * dp
                val openW = 70f * dp
                val openH = 50f * dp

                // Left page
                rotate(degrees = -8f, pivot = Offset(openCx, openCy)) {
                    drawRoundRect(Color.Black.copy(alpha = 0.08f),
                        Offset(openCx - openW + 2 * dp, openCy - openH / 2 + 2 * dp),
                        Size(openW, openH), CornerRadius(3 * dp))
                    drawRoundRect(PAGE_CREAM,
                        Offset(openCx - openW, openCy - openH / 2),
                        Size(openW, openH), CornerRadius(3 * dp))
                    for (i in 1..5) {
                        val ly = openCy - openH / 2 + openH * (i / 6f)
                        val lEnd = if (i == 5) openCx - openW * 0.4f else openCx - 6 * dp
                        drawLine(PAGE_LINE, Offset(openCx - openW + 8 * dp, ly), Offset(lEnd, ly), 0.8f * dp)
                    }
                }

                // Right page
                rotate(degrees = 8f, pivot = Offset(openCx, openCy)) {
                    drawRoundRect(Color.Black.copy(alpha = 0.08f),
                        Offset(openCx + 2 * dp, openCy - openH / 2 + 2 * dp),
                        Size(openW, openH), CornerRadius(3 * dp))
                    drawRoundRect(PAGE_CREAM,
                        Offset(openCx, openCy - openH / 2),
                        Size(openW, openH), CornerRadius(3 * dp))
                    for (i in 1..5) {
                        val ly = openCy - openH / 2 + openH * (i / 6f)
                        val lEnd = if (i == 5) openCx + openW * 0.55f else openCx + openW - 8 * dp
                        drawLine(PAGE_LINE, Offset(openCx + 6 * dp, ly), Offset(lEnd, ly), 0.8f * dp)
                    }
                }

                // Spine line
                drawLine(Color.Black.copy(alpha = 0.15f),
                    Offset(openCx, openCy - openH / 2 - 4 * dp),
                    Offset(openCx, openCy + openH / 2 + 2 * dp),
                    1.5f * dp, cap = StrokeCap.Round)

                // Floating pages
                rotate(degrees = -20f, pivot = Offset(w * 0.18f, h * 0.35f)) {
                    drawRoundRect(PAGE_CREAM.copy(alpha = 0.6f),
                        Offset(w * 0.12f, h * 0.30f), Size(32 * dp, 24 * dp), CornerRadius(2 * dp))
                    for (i in 1..3) {
                        val ly = h * 0.30f + 24 * dp * (i / 4f)
                        drawLine(PAGE_LINE.copy(alpha = 0.4f),
                            Offset(w * 0.12f + 4 * dp, ly), Offset(w * 0.12f + 28 * dp, ly), 0.6f * dp)
                    }
                }
                rotate(degrees = 15f, pivot = Offset(w * 0.82f, h * 0.30f)) {
                    drawRoundRect(PAGE_CREAM.copy(alpha = 0.5f),
                        Offset(w * 0.78f, h * 0.26f), Size(28 * dp, 20 * dp), CornerRadius(2 * dp))
                    for (i in 1..2) {
                        val ly = h * 0.26f + 20 * dp * (i / 3f)
                        drawLine(PAGE_LINE.copy(alpha = 0.3f),
                            Offset(w * 0.78f + 4 * dp, ly), Offset(w * 0.78f + 24 * dp, ly), 0.5f * dp)
                    }
                }
            }

            // ── TAGLINE ──
            Spacer(Modifier.height(32.dp))

            Text(
                S.tagline,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                color = TAG_DARK,
                letterSpacing = 0.4.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "MOCK MASTER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SUB_DARK.copy(alpha = 0.7f),
                letterSpacing = 4.sp
            )
        }
    }
}
