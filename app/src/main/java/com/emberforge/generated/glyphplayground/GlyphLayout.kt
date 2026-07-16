package com.emberforge.generated.glyphplayground

object GlyphLayout {
    const val GRID_SIZE = 25
    const val TOTAL_LEDS = GRID_SIZE * GRID_SIZE

    fun rowOf(index: Int) = index / GRID_SIZE
    fun colOf(index: Int) = index % GRID_SIZE
    fun indexOf(row: Int, col: Int) = row * GRID_SIZE + col
}
