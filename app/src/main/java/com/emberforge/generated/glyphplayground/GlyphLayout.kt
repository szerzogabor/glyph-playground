package com.emberforge.generated.glyphplayground

data class LedPosition(
    val index: Int,
    val x: Float,
    val y: Float,
    val type: LedType = LedType.MATRIX_DOT
)

enum class LedType {
    MATRIX_DOT,
    STRIP,
    CAMERA_RING,
    ACCENT
}

object GlyphLayout {

    private const val MATRIX_COLS = 3
    private const val MATRIX_ROWS = 11

    val leds: List<LedPosition> = buildList {
        var idx = 0

        // Camera ring (8 LEDs around the camera module at top-left)
        val cx = 0.27f; val cy = 0.09f
        val positions = listOf(
            cx to 0.045f,          // top
            cx + 0.08f to 0.06f,   // top-right
            cx + 0.11f to cy,      // right
            cx + 0.08f to 0.12f,   // bottom-right
            cx to 0.135f,          // bottom
            cx - 0.08f to 0.12f,   // bottom-left
            cx - 0.11f to cy,      // left
            cx - 0.08f to 0.06f    // top-left
        )
        for ((px, py) in positions) {
            add(LedPosition(idx++, px, py, LedType.CAMERA_RING))
        }

        // Top horizontal strip (6 LEDs from camera area to the right)
        for (i in 0 until 6) {
            add(LedPosition(idx++, 0.48f + i * 0.075f, 0.055f, LedType.STRIP))
        }

        // Diagonal strip running down-left (4 LEDs)
        for (i in 0 until 4) {
            add(LedPosition(idx++, 0.82f - i * 0.08f, 0.11f + i * 0.05f, LedType.STRIP))
        }

        // Left vertical strip (3 LEDs)
        for (i in 0 until 3) {
            add(LedPosition(idx++, 0.18f, 0.27f + i * 0.05f, LedType.STRIP))
        }

        // Glyph Matrix: 3 columns x 11 rows = 33 dots
        val matrixColX = floatArrayOf(0.35f, 0.50f, 0.65f)
        val matrixStartY = 0.44f
        val matrixRowSpacing = 0.042f
        for (row in 0 until MATRIX_ROWS) {
            for (col in 0 until MATRIX_COLS) {
                add(LedPosition(idx++, matrixColX[col], matrixStartY + row * matrixRowSpacing, LedType.MATRIX_DOT))
            }
        }

        // Bottom accent dot
        add(LedPosition(idx, 0.50f, 0.93f, LedType.ACCENT))
    }

    val matrixStartIndex = 21
    val matrixEndIndex = matrixStartIndex + MATRIX_COLS * MATRIX_ROWS - 1
    val totalLeds get() = leds.size
}
