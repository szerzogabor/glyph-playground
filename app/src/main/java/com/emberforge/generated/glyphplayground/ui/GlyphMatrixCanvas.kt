package com.emberforge.generated.glyphplayground.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.emberforge.generated.glyphplayground.GlyphLayout
import com.emberforge.generated.glyphplayground.LedType

private val PhoneBody = Color(0xFF0A0A0A)
private val PhoneBorder = Color(0xFF2A2A2A)
private val CameraBody = Color(0xFF111111)
private val CameraRing = Color(0xFF333333)
private val LedOff = Color(0xFF1C1C1C)
private val LedOn = Color(0xFFFFFFFF)
private val GlowColor = Color(0x40FFFFFF)

private const val PHONE_ASPECT = 2.16f
private const val PHONE_CORNER_FRAC = 0.08f
private const val LED_RADIUS_FRAC = 0.018f
private const val HIT_RADIUS_FRAC = 0.04f
private const val CAMERA_RADIUS_FRAC = 0.14f

data class PhoneMetrics(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun ledCenter(x: Float, y: Float) = Offset(left + x * width, top + y * height)
    val ledRadius get() = width * LED_RADIUS_FRAC
    val hitRadius get() = width * HIT_RADIUS_FRAC
}

private fun computeMetrics(canvasW: Float, canvasH: Float): PhoneMetrics {
    val pad = canvasW * 0.06f
    val availW = canvasW - 2 * pad
    val availH = canvasH - 2 * pad
    val phoneW: Float
    val phoneH: Float
    if (availW * PHONE_ASPECT <= availH) {
        phoneW = availW; phoneH = availW * PHONE_ASPECT
    } else {
        phoneH = availH; phoneW = availH / PHONE_ASPECT
    }
    return PhoneMetrics(
        left = (canvasW - phoneW) / 2f,
        top = (canvasH - phoneH) / 2f,
        width = phoneW,
        height = phoneH
    )
}

private fun findHitLed(tap: Offset, m: PhoneMetrics): Int? {
    var best = -1
    var bestDist = Float.MAX_VALUE
    for (led in GlyphLayout.leds) {
        val c = m.ledCenter(led.x, led.y)
        val d = (tap - c).getDistance()
        if (d < m.hitRadius && d < bestDist) {
            bestDist = d; best = led.index
        }
    }
    return if (best >= 0) best else null
}

@Composable
fun GlyphMatrixCanvas(
    activeLeds: Set<Int>,
    onToggle: (Int) -> Unit,
    onDraw: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var metrics by remember { mutableStateOf<PhoneMetrics?>(null) }
    var dragMode by remember { mutableIntStateOf(0) }
    var lastDragHit by remember { mutableIntStateOf(-1) }

    val currentActiveLeds by rememberUpdatedState(activeLeds)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnDraw by rememberUpdatedState(onDraw)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    metrics?.let { m ->
                        findHitLed(offset, m)?.let(currentOnToggle)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        metrics?.let { m ->
                            val hit = findHitLed(offset, m)
                            if (hit != null) {
                                dragMode = if (hit in currentActiveLeds) -1 else 1
                                lastDragHit = hit
                                currentOnDraw(hit, dragMode == 1)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        metrics?.let { m ->
                            val hit = findHitLed(change.position, m)
                            if (hit != null && hit != lastDragHit) {
                                lastDragHit = hit
                                currentOnDraw(hit, dragMode == 1)
                            }
                        }
                    },
                    onDragEnd = { dragMode = 0; lastDragHit = -1 },
                    onDragCancel = { dragMode = 0; lastDragHit = -1 }
                )
            }
    ) {
        val m = computeMetrics(size.width, size.height)
        metrics = m
        drawPhone(m)
        drawLeds(m, activeLeds)
    }
}

@Composable
fun GlyphMatrixPreview(
    activeLeds: Set<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val m = computeMetrics(size.width, size.height)
        drawPhone(m)
        drawLeds(m, activeLeds)
    }
}

private fun DrawScope.drawPhone(m: PhoneMetrics) {
    val cr = CornerRadius(m.width * PHONE_CORNER_FRAC)

    // Phone body
    drawRoundRect(
        color = PhoneBody,
        topLeft = Offset(m.left, m.top),
        size = Size(m.width, m.height),
        cornerRadius = cr
    )
    drawRoundRect(
        color = PhoneBorder,
        topLeft = Offset(m.left, m.top),
        size = Size(m.width, m.height),
        cornerRadius = cr,
        style = Stroke(width = 1.5f)
    )

    // Camera module
    val camCenter = m.ledCenter(0.27f, 0.09f)
    val camRadius = m.width * CAMERA_RADIUS_FRAC
    drawCircle(color = CameraBody, radius = camRadius, center = camCenter)
    drawCircle(color = CameraRing, radius = camRadius, center = camCenter, style = Stroke(1.5f))

    // Camera lens
    drawCircle(color = Color(0xFF050505), radius = camRadius * 0.45f, center = camCenter)
    drawCircle(color = CameraRing, radius = camRadius * 0.45f, center = camCenter, style = Stroke(0.8f))
}

private fun DrawScope.drawLeds(m: PhoneMetrics, activeLeds: Set<Int>) {
    val r = m.ledRadius

    for (led in GlyphLayout.leds) {
        val center = m.ledCenter(led.x, led.y)
        val isOn = led.index in activeLeds

        val sizeMultiplier = when (led.type) {
            LedType.CAMERA_RING -> 0.7f
            LedType.STRIP -> 0.65f
            LedType.ACCENT -> 1.1f
            LedType.MATRIX_DOT -> 1f
        }

        if (isOn) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlowColor, Color.Transparent),
                    center = center,
                    radius = r * sizeMultiplier * 4f
                ),
                radius = r * sizeMultiplier * 4f,
                center = center
            )
        }

        drawCircle(
            color = if (isOn) LedOn else LedOff,
            radius = r * sizeMultiplier,
            center = center
        )

        if (!isOn) {
            drawCircle(
                color = PhoneBorder,
                radius = r * sizeMultiplier,
                center = center,
                style = Stroke(0.5f)
            )
        }
    }
}
