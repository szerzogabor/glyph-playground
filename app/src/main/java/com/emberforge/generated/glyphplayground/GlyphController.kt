package com.emberforge.generated.glyphplayground

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

class GlyphController(context: Context) {

    private val appContext: Context = context.applicationContext
    private var manager: GlyphMatrixManager? = null
    private var connected = false

    init {
        try {
            manager = GlyphMatrixManager.getInstance(appContext)
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

    fun displayPattern(activeLeds: Set<Int>) {
        if (!connected) return
        try {
            val b = getSystemGlyphBrightness(appContext)
            val colors = IntArray(GlyphLayout.TOTAL_LEDS) { if (it in activeLeds) b else 0 }
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

        fun getSystemGlyphBrightness(context: Context): Int {
            return try {
                val fraction = Settings.System.getFloat(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_FLOAT,
                    0.5f
                )
                (fraction.coerceIn(0f, 1f) * MAX_BRIGHTNESS).toInt().coerceAtLeast(1)
            } catch (_: Exception) {
                MAX_BRIGHTNESS
            }
        }
    }
}
