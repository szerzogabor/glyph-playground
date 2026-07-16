package com.emberforge.generated.glyphplayground.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.emberforge.generated.glyphplayground.PatternRepository
import com.emberforge.generated.glyphplayground.R

/**
 * Home-screen widget that shows a single saved glyph and nothing else. Tapping
 * the glyph toggles it on the physical Glyph Matrix; tapping again clears it.
 * Which glyph a widget shows is chosen in [GlyphWidgetConfigActivity] (on
 * placement, or later via the launcher's "reconfigure" action).
 */
class GlyphWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) renderWidget(context, manager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPrefs.clearGlyph(context, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val glyphId = intent.getStringExtra(EXTRA_GLYPH_ID) ?: return
            handleToggle(context, glyphId)
        }
    }

    /**
     * Toggles the widget's glyph on the physical Glyph Matrix. Tapping the
     * active glyph clears it; tapping when off switches to it. Display goes
     * through [GlyphWidgetDisplayService], which uses the same
     * background-capable `setMatrixFrame` path the Glyph Toy uses. The
     * selection is also stored so the "Glyph Playground" toy shows the same
     * glyph when triggered by the Glyph button.
     */
    private fun handleToggle(context: Context, glyphId: String) {
        val pattern = PatternRepository(context).loadAll().find { it.id == glyphId } ?: return
        if (WidgetPrefs.getActiveGlyphId(context) == glyphId) {
            WidgetPrefs.setActiveGlyphId(context, null)
            GlyphWidgetDisplayService.hide(context)
        } else {
            WidgetPrefs.setActiveGlyphId(context, glyphId)
            GlyphWidgetDisplayService.show(context, pattern.activeLeds)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.emberforge.generated.glyphplayground.widget.TOGGLE"
        const val EXTRA_GLYPH_ID = "glyph_id"

        private const val BITMAP_PX = 216

        /** Rebuilds a single widget instance to show its assigned glyph. */
        fun renderWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_glyph)

            val glyphId = WidgetPrefs.getGlyph(context, appWidgetId)
            val pattern = glyphId?.let { id ->
                PatternRepository(context).loadAll().find { it.id == id }
            }

            if (pattern == null) {
                // No glyph assigned (or it was deleted): prompt to pick one.
                views.setViewVisibility(R.id.widget_glyph, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent(context, appWidgetId))
            } else {
                views.setViewVisibility(R.id.widget_empty, View.GONE)
                views.setViewVisibility(R.id.widget_glyph, View.VISIBLE)
                views.setImageViewBitmap(
                    R.id.widget_glyph,
                    GlyphBitmapRenderer.render(pattern.activeLeds, BITMAP_PX)
                )
                views.setOnClickPendingIntent(R.id.widget_root, togglePendingIntent(context, appWidgetId, pattern.id))
            }

            manager.updateAppWidget(appWidgetId, views)
        }

        /** Re-renders every placed widget (e.g. after a glyph is edited or deleted). */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, GlyphWidgetProvider::class.java)
            )
            for (id in ids) renderWidget(context, manager, id)
        }

        private fun togglePendingIntent(context: Context, appWidgetId: Int, glyphId: String): PendingIntent {
            val intent = Intent(context, GlyphWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(EXTRA_GLYPH_ID, glyphId)
                // Unique data so each widget keeps its own extras.
                data = android.net.Uri.parse("glyph://toggle/$appWidgetId")
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun configPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, GlyphWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                data = android.net.Uri.parse("glyph://config/$appWidgetId")
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
