package com.emberforge.generated.glyphplayground.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.emberforge.generated.glyphplayground.GlyphController
import com.emberforge.generated.glyphplayground.GlyphLayout

private const val G = GlyphLayout.GRID_SIZE

private val GridBg = Color(0xFF080808)
private val LedOff = Color(0xFF1A1A1A)
private val LedOn = Color(0xFFFFFFFF)
private val GlowColor = Color(0x18FFFFFF)

private fun hitIndex(offset: Offset, cellSize: Float, gridOffset: Offset): Int? {
    val col = ((offset.x - gridOffset.x) / cellSize).toInt()
    val row = ((offset.y - gridOffset.y) / cellSize).toInt()
    if (col !in 0 until G || row !in 0 until G) return null
    val idx = row * G + col
    if (!GlyphLayout.isInsideCircle(idx)) return null
    return idx
}

@Composable
fun GlyphMatrixCanvas(
    activeLeds: Set<Int>,
    onToggle: (Int) -> Unit,
    onDraw: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    ledBrightness: Map<Int, Int> = emptyMap()
) {
    var dragMode by remember { mutableIntStateOf(0) }
    var lastDragHit by remember { mutableIntStateOf(-1) }

    val currentActiveLeds by rememberUpdatedState(activeLeds)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnDraw by rememberUpdatedState(onDraw)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellSize = size.width.toFloat() / G
                    val gridOffset = Offset(0f, (size.height - size.width) / 2f)
                    hitIndex(offset, cellSize, gridOffset)?.let(currentOnToggle)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val cellSize = size.width.toFloat() / G
                        val gridOffset = Offset(0f, (size.height - size.width) / 2f)
                        val hit = hitIndex(offset, cellSize, gridOffset)
                        if (hit != null) {
                            dragMode = if (hit in currentActiveLeds) -1 else 1
                            lastDragHit = hit
                            currentOnDraw(hit, dragMode == 1)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val cellSize = size.width.toFloat() / G
                        val gridOffset = Offset(0f, (size.height - size.width) / 2f)
                        val hit = hitIndex(change.position, cellSize, gridOffset)
                        if (hit != null && hit != lastDragHit) {
                            lastDragHit = hit
                            currentOnDraw(hit, dragMode == 1)
                        }
                    },
                    onDragEnd = { dragMode = 0; lastDragHit = -1 },
                    onDragCancel = { dragMode = 0; lastDragHit = -1 }
                )
            }
    ) {
        val cellSize = size.width / G
        val gridOffsetY = (size.height - size.width) / 2f
        drawGrid(cellSize, gridOffsetY, activeLeds, ledBrightness)
    }
}

@Composable
fun GlyphMatrixPreview(
    activeLeds: Set<Int>,
    modifier: Modifier = Modifier,
    ledBrightness: Map<Int, Int> = emptyMap()
) {
    Canvas(modifier = modifier) {
        val side = minOf(size.width, size.height)
        val cellSize = side / G
        val offsetX = (size.width - side) / 2f
        val offsetY = (size.height - side) / 2f
        drawGridAt(cellSize, offsetX, offsetY, activeLeds, ledBrightness)
    }
}

private fun DrawScope.drawGrid(cellSize: Float, gridOffsetY: Float, activeLeds: Set<Int>, ledBrightness: Map<Int, Int> = emptyMap()) {
    drawGridAt(cellSize, 0f, gridOffsetY, activeLeds, ledBrightness)
}

private fun DrawScope.drawGridAt(cellSize: Float, offsetX: Float, offsetY: Float, activeLeds: Set<Int>, ledBrightness: Map<Int, Int> = emptyMap()) {
    val gap = (cellSize * 0.08f).coerceAtLeast(0.5f)
    val gridSide = cellSize * G
    val radius = gridSide / 2f
    val centerX = offsetX + radius
    val centerY = offsetY + radius

    val circlePath = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                left = centerX - radius,
                top = centerY - radius,
                right = centerX + radius,
                bottom = centerY + radius
            )
        )
    }

    drawCircle(
        color = GridBg,
        radius = radius,
        center = Offset(centerX, centerY)
    )

    val useGrayscale = ledBrightness.isNotEmpty()

    clipPath(circlePath) {
        if (useGrayscale) {
            for ((idx, brightness) in ledBrightness) {
                if (!GlyphLayout.isInsideCircle(idx)) continue
                val fraction = brightness.toFloat() / GlyphController.MAX_BRIGHTNESS
                val row = idx / G
                val col = idx % G
                val x = offsetX + col * cellSize
                val y = offsetY + row * cellSize
                drawCircle(
                    color = GlowColor.copy(alpha = GlowColor.alpha * fraction),
                    radius = cellSize * 0.8f,
                    center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                )
            }
        } else {
            for (idx in activeLeds) {
                if (!GlyphLayout.isInsideCircle(idx)) continue
                val row = idx / G
                val col = idx % G
                val x = offsetX + col * cellSize
                val y = offsetY + row * cellSize
                drawCircle(
                    color = GlowColor,
                    radius = cellSize * 0.8f,
                    center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                )
            }
        }

        for (row in 0 until G) {
            for (col in 0 until G) {
                val idx = row * G + col
                if (!GlyphLayout.isInsideCircle(idx)) continue

                val x = offsetX + col * cellSize + gap
                val y = offsetY + row * cellSize + gap
                val s = cellSize - 2 * gap

                val color = if (useGrayscale) {
                    val brightness = ledBrightness.getOrDefault(idx, 0)
                    val fraction = brightness.toFloat() / GlyphController.MAX_BRIGHTNESS
                    lerp(LedOff, LedOn, fraction)
                } else {
                    if (idx in activeLeds) LedOn else LedOff
                }

                drawCircle(
                    color = color,
                    radius = s / 2f,
                    center = Offset(x + s / 2f, y + s / 2f)
                )
            }
        }
    }
}
