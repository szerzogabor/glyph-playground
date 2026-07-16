package com.emberforge.generated.glyphplayground.widget

import android.content.Context

/**
 * Storage for the Glyph widget:
 *  - which saved glyphs are assigned to each widget instance
 *    (`selected_<appWidgetId>`), and
 *  - which glyph (if any) is currently lit on the physical matrix
 *    (`active_glyph_id`, global — the hardware shows one frame at a time).
 */
object WidgetPrefs {

    private const val PREFS = "glyph_widget"
    private const val KEY_SELECTED_PREFIX = "selected_"
    private const val KEY_GLYPH_PREFIX = "glyph_" // legacy single-glyph storage
    private const val KEY_ACTIVE = "active_glyph_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setSelected(context: Context, appWidgetId: Int, ids: Set<String>) {
        prefs(context).edit()
            .putStringSet(KEY_SELECTED_PREFIX + appWidgetId, ids)
            .remove(KEY_GLYPH_PREFIX + appWidgetId)
            .apply()
    }

    /** Returns the assigned glyph ids, or null when the widget shows every saved glyph. */
    fun getSelected(context: Context, appWidgetId: Int): Set<String>? {
        val p = prefs(context)
        p.getStringSet(KEY_SELECTED_PREFIX + appWidgetId, null)?.let { return it }
        // Migrate widgets configured under single-glyph mode into a one-item set.
        return p.getString(KEY_GLYPH_PREFIX + appWidgetId, null)?.let { setOf(it) }
    }

    fun clearSelected(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(KEY_SELECTED_PREFIX + appWidgetId)
            .remove(KEY_GLYPH_PREFIX + appWidgetId)
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
