package com.emberforge.generated.glyphplayground.widget

import android.content.Context

/**
 * Storage for the Glyph widget:
 *  - which saved glyphs are assigned to each widget instance
 *    (`selected_<appWidgetId>`),
 *  - which of them the widget currently pages to (`index_<appWidgetId>`), and
 *  - which glyph (if any) is currently lit on the physical matrix
 *    (`active_glyph_id`, global — the hardware shows one frame at a time).
 */
object WidgetPrefs {

    private const val PREFS = "glyph_widget"
    private const val KEY_SELECTED_PREFIX = "selected_"
    private const val KEY_INDEX_PREFIX = "index_"
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

    /** Drops everything stored for a removed widget instance. */
    fun clearWidget(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(KEY_SELECTED_PREFIX + appWidgetId)
            .remove(KEY_INDEX_PREFIX + appWidgetId)
            .remove(KEY_GLYPH_PREFIX + appWidgetId)
            .apply()
    }

    /** Position of the glyph the widget is currently paged to. */
    fun getCurrentIndex(context: Context, appWidgetId: Int): Int =
        prefs(context).getInt(KEY_INDEX_PREFIX + appWidgetId, 0)

    fun setCurrentIndex(context: Context, appWidgetId: Int, index: Int) {
        prefs(context).edit().putInt(KEY_INDEX_PREFIX + appWidgetId, index).apply()
    }

    fun getActiveGlyphId(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE, null)

    fun setActiveGlyphId(context: Context, id: String?) {
        prefs(context).edit().apply {
            if (id == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, id)
        }.apply()
    }
}
