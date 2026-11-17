package com.laffy.caffeine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service that holds a screen wake lock until [CaffeineController.EXTRA_END_AT].
 * The countdown in the notification is drawn by the system (chronometer), so the app does no
 * periodic work: one timed wake lock + one delayed message, nothing else.
 */
class CaffeineService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopSelf() }
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            CaffeineController.stop(this)
            return START_NOT_STICKY
        }

        val now = System.currentTimeMillis()
        val endAt = intent?.getLongExtra(CaffeineController.EXTRA_END_AT, 0L) ?: 0L
        val remaining = (endAt - now).coerceAtLeast(1_000L)

        // Must run promptly after startForegroundService().
        val notification = buildNotification(now + remaining)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        @Suppress("DEPRECATION") // Still the only way to keep the screen on without an Activity window.
        val lock = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Caffeine:keepAwake")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(remaining) // Auto-releases even if we get killed.

        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, remaining)

        CaffeineState.endAt.value = now + remaining
        CaffeineTileService.requestUpdate(this)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        CaffeineState.endAt.value = 0L
        CaffeineTileService.requestUpdate(this)
        super.onDestroy()
    }

    private fun buildNotification(endAt: Long): android.app.Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW
                ).apply { setShowBadge(false) }
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, CaffeineService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_coffee)
            .setContentTitle(getString(R.string.notif_title))
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_stop), stop)
            .setWhen(endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private companion object {
        const val ACTION_STOP = "com.laffy.caffeine.STOP"
        const val CHANNEL_ID = "caffeine"
        const val NOTIFICATION_ID = 1
    }
}
