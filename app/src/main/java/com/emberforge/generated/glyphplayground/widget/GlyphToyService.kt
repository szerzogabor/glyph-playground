package com.emberforge.generated.glyphplayground.widget

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.emberforge.generated.glyphplayground.GlyphLayout
import com.emberforge.generated.glyphplayground.GlyphPattern
import com.emberforge.generated.glyphplayground.PatternRepository
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Glyph Toy that shows the saved glyphs on the physical Glyph Matrix.
 *
 * The App Matrix (`setAppMatrixFrame`) only displays while the app is in the
 * foreground, so it can't be driven from a home-screen widget. A Glyph Toy is
 * the officially supported *background* surface: the Glyph system binds this
 * service when the toy is active and delivers Glyph-button events over a
 * Messenger. Here we render the glyph the widget selected and let the Glyph
 * button toggle it on/off (tap) or cycle to the next saved glyph (long press).
 */
class GlyphToyService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var connected = false

    private var glyphs: List<GlyphPattern> = emptyList()
    private var index = 0
    private var isOn = true

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GlyphToy.MSG_GLYPH_TOY) return
            when (msg.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {
                GlyphToy.STATUS_PREPARE, GlyphToy.STATUS_START -> {
                    isOn = true
                    renderCurrent()
                }
                GlyphToy.STATUS_END -> turnOff()
                GlyphToy.EVENT_CHANGE -> {
                    advance()
                    isOn = true
                    renderCurrent()
                }
                GlyphToy.EVENT_ACTION_DOWN -> {
                    isOn = !isOn
                    if (isOn) renderCurrent() else turnOff()
                }
            }
        }
    }
    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder {
        loadGlyphs()
        initManager()
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        try {
            gm?.turnOff()
            gm?.unInit()
        } catch (_: Exception) {
        }
        gm = null
        connected = false
        return false
    }

    private fun loadGlyphs() {
        glyphs = PatternRepository(this).loadAll()
        val activeId = WidgetPrefs.getActiveGlyphId(this)
        index = glyphs.indexOfFirst { it.id == activeId }.let { if (it >= 0) it else 0 }
    }

    private fun initManager() {
        try {
            gm = GlyphMatrixManager.getInstance(applicationContext)
            gm?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName) {
                    try {
                        gm?.register(Glyph.DEVICE_23112)
                        connected = true
                        if (isOn) renderCurrent()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to register device", e)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    connected = false
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Glyph SDK not available", e)
        }
    }

    /** Advance to the next saved glyph and keep the widget's selection in sync. */
    private fun advance() {
        if (glyphs.isEmpty()) return
        index = (index + 1) % glyphs.size
        WidgetPrefs.setActiveGlyphId(this, glyphs[index].id)
        GlyphWidgetProvider.refreshAll(this)
    }

    private fun renderCurrent() {
        val mgr = gm ?: return
        if (!connected) return
        val leds = glyphs.getOrNull(index)?.activeLeds ?: emptySet()
        try {
            val colors = IntArray(GlyphLayout.TOTAL_LEDS) { if (it in leds) MAX_BRIGHTNESS else 0 }
            mgr.setMatrixFrame(colors)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render glyph", e)
        }
    }

    private fun turnOff() {
        try {
            gm?.turnOff()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off", e)
        }
    }

    companion object {
        private const val TAG = "GlyphToyService"
        private const val MAX_BRIGHTNESS = 255
    }
}
