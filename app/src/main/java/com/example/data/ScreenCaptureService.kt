package com.example.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "cash_flow_ai_screen_capture"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "ScreenCaptureService"

        // Keys must match what MainActivity uses
        const val EXTRA_RESULT_CODE = "com.example.extra.RESULT_CODE"
        const val EXTRA_RESULT_INTENT = "com.example.extra.RESULT_INTENT"

        @JvmStatic
        fun startService(context: Context) {
            Log.w(TAG, "startService(context) called — this is deprecated. Prefer the Activity permission flow.")
            val intent = Intent(context, ScreenCaptureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        @JvmStatic
        fun stopService(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java)
            context.stopService(intent)
        }
    }

    private var mediaProjection: MediaProjection? = null
    private lateinit var pm: MediaProjectionManager

    override fun onCreate() {
        super.onCreate()
        pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
        Log.d(TAG, "onCreate - notification channel ready. Not calling startForeground() yet.")
        // Do NOT call startForeground here — wait for valid projection data in onStartCommand
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand intent=$intent flags=$flags startId=$startId")
        if (intent == null) {
            Log.e(TAG, "Null intent in onStartCommand; stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, android.app.Activity.RESULT_CANCELED)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_INTENT)

        if (resultCode != android.app.Activity.RESULT_OK || resultData == null) {
            Log.e(TAG, "Invalid MediaProjection extras: resultCode=$resultCode resultData=${'$'}{resultData != null}")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            mediaProjection = pm.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjectionManager.getMediaProjection returned null")
                stopSelf()
                return START_NOT_STICKY
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException obtaining MediaProjection", se)
            stopSelf()
            return START_NOT_STICKY
        } catch (t: Throwable) {
            Log.e(TAG, "Exception obtaining MediaProjection", t)
            stopSelf()
            return START_NOT_STICKY
        }

        // Build notification
        val notification = buildNotification()

        // Use the 3-arg startForeground if available (to set the foreground service type),
        // otherwise fall back to 2-arg startForeground. Wrap in try/catch to prevent crashes.
        try {
            val used3Arg = tryUse3ArgStartForeground(notification)
            if (!used3Arg) {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.i(TAG, "startForeground completed (used3Arg=$used3Arg)")
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
            stopSelf()
            return START_NOT_STICKY
        }

        // Now it's safe to start the actual screen capture machinery
        try {
            startCaptureWith(mediaProjection!!)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed while starting capture", t)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun tryUse3ArgStartForeground(notification: Notification): Boolean {
        // Try reflection to call startForeground(id, notification, foregroundServiceType) if present.
        return try {
            val svcClass = Service::class.java
            val method = svcClass.getMethod("startForeground", Int::class.javaPrimitiveType, Notification::class.java, Int::class.javaPrimitiveType)
            val svcInfoClass = android.content.pm.ServiceInfo::class.java
            val field = try {
                svcInfoClass.getField("FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION")
            } catch (e: NoSuchFieldException) {
                null
            }
            val projectionType = field?.getInt(null) ?: 0
            method.invoke(this, NOTIFICATION_ID, notification, projectionType)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "3-arg startForeground not available or failed, will fallback to 2-arg", t)
            false
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, com.example.MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CASH FLOW AI Active")
            .setContentText("Continuously analyzing live chart screen for trading signals...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cash Flow AI Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active live chart screen capture for AI trading signals"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Start the capture machinery using the provided MediaProjection.
     * This should be replaced or integrated with the repo's existing capture implementation.
     */
    private fun startCaptureWith(mp: MediaProjection) {
        Log.d(TAG, "startCaptureWith: starting actual capture (placeholder)")
        // Existing capture setup would run here (VirtualDisplay, ImageReader, etc.)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            mediaProjection?.stop()
        } catch (t: Throwable) {
            Log.w(TAG, "Exception stopping mediaProjection", t)
        }
        super.onDestroy()
    }
}
