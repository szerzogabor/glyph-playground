package com.emberforge.generated.glyphplayground.widget

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.emberforge.generated.glyphplayground.GlyphController
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
 *
 * Besides the Glyph-button events, the widget and the in-app "Glyph" button
 * also drive the matrix through this service via [show]/[hide] start commands.
 * Riding on the Glyph system's own binding means no foreground service (and no
 * FOREGROUND_SERVICE_SPECIAL_USE permission) is needed, with one trade-off:
 * the commands only reach the hardware while this toy is the active one in the
 * Glyph settings — otherwise the selection is stored and shows the next time
 * the toy runs.
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

    /**
     * Widget/in-app commands. Each command finishes synchronously, so the
     * started state is released right away — the service stays alive only for
     * as long as the Glyph system keeps it bound.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                // Re-read the selection the sender just wrote in its own process.
                loadGlyphs()
                isOn = true
                val leds = intent.getIntArrayExtra(EXTRA_LEDS)?.toSet() ?: emptySet()
                val bKeys = intent.getIntArrayExtra(EXTRA_BRIGHTNESS_KEYS)
                val bVals = intent.getIntArrayExtra(EXTRA_BRIGHTNESS_VALS)
                val brightness = if (bKeys != null && bVals != null && bKeys.size == bVals.size) {
                    bKeys.zip(bVals).toMap()
                } else {
                    emptyMap()
                }
                renderFrame(leds, brightness)
            }
            ACTION_HIDE -> {
                isOn = false
                turnOff()
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
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
        val pattern = glyphs.getOrNull(index) ?: return
        renderFrame(pattern.activeLeds, pattern.ledBrightness)
    }

    private fun renderFrame(activeLeds: Set<Int>, ledBrightness: Map<Int, Int>) {
        val mgr = gm ?: return
        if (!connected) return
        try {
            val colors = if (ledBrightness.isNotEmpty()) {
                IntArray(GlyphLayout.TOTAL_LEDS) { ledBrightness.getOrDefault(it, 0) }
            } else {
                IntArray(GlyphLayout.TOTAL_LEDS) { if (it in activeLeds) GlyphController.MAX_BRIGHTNESS else 0 }
            }
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

        private const val ACTION_SHOW = "com.emberforge.generated.glyphplayground.widget.SHOW"
        private const val ACTION_HIDE = "com.emberforge.generated.glyphplayground.widget.HIDE"
        private const val EXTRA_LEDS = "leds"
        private const val EXTRA_BRIGHTNESS_KEYS = "brightness_keys"
        private const val EXTRA_BRIGHTNESS_VALS = "brightness_vals"

        /**
         * Lights [leds] on the matrix, provided the toy is currently active.
         * Callers are either foreground (MainActivity) or inside a widget-tap
         * broadcast, both of which may start services; the catch covers any
         * other background edge case rather than crashing the sender.
         */
        fun show(context: Context, leds: Set<Int>, ledBrightness: Map<Int, Int> = emptyMap()) {
            val intent = Intent(context, GlyphToyService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_LEDS, leds.toIntArray())
                if (ledBrightness.isNotEmpty()) {
                    putExtra(EXTRA_BRIGHTNESS_KEYS, ledBrightness.keys.toIntArray())
                    putExtra(EXTRA_BRIGHTNESS_VALS, ledBrightness.values.toIntArray())
                }
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to deliver show command", e)
            }
        }

        /** Clears the matrix, provided the toy is currently active. */
        fun hide(context: Context) {
            val intent = Intent(context, GlyphToyService::class.java).apply {
                action = ACTION_HIDE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to deliver hide command", e)
            }
        }
    }
}
