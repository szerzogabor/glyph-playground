package com.emberforge.generated.glyphplayground.widget

import android.content.Context

/**
 * Storage for the Glyph widget:
 *  - which single saved glyph is assigned to each widget instance
 *    (`glyph_<appWidgetId>`), and
 *  - which glyph (if any) is currently lit on the physical matrix
 *    (`active_glyph_id`, global — the hardware shows one frame at a time).
 */
object WidgetPrefs {

    private const val PREFS = "glyph_widget"
    private const val KEY_GLYPH_PREFIX = "glyph_"
    private const val KEY_SELECTED_PREFIX = "selected_" // legacy multi-select storage
    private const val KEY_ACTIVE = "active_glyph_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Assigns the single glyph a widget instance shows. */
    fun setGlyph(context: Context, appWidgetId: Int, id: String?) {
        prefs(context).edit().apply {
            if (id == null) remove(KEY_GLYPH_PREFIX + appWidgetId)
            else putString(KEY_GLYPH_PREFIX + appWidgetId, id)
            // Drop any leftover legacy selection for this widget.
            remove(KEY_SELECTED_PREFIX + appWidgetId)
        }.apply()
    }

    /** Returns the glyph id assigned to this widget, or null when none is set. */
    fun getGlyph(context: Context, appWidgetId: Int): String? {
        val p = prefs(context)
        p.getString(KEY_GLYPH_PREFIX + appWidgetId, null)?.let { return it }
        // Migrate widgets created before single-glyph mode: use the first of
        // the old multi-selection, if any.
        return p.getStringSet(KEY_SELECTED_PREFIX + appWidgetId, null)?.firstOrNull()
    }

    fun clearGlyph(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(KEY_GLYPH_PREFIX + appWidgetId)
            .remove(KEY_SELECTED_PREFIX + appWidgetId)
            .apply()
    }

    fun getActiveGlyphId(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE, null)

    fun setActiveGlyphId(context: Context, id: String?) {
        prefs(context).edit().apply {
            if (id == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, id)
        }.apply()
    }
}
