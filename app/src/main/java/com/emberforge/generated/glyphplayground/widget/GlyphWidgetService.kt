package com.emberforge.generated.glyphplayground.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.emberforge.generated.glyphplayground.GlyphPattern
import com.emberforge.generated.glyphplayground.PatternRepository
import com.emberforge.generated.glyphplayground.R

/** Supplies the scrollable list of glyph items shown inside the widget. */
class GlyphWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return GlyphWidgetFactory(applicationContext, appWidgetId)
    }
}

private class GlyphWidgetFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<GlyphPattern> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val all = PatternRepository(context).loadAll()
        val selected = WidgetPrefs.getSelected(context, appWidgetId)
        // No explicit selection → show every saved glyph.
        items = if (selected == null) all else all.filter { it.id in selected }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val pattern = items[position]
        val active = WidgetPrefs.getActiveGlyphId(context) == pattern.id

        val views = RemoteViews(context.packageName, R.layout.widget_glyph_item)
        views.setImageViewBitmap(
            R.id.item_glyph,
            GlyphBitmapRenderer.render(pattern.activeLeds, BITMAP_PX)
        )
        views.setTextViewText(R.id.item_name, pattern.name)
        views.setTextViewText(R.id.item_state, if (active) "SELECTED" else "TAP TO SELECT")
        views.setTextColor(R.id.item_state, if (active) COLOR_ON else COLOR_DIM)
        views.setInt(
            R.id.item_root,
            "setBackgroundResource",
            if (active) R.drawable.widget_item_bg_active else R.drawable.widget_item_bg
        )

        val fillIn = Intent().apply {
            putExtra(GlyphWidgetProvider.EXTRA_GLYPH_ID, pattern.id)
            // Unique data so each item keeps its own fill-in extras.
            data = Uri.parse("glyph://toggle/${pattern.id}")
        }
        views.setOnClickFillInIntent(R.id.item_root, fillIn)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    companion object {
        private const val BITMAP_PX = 144
        private const val COLOR_ON = 0xFF00D563.toInt()
        private const val COLOR_DIM = 0xFF777777.toInt()
    }
}
