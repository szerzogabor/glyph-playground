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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
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
    return row * G + col
}

@Composable
fun GlyphMatrixCanvas(
    activeLeds: Set<Int>,
    onToggle: (Int) -> Unit,
    onDraw: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
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
        drawGrid(cellSize, gridOffsetY, activeLeds)
    }
}

@Composable
fun GlyphMatrixPreview(
    activeLeds: Set<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val side = minOf(size.width, size.height)
        val cellSize = side / G
        val offsetX = (size.width - side) / 2f
        val offsetY = (size.height - side) / 2f
        drawGridAt(cellSize, offsetX, offsetY, activeLeds)
    }
}

private fun DrawScope.drawGrid(cellSize: Float, gridOffsetY: Float, activeLeds: Set<Int>) {
    drawGridAt(cellSize, 0f, gridOffsetY, activeLeds)
}

private fun DrawScope.drawGridAt(cellSize: Float, offsetX: Float, offsetY: Float, activeLeds: Set<Int>) {
    val gap = (cellSize * 0.08f).coerceAtLeast(0.5f)
    val cr = CornerRadius(gap)

    // Grid background
    drawRoundRect(
        color = GridBg,
        topLeft = Offset(offsetX, offsetY),
        size = Size(cellSize * G, cellSize * G),
        cornerRadius = CornerRadius(cellSize * 0.2f)
    )

    // Glow pass for active LEDs
    for (idx in activeLeds) {
        val row = idx / G
        val col = idx % G
        if (row !in 0 until G || col !in 0 until G) continue
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize
        drawRoundRect(
            color = GlowColor,
            topLeft = Offset(x - gap, y - gap),
            size = Size(cellSize + 2 * gap, cellSize + 2 * gap),
            cornerRadius = CornerRadius(gap * 2)
        )
    }

    // Cell pass
    for (row in 0 until G) {
        for (col in 0 until G) {
            val idx = row * G + col
            val isOn = idx in activeLeds
            val x = offsetX + col * cellSize + gap
            val y = offsetY + row * cellSize + gap
            val s = cellSize - 2 * gap

            drawRoundRect(
                color = if (isOn) LedOn else LedOff,
                topLeft = Offset(x, y),
                size = Size(s, s),
                cornerRadius = cr
            )
        }
    }
}
