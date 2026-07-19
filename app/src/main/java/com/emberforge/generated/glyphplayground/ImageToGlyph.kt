package com.emberforge.generated.glyphplayground

import android.graphics.Bitmap
import android.graphics.Color

object ImageToGlyph {

    private const val G = GlyphLayout.GRID_SIZE

    fun convert(source: Bitmap, threshold: Float = 0.5f, invert: Boolean = false): Set<Int> {
        val scaled = Bitmap.createScaledBitmap(source, G, G, true)
        val result = mutableSetOf<Int>()

        for (row in 0 until G) {
            for (col in 0 until G) {
                val idx = row * G + col
                if (!GlyphLayout.isInsideCircle(idx)) continue

                val pixel = scaled.getPixel(col, row)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                val isLit = if (invert) luminance < threshold else luminance >= threshold
                if (isLit) result.add(idx)
            }
        }

        if (scaled != source) scaled.recycle()
        return result
    }
}
