package com.govtprep.app.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// BLOB SQUAD MASCOT — 3 Among-Us style blob characters
// Each reacts independently per state
// States: IDLE · EMAIL · PASSWORD · NAME · SUCCESS · ERROR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

enum class OwlState { IDLE, EMAIL, PASSWORD, NAME, SUCCESS, ERROR }

// Per-character config
private data class BlobConfig(
    val color: Color,
    val shadow: Color,
    val highlight: Color,
    val xOffset: Float,   // relative to center, in dp
    val scale: Float,
    val zOrder: Int       // paint order (0=back, 2=front)
)

private val BLOBS = listOf(
    BlobConfig(Color(0xFFEA580C), Color(0xFFC2410C), Color(0xFFFB923C), xOffset = -44f, scale = 1.00f, zOrder = 1),  // orange - left
    BlobConfig(Color(0xFF7C3AED), Color(0xFF5B21B6), Color(0xFF8B5CF6), xOffset =   4f, scale = 1.08f, zOrder = 2),  // purple - center/front
    BlobConfig(Color(0xFFD97706), Color(0xFFB45309), Color(0xFFFBBF24), xOffset =  52f, scale = 0.92f, zOrder = 0),  // yellow - right/back
)

@Composable
fun OwlMascot(
    state: OwlState,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // ── Idle float — staggered per blob ──
    val floatAnim = rememberInfiniteTransition(label = "float")
    val float0 by floatAnim.animateFloat(0f, -7f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f0")
    val float1 by floatAnim.animateFloat(0f, -9f, infiniteRepeatable(tween(2000, 300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f1")
    val float2 by floatAnim.animateFloat(0f, -6f, infiniteRepeatable(tween(1600, 600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f2")
    val floats = listOf(float0, float1, float2)

    // ── Bounce (excited states) ──
    val bounceAnim = rememberInfiniteTransition(label = "bounce")
    val bounce0 by bounceAnim.animateFloat(0f, -14f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b0")
    val bounce1 by bounceAnim.animateFloat(0f, -16f, infiniteRepeatable(tween(380, 120, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b1")
    val bounce2 by bounceAnim.animateFloat(0f, -12f, infiniteRepeatable(tween(450, 240, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b2")
    val bounces = listOf(bounce0, bounce1, bounce2)

    // ── Random blinks per blob ──
    val blink0 = rememberBlink(delay = 3200)
    val blink1 = rememberBlink(delay = 4100)
    val blink2 = rememberBlink(delay = 2800)
    val blinks = listOf(blink0, blink1, blink2)

    // ── Lean forward (email/name) ──
    val leanFwd by animateFloatAsState(
        targetValue = when (state) { OwlState.EMAIL, OwlState.NAME -> 1f else -> 0f },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 160f), label = "lean"
    )

    // ── Head tilt (curious) ──
    val tilt0 by animateFloatAsState(when (state) { OwlState.EMAIL -> -10f; OwlState.ERROR -> 5f; else -> 0f }, tween(350), label = "t0")
    val tilt1 by animateFloatAsState(when (state) { OwlState.EMAIL -> 8f;  OwlState.ERROR -> -6f; else -> 0f }, tween(350), label = "t1")
    val tilt2 by animateFloatAsState(when (state) { OwlState.EMAIL -> -6f; OwlState.ERROR -> 4f;  else -> 0f }, tween(350), label = "t2")
    val tilts = listOf(tilt0, tilt1, tilt2)

    // ── Pupil direction ──
    val pupilOffsets = when (state) {
        OwlState.EMAIL    -> listOf(Offset(-2f, 4f), Offset(0f, 5f), Offset(2f, 4f))    // look down at field
        OwlState.PASSWORD -> listOf(Offset(-8f, -2f), Offset(8f, -3f), Offset(-6f, -2f)) // look away in different dirs
        OwlState.SUCCESS  -> listOf(Offset(-2f,-4f), Offset(0f,-5f), Offset(2f,-4f))    // look up/happy
        OwlState.ERROR    -> listOf(Offset(3f,4f), Offset(0f,4f), Offset(-3f,4f))       // look down sad
        else              -> listOf(Offset(0f,0f), Offset(0f,0f), Offset(0f,0f))
    }
    val animPupils = pupilOffsets.mapIndexed { i, target ->
        val px by animateFloatAsState(target.x, tween(300), label = "px$i")
        val py by animateFloatAsState(target.y, tween(300), label = "py$i")
        Offset(px, py)
    }

    // ── Eye expressions ──
    // PASSWORD: blob 1 (center) covers eyes with hands, blobs 0 & 2 look away
    // Others: normal eyes
    val eyeScales = listOf(
        animateFloatAsState(if (state == OwlState.ERROR) 0.7f else 1.0f, tween(300), label = "es0").value,
        animateFloatAsState(if (state == OwlState.SUCCESS || state == OwlState.NAME) 1.3f else if (state == OwlState.ERROR) 0.7f else 1.0f, tween(300), label = "es1").value,
        animateFloatAsState(if (state == OwlState.ERROR) 0.7f else 1.0f, tween(300), label = "es2").value,
    )

    // PASSWORD: center blob raises hands over face
    val handRaise by animateFloatAsState(
        targetValue = if (state == OwlState.PASSWORD) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 180f), label = "hands"
    )

    // ── Mouth curve ──
    val mouths = listOf(
        animateFloatAsState(when (state) { OwlState.SUCCESS, OwlState.NAME -> 1f; OwlState.ERROR -> -1f; OwlState.PASSWORD -> -0.4f; else -> 0.3f }, tween(350), label = "m0").value,
        animateFloatAsState(when (state) { OwlState.SUCCESS, OwlState.NAME -> 1f; OwlState.ERROR -> -1f; OwlState.PASSWORD -> -0.5f; else -> 0.4f }, tween(350), label = "m1").value,
        animateFloatAsState(when (state) { OwlState.SUCCESS, OwlState.NAME -> 1f; OwlState.ERROR -> -1f; OwlState.PASSWORD -> -0.3f; else -> 0.3f }, tween(350), label = "m2").value,
    )

    // ── Squash/stretch ──
    val squash = listOf(
        animateFloatAsState(when (state) { OwlState.SUCCESS, OwlState.NAME -> 0.88f; else -> 1.0f }, tween(300), label = "sq0").value,
        animateFloatAsState(when (state) { OwlState.SUCCESS, OwlState.NAME -> 0.85f; else -> 1.0f }, tween(300), label = "sq1").value,
        animateFloatAsState(when (state) { OwlState.SUCCESS, OwlState.NAME -> 0.90f; else -> 1.0f }, tween(300), label = "sq2").value,
    )

    Canvas(modifier = modifier.size(220.dp, 170.dp)) {
        val w  = size.width
        val h  = size.height
        val cx = w / 2f
        val d  = density

        // Draw in z-order
        val drawOrder = BLOBS.sortedBy { it.zOrder }
        drawOrder.forEach { blob ->
            val i = BLOBS.indexOf(blob)
            val isExcited = (state == OwlState.SUCCESS || state == OwlState.NAME)
            val bobY = if (isExcited) bounces[i] else floats[i]

            drawBlob(
                cx      = cx + blob.xOffset * d,
                baseY   = h * 0.75f + bobY * d,
                scale   = blob.scale,
                d       = d,
                color   = blob.color,
                shadow  = blob.shadow,
                highlight = blob.highlight,
                blink   = blinks[i],
                tiltDeg = tilts[i],
                pupilOff = animPupils[i] * d,
                eyeScale = eyeScales[i],
                mouthCurve = mouths[i],
                squashY  = squash[i],
                leanFwd  = if (i == 1) leanFwd else leanFwd * 0.6f,
                showHands = handRaise,
                isCenter  = i == 1,
                state     = state
            )
        }
    }
}

// ━━━ SINGLE BLOB ━━━
private fun DrawScope.drawBlob(
    cx: Float, baseY: Float, scale: Float, d: Float,
    color: Color, shadow: Color, highlight: Color,
    blink: Float, tiltDeg: Float, pupilOff: Offset, eyeScale: Float,
    mouthCurve: Float, squashY: Float, leanFwd: Float,
    showHands: Float, isCenter: Boolean, state: OwlState
) {
    val bW  = 52f * d * scale        // body width
    val bH  = 68f * d * scale * squashY  // body height (squash)
    val topX = cx
    val topY = baseY - bH

    // ── Lean forward: shift x slightly, tilt ──
    val leanX = leanFwd * (if (cx < 0) 8f else if (cx > 100*d) -8f else 0f) * d
    val leanTilt = leanFwd * (if (isCenter) 0f else if (cx < 400f) 8f else -8f)

    withTransform({ translate(leanX, 0f); rotate(tiltDeg + leanTilt, Offset(cx, baseY)) }) {

        // ── SHADOW ──
        drawOval(shadow.copy(alpha = 0.35f),
            Offset(cx - bW * 0.55f + 3*d, topY - bH * 0.02f + 4*d),
            Size(bW * 1.1f, bH * 1.04f))

        // ── BODY (bean shape via path) ──
        val bodyPath = buildBeanPath(cx, topY, bW, bH)
        drawPath(bodyPath, shadow.copy(alpha = 0.6f))
        // Offset body slightly up for 3d feel
        val bodyPath2 = buildBeanPath(cx, topY - 2*d, bW, bH)
        drawPath(bodyPath2, color)

        // ── BODY HIGHLIGHT (left edge sheen) ──
        val gloss = Path().apply {
            moveTo(cx - bW * 0.35f, topY + bH * 0.12f)
            cubicTo(
                cx - bW * 0.45f, topY + bH * 0.28f,
                cx - bW * 0.42f, topY + bH * 0.48f,
                cx - bW * 0.30f, topY + bH * 0.55f
            )
        }
        drawPath(gloss, highlight.copy(alpha = 0.35f),
            style = Stroke(width = 6f * d * scale, cap = StrokeCap.Round))

        // ── VISOR (face window) ──
        val visW = bW * 0.72f
        val visH = bH * 0.36f
        val visX = cx - visW / 2f
        val visY = topY + bH * 0.08f
        // Visor shadow
        drawRoundRect(Color.Black.copy(alpha = 0.55f),
            Offset(visX + 2*d, visY + 2*d), Size(visW, visH),
            CornerRadius(visW * 0.3f))
        // Visor fill (dark glass)
        drawRoundRect(Color(0xFF111111).copy(alpha = 0.88f),
            Offset(visX, visY), Size(visW, visH), CornerRadius(visW * 0.3f))
        // Visor glass sheen
        drawRoundRect(Color.White.copy(alpha = 0.10f),
            Offset(visX + 3*d, visY + 2*d), Size(visW * 0.55f, visH * 0.45f),
            CornerRadius(visW * 0.25f))

        // ── EYES inside visor ──
        val eyeSep  = visW * 0.26f
        val eyeCY   = visY + visH * 0.52f
        val eyeR    = visH * 0.28f * eyeScale.coerceIn(0.6f, 1.5f)

        if (showHands < 0.5f || !isCenter) {
            // Normal eyes
            val leftEye  = Offset(cx - eyeSep, eyeCY)
            val rightEye = Offset(cx + eyeSep, eyeCY)

            drawEyePair(leftEye, rightEye, eyeR, d, blink, pupilOff, mouthCurve, state)
        }

        // ── MOUTH (below visor) ──
        val mouthY = visY + visH + 7f * d * scale
        val mouthW = bW * 0.28f
        val mouthPath = Path().apply {
            moveTo(cx - mouthW, mouthY)
            quadraticBezierTo(cx, mouthY + mouthCurve * 7f * d * scale, cx + mouthW, mouthY)
        }
        drawPath(mouthPath, highlight.copy(alpha = 0.75f),
            style = Stroke(width = 2.8f * d * scale, cap = StrokeCap.Round))

        // ── BACKPACK (Among Us style detail) ──
        val bpX = cx + bW * 0.42f
        val bpY = topY + bH * 0.28f
        val bpW = bW * 0.22f
        val bpH = bH * 0.35f
        drawRoundRect(shadow, Offset(bpX + 1.5f*d, bpY + 1.5f*d), Size(bpW, bpH), CornerRadius(bpW*0.35f))
        drawRoundRect(shadow.copy(alpha = 0.85f),  Offset(bpX, bpY),           Size(bpW, bpH), CornerRadius(bpW*0.35f))

        // ── HANDS (raised for password state on center blob) ──
        if (isCenter && showHands > 0.01f) {
            drawRaisedHands(cx, topY, bH, bW, d, scale, color, shadow, showHands)
        }
    }
}

// ━━━ EYE PAIR ━━━
private fun DrawScope.drawEyePair(
    leftC: Offset, rightC: Offset, eyeR: Float, d: Float,
    blink: Float, pupilOff: Offset, mouthCurve: Float, state: OwlState
) {
    listOf(leftC, rightC).forEachIndexed { i, center ->
        val pOff = Offset(
            (if (i == 0) pupilOff.x else -pupilOff.x).coerceIn(-eyeR*0.45f, eyeR*0.45f),
            pupilOff.y.coerceIn(-eyeR*0.45f, eyeR*0.45f)
        )

        val lidH = eyeR * 2f * (1f - blink.coerceIn(0f, 1f))

        // White sclera
        drawCircle(Color.White, eyeR, center)

        // Iris
        val irisCol = when (state) {
            OwlState.SUCCESS -> Color(0xFF4ADE80)
            OwlState.ERROR   -> Color(0xFFF87171)
            else             -> Color(0xFF60A5FA)
        }
        drawCircle(irisCol, eyeR * 0.68f, center + pOff)

        // Pupil
        drawCircle(Color(0xFF111111), eyeR * 0.38f, center + pOff)

        // Shine
        drawCircle(Color.White.copy(alpha = 0.9f), eyeR * 0.14f,
            center + pOff + Offset(-eyeR * 0.20f, -eyeR * 0.20f))

        // Eyelid (blink)
        if (lidH > 0.5f) {
            drawArc(Color(0xFF111111).copy(alpha = 0.88f),
                startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(center.x - eyeR, center.y - eyeR),
                size = Size(eyeR * 2, eyeR * 2))
        }

        // Eyebrow (happy = raised, sad = angled down inner)
        val browY  = center.y - eyeR * 1.5f
        val browW  = eyeR * 1.3f
        val browLift = when (state) {
            OwlState.SUCCESS, OwlState.NAME -> -eyeR * 0.4f
            OwlState.ERROR                  ->  eyeR * 0.2f
            else -> 0f
        }
        val browSlant = when (state) {
            OwlState.ERROR -> if (i == 0) eyeR * 0.3f else -eyeR * 0.3f
            else -> 0f
        }
        drawLine(Color.White.copy(alpha = 0.70f),
            start = Offset(center.x - browW, browY + browLift + browSlant),
            end   = Offset(center.x + browW, browY + browLift - browSlant),
            strokeWidth = 2.5f * d, cap = StrokeCap.Round)
    }
}

// ━━━ RAISED HANDS (password state) ━━━
private fun DrawScope.drawRaisedHands(
    cx: Float, topY: Float, bH: Float, bW: Float, d: Float,
    scale: Float, color: Color, shadow: Color, t: Float
) {
    // Both arms swing up to cover the visor
    val armLen  = 28f * d * scale
    val handR   = 9f  * d * scale
    val baseY   = topY + bH * 0.50f

    // Left arm
    val lArmEndX = lerp(cx - bW * 0.55f, cx - bW * 0.15f, t)
    val lArmEndY = lerp(baseY,            topY + bH * 0.20f, t)
    drawLine(shadow.copy(alpha = 0.5f),
        Offset(cx - bW * 0.38f, baseY + 2*d), Offset(lArmEndX + 2*d, lArmEndY + 2*d),
        strokeWidth = 9f * d * scale, cap = StrokeCap.Round)
    drawLine(color,
        Offset(cx - bW * 0.38f, baseY), Offset(lArmEndX, lArmEndY),
        strokeWidth = 9f * d * scale, cap = StrokeCap.Round)
    drawCircle(shadow.copy(alpha = 0.5f), handR, Offset(lArmEndX + 2*d, lArmEndY + 2*d))
    drawCircle(color, handR, Offset(lArmEndX, lArmEndY))

    // Right arm
    val rArmEndX = lerp(cx + bW * 0.55f, cx + bW * 0.15f, t)
    val rArmEndY = lerp(baseY,            topY + bH * 0.20f, t)
    drawLine(shadow.copy(alpha = 0.5f),
        Offset(cx + bW * 0.38f, baseY + 2*d), Offset(rArmEndX + 2*d, rArmEndY + 2*d),
        strokeWidth = 9f * d * scale, cap = StrokeCap.Round)
    drawLine(color,
        Offset(cx + bW * 0.38f, baseY), Offset(rArmEndX, rArmEndY),
        strokeWidth = 9f * d * scale, cap = StrokeCap.Round)
    drawCircle(shadow.copy(alpha = 0.5f), handR, Offset(rArmEndX + 2*d, rArmEndY + 2*d))
    drawCircle(color, handR, Offset(rArmEndX, rArmEndY))

    // Fingers on hands
    for (f in -1..1) {
        val fOff = f * 5f * d * scale
        drawCircle(shadow.copy(alpha = 0.4f), handR * 0.38f, Offset(lArmEndX + fOff + 1.5f*d, lArmEndY - handR*0.5f + 1.5f*d))
        drawCircle(color.copy(alpha = 0.85f), handR * 0.38f, Offset(lArmEndX + fOff, lArmEndY - handR*0.5f))
        drawCircle(shadow.copy(alpha = 0.4f), handR * 0.38f, Offset(rArmEndX + fOff + 1.5f*d, rArmEndY - handR*0.5f + 1.5f*d))
        drawCircle(color.copy(alpha = 0.85f), handR * 0.38f, Offset(rArmEndX + fOff, rArmEndY - handR*0.5f))
    }
}

// ━━━ BEAN PATH ━━━
private fun buildBeanPath(cx: Float, topY: Float, bW: Float, bH: Float): Path {
    val left  = cx - bW / 2f
    val right = cx + bW / 2f
    val bottom = topY + bH
    val topR  = bW * 0.50f
    val botR  = bW * 0.45f
    return Path().apply {
        moveTo(cx, topY)
        cubicTo(right + bW*0.10f, topY,          right + bW*0.12f, topY + bH*0.35f, right, bottom - bH*0.15f)
        cubicTo(right - bW*0.05f, bottom + bH*0.04f, left + bW*0.05f, bottom + bH*0.04f, left, bottom - bH*0.15f)
        cubicTo(left - bW*0.12f, topY + bH*0.35f, left - bW*0.10f, topY, cx, topY)
        close()
    }
}

// ━━━ BLINK STATE ━━━
@Composable
private fun rememberBlink(delay: Long): Float {
    var blinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(delay + (Math.random() * 2000).toLong())
            blinking = true; kotlinx.coroutines.delay(100); blinking = false
        }
    }
    return animateFloatAsState(
        targetValue = if (blinking) 0f else 1f,
        animationSpec = tween(if (blinking) 70 else 120),
        label = "blink"
    ).value
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
private operator fun Offset.times(scalar: Float) = Offset(x * scalar, y * scalar)
