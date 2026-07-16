package com.emberforge.generated.glyphplayground.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.emberforge.generated.glyphplayground.PatternRepository
import com.emberforge.generated.glyphplayground.R

/**
 * Home-screen widget that lists the saved glyphs assigned to it. The list
 * scrolls when more than one glyph is shown, and tapping a glyph toggles it
 * on/off on the physical Glyph Matrix.
 */
class GlyphWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) renderWidget(context, manager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPrefs.clearSelected(context, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val glyphId = intent.getStringExtra(EXTRA_GLYPH_ID) ?: return
            handleToggle(context, glyphId)
        }
    }

    private fun handleToggle(context: Context, glyphId: String) {
        val pattern = PatternRepository(context).loadAll().find { it.id == glyphId } ?: return
        if (WidgetPrefs.getActiveGlyphId(context) == glyphId) {
            WidgetPrefs.setActiveGlyphId(context, null)
            GlyphDisplayService.clear(context)
        } else {
            WidgetPrefs.setActiveGlyphId(context, glyphId)
            GlyphDisplayService.display(context, pattern.activeLeds)
        }
        refreshAll(context)
    }

    companion object {
        const val ACTION_TOGGLE = "com.emberforge.generated.glyphplayground.widget.TOGGLE"
        const val EXTRA_GLYPH_ID = "glyph_id"

        /** Rebuilds a single widget instance and refreshes its list contents. */
        fun renderWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_glyph)

            val adapterIntent = Intent(context, GlyphWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, adapterIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Template intent: item fill-in intents add the glyph id.
            val toggleTemplate = Intent(context, GlyphWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val togglePending = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, togglePending)

            // Empty state and the header button both open configuration.
            val configPending = PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, GlyphWidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_empty, configPending)
            views.setOnClickPendingIntent(R.id.widget_config, configPending)

            manager.updateAppWidget(appWidgetId, views)
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }

        /** Refreshes the glyph list of every placed widget (e.g. after a toggle or edit). */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, GlyphWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            }
        }
    }
}
