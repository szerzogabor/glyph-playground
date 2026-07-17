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
import com.emberforge.generated.glyphplayground.GlyphController
import com.emberforge.generated.glyphplayground.GlyphLayout
import com.emberforge.generated.glyphplayground.R
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Lights the physical Glyph Matrix in response to a widget tap.
 *
 * The working Glyph Toy draws with `setMatrixFrame` (the system matrix layer),
 * which — unlike the foreground-only App Matrix (`setAppMatrixFrame`) — keeps
 * displaying while the app is in the background. This service uses the same
 * `setMatrixFrame` call so a home-screen widget tap can show a glyph without
 * opening the app. It stays foreground so the binding (and therefore the frame)
 * survives, and runs in its own `:glyphwidget` process so it never disturbs the
 * app's or the toy's GlyphMatrixManager.
 */
class GlyphWidgetDisplayService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var connected = false

    private var pendingLeds: Set<Int>? = null
    private var pendingClear = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        try {
            gm = GlyphMatrixManager.getInstance(applicationContext)
            gm?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName) {
                    try {
                        gm?.register(Glyph.DEVICE_23112)
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
            ACTION_SHOW -> {
                pendingLeds = intent.getIntArrayExtra(EXTRA_LEDS)?.toSet() ?: emptySet()
                pendingClear = false
            }
            ACTION_HIDE -> {
                pendingLeds = null
                pendingClear = true
            }
        }
        if (connected) applyPending() else if (pendingClear) stopSelfSafely()
        return START_NOT_STICKY
    }

    private fun applyPending() {
        val mgr = gm ?: return
        try {
            when {
                pendingClear -> {
                    mgr.turnOff()
                    pendingClear = false
                    stopSelfSafely()
                }
                pendingLeds != null -> {
                    val leds = pendingLeds!!
                    val b = GlyphController.getSystemGlyphBrightness(this)
                    val colors = IntArray(GlyphLayout.TOTAL_LEDS) { if (it in leds) b else 0 }
                    mgr.setMatrixFrame(colors)
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
            gm?.turnOff()
            gm?.unInit()
        } catch (_: Exception) {
        }
        gm = null
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
        private const val TAG = "GlyphWidgetDisplay"
        private const val NOTIF_ID = 43
        private const val CHANNEL_ID = "glyph_widget_display"

        private const val ACTION_SHOW = "com.emberforge.generated.glyphplayground.widget.SHOW"
        private const val ACTION_HIDE = "com.emberforge.generated.glyphplayground.widget.HIDE"
        private const val EXTRA_LEDS = "leds"

        fun show(context: Context, leds: Set<Int>) {
            val intent = Intent(context, GlyphWidgetDisplayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_LEDS, leds.toIntArray())
            }
            context.startForegroundService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, GlyphWidgetDisplayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startForegroundService(intent)
        }
    }
}
