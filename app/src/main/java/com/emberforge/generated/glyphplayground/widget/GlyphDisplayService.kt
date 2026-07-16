package com.emberforge.generated.glyphplayground.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.emberforge.generated.glyphplayground.GlyphLayout
import com.emberforge.generated.glyphplayground.R
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Foreground service that owns the connection to the Nothing Glyph Matrix and
 * keeps a frame lit while the widget has a glyph switched on. The binding must
 * stay alive for the frame to remain on the physical matrix, so this runs as a
 * foreground service and stops itself as soon as the glyph is switched off.
 *
 * Because binding is asynchronous, the requested frame is stashed and applied
 * from the SDK's connection callback.
 */
class GlyphDisplayService : Service() {

    private var manager: GlyphMatrixManager? = null
    private var connected = false

    private var pendingLeds: Set<Int>? = null
    private var pendingClear = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        try {
            manager = GlyphMatrixManager.getInstance(applicationContext)
            manager?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName) {
                    try {
                        manager?.register(Glyph.DEVICE_23112)
                        connected = true
                        applyPending()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISPLAY -> {
                pendingLeds = intent.getIntArrayExtra(EXTRA_LEDS)?.toSet() ?: emptySet()
                pendingClear = false
            }
            ACTION_CLEAR -> {
                pendingLeds = null
                pendingClear = true
            }
        }
        if (connected) applyPending() else if (pendingClear) stopSelfSafely()
        return START_NOT_STICKY
    }

    private fun applyPending() {
        val mgr = manager ?: return
        try {
            when {
                pendingClear -> {
                    mgr.closeAppMatrix()
                    pendingClear = false
                    stopSelfSafely()
                }
                pendingLeds != null -> {
                    val leds = pendingLeds!!
                    val colors = IntArray(GlyphLayout.TOTAL_LEDS) { if (it in leds) MAX_BRIGHTNESS else 0 }
                    mgr.setAppMatrixFrame(colors)
                    pendingLeds = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Glyph Matrix", e)
        }
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        try {
            manager?.closeAppMatrix()
            manager?.unInit()
        } catch (_: Exception) {
        }
        manager = null
        connected = false
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Glyph Matrix",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Keeps a glyph lit on the Glyph Matrix" }
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Glyph Matrix")
            .setContentText("Displaying a glyph")
            .setSmallIcon(R.drawable.ic_glyph)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "GlyphDisplayService"
        private const val MAX_BRIGHTNESS = 255
        private const val NOTIF_ID = 42
        private const val CHANNEL_ID = "glyph_matrix_display"

        private const val ACTION_DISPLAY = "com.emberforge.generated.glyphplayground.widget.DISPLAY"
        private const val ACTION_CLEAR = "com.emberforge.generated.glyphplayground.widget.CLEAR"
        private const val EXTRA_LEDS = "leds"

        fun display(context: Context, leds: Set<Int>) {
            val intent = Intent(context, GlyphDisplayService::class.java).apply {
                action = ACTION_DISPLAY
                putExtra(EXTRA_LEDS, leds.toIntArray())
            }
            context.startForegroundService(intent)
        }

        fun clear(context: Context) {
            val intent = Intent(context, GlyphDisplayService::class.java).apply {
                action = ACTION_CLEAR
            }
            context.startForegroundService(intent)
        }
    }
}
