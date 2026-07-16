package com.emberforge.generated.glyphplayground

import kotlin.math.sqrt

object GlyphLayout {
    const val GRID_SIZE = 25
    const val TOTAL_LEDS = GRID_SIZE * GRID_SIZE

    private const val CENTER = (GRID_SIZE - 1) / 2f
    private const val RADIUS = GRID_SIZE / 2f

    val VALID_INDICES: Set<Int> = buildSet {
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val dx = col - CENTER
                val dy = row - CENTER
                if (sqrt(dx * dx + dy * dy) <= RADIUS) {
                    add(row * GRID_SIZE + col)
                }
            }
        }
    }

    val VALID_LED_COUNT = VALID_INDICES.size

    fun rowOf(index: Int) = index / GRID_SIZE
    fun colOf(index: Int) = index % GRID_SIZE
    fun indexOf(row: Int, col: Int) = row * GRID_SIZE + col
    fun isInsideCircle(index: Int) = index in VALID_INDICES
}
