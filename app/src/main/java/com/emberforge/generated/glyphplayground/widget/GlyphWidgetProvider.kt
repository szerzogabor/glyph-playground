package com.emberforge.generated.glyphplayground.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.emberforge.generated.glyphplayground.PatternRepository
import com.emberforge.generated.glyphplayground.R

/**
 * Home-screen widget that shows one saved glyph at a time, rendered full-size —
 * no title, no labels. When more than one glyph is assigned, slim ‹ › chevrons
 * on the edges page through them; tapping the glyph itself lights it on the
 * physical Glyph Matrix (green dots mark the lit one). Which glyphs a widget
 * shows is chosen in [GlyphWidgetConfigActivity] (on placement, or later via
 * the launcher's "reconfigure" action).
 *
 * The glyph is drawn directly into the widget's [RemoteViews] rather than
 * through a collection view: StackView (the only swipeable RemoteViews
 * collection) always renders its items as a scaled-down 3D card deck with the
 * next card peeking out behind, which shrank the glyph and overlapped two of
 * them.
 */
class GlyphWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) renderWidget(context, manager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPrefs.clearWidget(context, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> {
                val glyphId = intent.getStringExtra(EXTRA_GLYPH_ID) ?: return
                handleToggle(context, glyphId)
            }
            ACTION_PREV, ACTION_NEXT -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
                val delta = if (intent.action == ACTION_NEXT) 1 else -1
                WidgetPrefs.setCurrentIndex(
                    context,
                    appWidgetId,
                    WidgetPrefs.getCurrentIndex(context, appWidgetId) + delta
                )
                renderWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }
    }

    /**
     * Toggles the tapped glyph on the physical Glyph Matrix. Tapping the active
     * glyph clears it; tapping another switches to it. Display goes through
     * [GlyphWidgetDisplayService], which uses the same background-capable
     * `setMatrixFrame` path the Glyph Toy uses. The selection is also stored so
     * the "Glyph Playground" toy shows the same glyph when triggered by the
     * Glyph button.
     */
    private fun handleToggle(context: Context, glyphId: String) {
        val pattern = PatternRepository(context).loadAll().find { it.id == glyphId } ?: return
        if (WidgetPrefs.getActiveGlyphId(context) == glyphId) {
            WidgetPrefs.setActiveGlyphId(context, null)
            GlyphWidgetDisplayService.hide(context)
        } else {
            WidgetPrefs.setActiveGlyphId(context, glyphId)
            GlyphWidgetDisplayService.show(context, pattern.activeLeds, pattern.ledBrightness)
        }
        refreshAll(context)
    }

    companion object {
        const val ACTION_TOGGLE = "com.emberforge.generated.glyphplayground.widget.TOGGLE"
        const val ACTION_PREV = "com.emberforge.generated.glyphplayground.widget.PREV"
        const val ACTION_NEXT = "com.emberforge.generated.glyphplayground.widget.NEXT"
        const val EXTRA_GLYPH_ID = "glyph_id"

        /** Rendered bitmap edge; the ImageView scales it to the widget size. */
        private const val BITMAP_PX = 480

        /** Rebuilds a single widget instance around its current glyph. */
        fun renderWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_glyph)

            val all = PatternRepository(context).loadAll()
            val selected = WidgetPrefs.getSelected(context, appWidgetId)
            // No explicit selection → show every saved glyph.
            val items = if (selected == null) all else all.filter { it.id in selected }

            if (items.isEmpty()) {
                views.setViewVisibility(R.id.widget_glyph_image, View.GONE)
                views.setViewVisibility(R.id.widget_prev, View.GONE)
                views.setViewVisibility(R.id.widget_next, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                // Empty state opens configuration to pick glyphs.
                val configPending = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    Intent(context, GlyphWidgetConfigActivity::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        data = Uri.parse("glyph://config/$appWidgetId")
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_empty, configPending)
                manager.updateAppWidget(appWidgetId, views)
                return
            }

            // floorMod keeps paging valid after the selection shrinks and lets
            // the chevrons wrap around both ends.
            val index = Math.floorMod(WidgetPrefs.getCurrentIndex(context, appWidgetId), items.size)
            WidgetPrefs.setCurrentIndex(context, appWidgetId, index)
            val pattern = items[index]
            val lit = WidgetPrefs.getActiveGlyphId(context) == pattern.id

            views.setImageViewBitmap(
                R.id.widget_glyph_image,
                GlyphBitmapRenderer.render(pattern.activeLeds, BITMAP_PX, lit, pattern.ledBrightness)
            )
            views.setContentDescription(R.id.widget_glyph_image, pattern.name)
            views.setViewVisibility(R.id.widget_glyph_image, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty, View.GONE)

            val pagerVisibility = if (items.size > 1) View.VISIBLE else View.GONE
            views.setViewVisibility(R.id.widget_prev, pagerVisibility)
            views.setViewVisibility(R.id.widget_next, pagerVisibility)

            views.setOnClickPendingIntent(
                R.id.widget_glyph_image,
                broadcast(context, appWidgetId, ACTION_TOGGLE, "glyph://toggle/$appWidgetId", pattern.id)
            )
            views.setOnClickPendingIntent(
                R.id.widget_prev,
                broadcast(context, appWidgetId, ACTION_PREV, "glyph://prev/$appWidgetId")
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                broadcast(context, appWidgetId, ACTION_NEXT, "glyph://next/$appWidgetId")
            )

            manager.updateAppWidget(appWidgetId, views)
        }

        /** Rebuilds every placed widget (e.g. after a toggle or a library edit). */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, GlyphWidgetProvider::class.java)
            )
            for (id in ids) renderWidget(context, manager, id)
        }

        /**
         * Self-addressed broadcast for a widget tap target. The data [uri]
         * makes each (widget, action) pair a distinct PendingIntent —
         * `Intent.filterEquals` ignores extras.
         */
        private fun broadcast(
            context: Context,
            appWidgetId: Int,
            action: String,
            uri: String,
            glyphId: String? = null
        ): PendingIntent {
            val intent = Intent(context, GlyphWidgetProvider::class.java).apply {
                this.action = action
                data = Uri.parse(uri)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                if (glyphId != null) putExtra(EXTRA_GLYPH_ID, glyphId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
