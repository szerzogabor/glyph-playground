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
                val luminance = pixelLuminance(pixel)

                val isLit = if (invert) luminance < threshold else luminance >= threshold
                if (isLit) result.add(idx)
            }
        }

        if (scaled != source) scaled.recycle()
        return result
    }

    fun convertWithBrightness(source: Bitmap, invert: Boolean = false): Map<Int, Int> {
        val scaled = Bitmap.createScaledBitmap(source, G, G, true)
        val result = mutableMapOf<Int, Int>()

        for (row in 0 until G) {
            for (col in 0 until G) {
                val idx = row * G + col
                if (!GlyphLayout.isInsideCircle(idx)) continue

                val pixel = scaled.getPixel(col, row)
                val luminance = pixelLuminance(pixel)
                val value = if (invert) 1f - luminance else luminance
                val brightness = (value * GlyphController.MAX_BRIGHTNESS).toInt()
                    .coerceIn(0, GlyphController.MAX_BRIGHTNESS)
                if (brightness > 0) result[idx] = brightness
            }
        }

        if (scaled != source) scaled.recycle()
        return result
    }

    private fun pixelLuminance(pixel: Int): Float {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }
}
