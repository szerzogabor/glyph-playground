package com.emberforge.generated.glyphplayground

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

class GlyphController(context: Context) {

    private var manager: GlyphMatrixManager? = null
    private var connected = false

    init {
        try {
            manager = GlyphMatrixManager.getInstance(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Glyph SDK not available", e)
        }
    }

    fun init() {
        manager?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                try {
                    manager?.register(Glyph.DEVICE_23112)
                    connected = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to register device", e)
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                connected = false
            }
        })
    }

    val isAvailable get() = connected

    fun displayPattern(activeLeds: Set<Int>, ledBrightness: Map<Int, Int> = emptyMap()) {
        if (!connected) return
        try {
            val colors = if (ledBrightness.isNotEmpty()) {
                IntArray(GlyphLayout.TOTAL_LEDS) { ledBrightness.getOrDefault(it, 0) }
            } else {
                IntArray(GlyphLayout.TOTAL_LEDS) { if (it in activeLeds) MAX_BRIGHTNESS else 0 }
            }
            manager?.setAppMatrixFrame(colors)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display pattern", e)
        }
    }

    fun clear() {
        if (!connected) return
        try {
            manager?.closeAppMatrix()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear display", e)
        }
    }

    fun destroy() {
        try {
            manager?.closeAppMatrix()
            manager?.unInit()
        } catch (_: Exception) {}
        manager = null
        connected = false
    }

    companion object {
        private const val TAG = "GlyphController"
        const val MAX_BRIGHTNESS = 4095
    }
}
