package com.laffy.caffeine

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * Foreground service that holds a screen wake lock for the current [Session] (timed or ∞).
 * The countdown in the notification is drawn by the system (chronometer), so the app does no
 * periodic work: one wake lock, at most one delayed message, and guard receivers that exist only
 * while this service runs.
 */
class CaffeineService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopSelf() }
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockLevel = 0

    private val guards = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stop = when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> Prefs.stopOnScreenOff(context)
                Intent.ACTION_POWER_DISCONNECTED -> Prefs.stopOnUnplug(context)
                Intent.ACTION_BATTERY_LOW -> true
                else -> false
            }
            if (stop) CaffeineController.stop(context)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this, guards,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_LOW)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CaffeineController.ACTION_STOP -> {
                CaffeineController.stop(this)
                return START_NOT_STICKY
            }
            CaffeineController.ACTION_EXTEND -> {
                val extended = CaffeineState.session.value?.extendedBy(EXTEND_MINUTES, System.currentTimeMillis())
                if (extended == null) {
                    // Stale "+5 min" (session already over): never start a new one.
                    stopSelf()
                    return START_NOT_STICKY
                }
                CaffeineState.session.value = extended
                arm(extended)
            }
            else -> {
                val duration = decodeDuration(
                    intent?.getIntExtra(CaffeineController.EXTRA_DURATION, 0) ?: 0
                )
                val startAt = intent?.getLongExtra(CaffeineController.EXTRA_START_AT, 0L)
                    ?.takeIf { it > 0L } ?: System.currentTimeMillis()
                val session = duration.toSession(startAt)
                CaffeineState.session.value = session
                arm(session)
            }
        }
        return START_NOT_STICKY
    }

    /** (Re)applies [session]: notification, wake lock and timeout. Replaces, never stacks. */
    @SuppressLint("WakelockTimeout") // ∞ sessions are bounded by the battery-low guard, not a timeout.
    private fun arm(session: Session) {
        val now = System.currentTimeMillis()

        // Must run promptly after startForegroundService().
        val notification = buildCaffeineNotification(this, session)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(CAFFEINE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(CAFFEINE_NOTIFICATION_ID, notification)
        }

        @Suppress("DEPRECATION") // Still the only way to keep the screen on without an Activity window.
        val level = if (Prefs.dimScreen(this)) PowerManager.SCREEN_DIM_WAKE_LOCK
        else PowerManager.SCREEN_BRIGHT_WAKE_LOCK
        if (wakeLock != null && wakeLockLevel != level) {
            wakeLock?.takeIf { it.isHeld }?.release()
            wakeLock = null
        }
        val lock = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(level, "Caffeine:keepAwake")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it; wakeLockLevel = level }

        handler.removeCallbacks(timeout)
        val remaining = session.remainingMs(now)
        if (remaining == null) {
            lock.acquire()
        } else {
            lock.acquire(remaining.coerceAtLeast(1_000L)) // Auto-releases even if we get killed.
            handler.postDelayed(timeout, remaining.coerceAtLeast(1_000L))
        }

        CaffeineTileService.requestUpdate(this)
    }

    override fun onDestroy() {
        unregisterReceiver(guards)
        handler.removeCallbacks(timeout)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        CaffeineState.session.value = null
        CaffeineTileService.requestUpdate(this)
        super.onDestroy()
    }
}
