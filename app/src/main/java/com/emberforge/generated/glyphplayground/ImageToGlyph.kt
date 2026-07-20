package com.emberforge.generated.glyphplayground

import android.graphics.Bitmap
import kotlin.math.min

object ImageToGlyph {

    private const val G = GlyphLayout.GRID_SIZE

    // Each glyph cell averages an 8x8 block of the working bitmap, so one
    // stray source pixel can't flip a whole LED.
    private const val SUPERSAMPLE = 8
    private const val WORK_SIZE = G * SUPERSAMPLE

    // Cells dimmer than this (after contrast stretch) stay off — keeps
    // anti-aliasing and sensor noise from lighting the whole disc.
    private const val NOISE_FLOOR = 0.08f

    // Cells that are mostly transparent in the source stay off regardless
    // of invert, so PNG logos keep their background dark.
    private const val ALPHA_CUTOFF = 0.5f

    fun convert(source: Bitmap, threshold: Float = 0.5f, invert: Boolean = false): Set<Int> {
        val cells = sampleCells(source)
        val result = mutableSetOf<Int>()
        for (idx in 0 until G * G) {
            if (!GlyphLayout.isInsideCircle(idx)) continue
            if (cells.alpha[idx] < ALPHA_CUTOFF) continue
            val lum = cells.luminance[idx]
            val isLit = if (invert) lum < threshold else lum >= threshold
            if (isLit) result.add(idx)
        }
        return result
    }

    fun convertWithBrightness(source: Bitmap, invert: Boolean = false): Map<Int, Int> {
        val cells = sampleCells(source)
        val result = mutableMapOf<Int, Int>()
        for (idx in 0 until G * G) {
            if (!GlyphLayout.isInsideCircle(idx)) continue
            if (cells.alpha[idx] < ALPHA_CUTOFF) continue
            val value = if (invert) 1f - cells.luminance[idx] else cells.luminance[idx]
            if (value < NOISE_FLOOR) continue
            val brightness = (value * GlyphController.MAX_BRIGHTNESS).toInt()
                .coerceIn(0, GlyphController.MAX_BRIGHTNESS)
            if (brightness > 0) result[idx] = brightness
        }
        return result
    }

    private class CellGrid(val luminance: FloatArray, val alpha: FloatArray)

    /**
     * Center-crops the source to a square (matching the on-screen preview's
     * ContentScale.Crop), downsamples it to [WORK_SIZE], then box-averages
     * each [SUPERSAMPLE]-sized block into one cell. Luminance is contrast
     * stretched over the visible in-circle cells so low-contrast pictures
     * still use the full brightness range.
     */
    private fun sampleCells(source: Bitmap): CellGrid {
        val work = renderWorkBitmap(source)
        val pixels = IntArray(WORK_SIZE * WORK_SIZE)
        work.getPixels(pixels, 0, WORK_SIZE, 0, 0, WORK_SIZE, WORK_SIZE)
        if (work != source) work.recycle()

        val luminance = FloatArray(G * G)
        val alpha = FloatArray(G * G)
        val samplesPerCell = SUPERSAMPLE * SUPERSAMPLE

        for (row in 0 until G) {
            for (col in 0 until G) {
                var lumSum = 0f
                var alphaSum = 0f
                for (dy in 0 until SUPERSAMPLE) {
                    val base = (row * SUPERSAMPLE + dy) * WORK_SIZE + col * SUPERSAMPLE
                    for (dx in 0 until SUPERSAMPLE) {
                        val pixel = pixels[base + dx]
                        val a = (pixel ushr 24 and 0xFF) / 255f
                        val r = pixel ushr 16 and 0xFF
                        val g = pixel ushr 8 and 0xFF
                        val b = pixel and 0xFF
                        lumSum += (0.299f * r + 0.587f * g + 0.114f * b) / 255f * a
                        alphaSum += a
                    }
                }
                val idx = row * G + col
                luminance[idx] = lumSum / samplesPerCell
                alpha[idx] = alphaSum / samplesPerCell
            }
        }

        normalizeContrast(luminance, alpha)
        return CellGrid(luminance, alpha)
    }

    /** Stretches luminance of visible in-circle cells to span [0, 1]. */
    private fun normalizeContrast(luminance: FloatArray, alpha: FloatArray) {
        var minLum = Float.MAX_VALUE
        var maxLum = -Float.MAX_VALUE
        for (idx in luminance.indices) {
            if (!GlyphLayout.isInsideCircle(idx) || alpha[idx] < ALPHA_CUTOFF) continue
            val lum = luminance[idx]
            if (lum < minLum) minLum = lum
            if (lum > maxLum) maxLum = lum
        }
        val range = maxLum - minLum
        if (range < 0.05f) return
        for (idx in luminance.indices) {
            luminance[idx] = ((luminance[idx] - minLum) / range).coerceIn(0f, 1f)
        }
    }

    /**
     * Produces a [WORK_SIZE] square bitmap: center-crop, then repeated
     * halving before the final scale so large photos downsample without
     * aliasing.
     */
    private fun renderWorkBitmap(source: Bitmap): Bitmap {
        val safeSource = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }

        val side = min(safeSource.width, safeSource.height)
        val left = (safeSource.width - side) / 2
        val top = (safeSource.height - side) / 2
        var bmp = Bitmap.createBitmap(safeSource, left, top, side, side)
        if (safeSource != source && bmp != safeSource) safeSource.recycle()

        while (bmp.width / 2 >= WORK_SIZE) {
            val next = Bitmap.createScaledBitmap(bmp, bmp.width / 2, bmp.height / 2, true)
            if (bmp != source) bmp.recycle()
            bmp = next
        }

        if (bmp.width == WORK_SIZE) return bmp
        val work = Bitmap.createScaledBitmap(bmp, WORK_SIZE, WORK_SIZE, true)
        if (bmp != source && bmp != work) bmp.recycle()
        return work
    }
}
