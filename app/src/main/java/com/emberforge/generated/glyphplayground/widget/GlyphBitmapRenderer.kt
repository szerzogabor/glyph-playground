package com.emberforge.generated.glyphplayground.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.emberforge.generated.glyphplayground.GlyphLayout

/**
 * Renders a Glyph pattern into a [Bitmap] so it can be shown inside an app
 * widget via `RemoteViews.setImageViewBitmap`. Compose Canvas can't be used
 * in widgets, so this mirrors the look of `GlyphMatrixPreview`: a circular
 * disc of round LED dots.
 *
 * When [lit] is true the glyph is the one currently displayed on the physical
 * Glyph Matrix: its dots glow in the Nothing accent green. That green is the
 * widget's active-state cue — no ring, no text.
 */
object GlyphBitmapRenderer {

    private const val G = GlyphLayout.GRID_SIZE

    private const val COLOR_BG = 0xFF080808.toInt()
    private const val COLOR_LED_OFF = 0xFF1A1A1A.toInt()
    private const val COLOR_LED_ON = 0xFFFFFFFF.toInt()
    private const val COLOR_ACCENT = 0xFFD0FD3E.toInt()

    fun render(activeLeds: Set<Int>, sizePx: Int, lit: Boolean = false): Bitmap {
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
        val ledOn = if (lit) COLOR_ACCENT else COLOR_LED_ON

        for (row in 0 until G) {
            for (col in 0 until G) {
                val idx = row * G + col
                if (!GlyphLayout.isInsideCircle(idx)) continue
                dotPaint.color = if (idx in activeLeds) ledOn else COLOR_LED_OFF
                val left = col * cell + gap
                val top = row * cell + gap
                canvas.drawCircle(left + dot / 2f, top + dot / 2f, dot / 2f, dotPaint)
            }
        }

        canvas.restoreToCount(save)

        return bitmap
    }
}
