package com.emberforge.generated.glyphplayground.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.emberforge.generated.glyphplayground.GlyphController
import com.emberforge.generated.glyphplayground.GlyphLayout

object GlyphBitmapRenderer {

    private const val G = GlyphLayout.GRID_SIZE

    private const val COLOR_BG = 0xFF080808.toInt()
    private const val COLOR_LED_OFF = 0xFF1A1A1A.toInt()
    private const val COLOR_LED_ON = 0xFFFFFFFF.toInt()
    private const val COLOR_ACCENT = 0xFFD0FD3E.toInt()

    fun render(
        activeLeds: Set<Int>,
        sizePx: Int,
        lit: Boolean = false,
        ledBrightness: Map<Int, Int> = emptyMap()
    ): Bitmap {
        val size = sizePx.coerceAtLeast(G)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cell = size.toFloat() / G
        val radius = size / 2f
        val cx = radius
        val cy = radius

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_BG }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val save = canvas.save()
        val clip = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        canvas.clipPath(clip)

        val gap = (cell * 0.08f).coerceAtLeast(0.5f)
        val dot = cell - 2 * gap
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val useGrayscale = ledBrightness.isNotEmpty()

        for (row in 0 until G) {
            for (col in 0 until G) {
                val idx = row * G + col
                if (!GlyphLayout.isInsideCircle(idx)) continue

                dotPaint.color = if (useGrayscale) {
                    val b = ledBrightness.getOrDefault(idx, 0)
                    val fraction = b.toFloat() / GlyphController.MAX_BRIGHTNESS
                    lerpColor(COLOR_LED_OFF, if (lit) COLOR_ACCENT else COLOR_LED_ON, fraction)
                } else {
                    if (idx in activeLeds) (if (lit) COLOR_ACCENT else COLOR_LED_ON) else COLOR_LED_OFF
                }

                val left = col * cell + gap
                val top = row * cell + gap
                canvas.drawCircle(left + dot / 2f, top + dot / 2f, dot / 2f, dotPaint)
            }
        }

        canvas.restoreToCount(save)
        return bitmap
    }

    private fun lerpColor(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val a = lerp((from shr 24) and 0xFF, (to shr 24) and 0xFF, f)
        val r = lerp((from shr 16) and 0xFF, (to shr 16) and 0xFF, f)
        val g = lerp((from shr 8) and 0xFF, (to shr 8) and 0xFF, f)
        val b = lerp(from and 0xFF, to and 0xFF, f)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerp(a: Int, b: Int, f: Float): Int = (a + (b - a) * f).toInt()
}
