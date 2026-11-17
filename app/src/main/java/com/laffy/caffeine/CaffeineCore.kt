package com.laffy.caffeine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** Process-wide state shared by the service, the tile and the UI. null = off. */
object CaffeineState {
    val session = MutableStateFlow<Session?>(null)
}

/** Single entry point for the app, tile, shortcuts and notification: mutates the state and drives the service. */
object CaffeineController {
    const val ACTION_STOP = "com.laffy.caffeine.STOP"
    const val ACTION_EXTEND = "com.laffy.caffeine.EXTEND"
    const val EXTRA_DURATION = "duration"
    const val EXTRA_START_AT = "start_at"

    fun isActive(): Boolean = CaffeineState.session.value?.isActive(System.currentTimeMillis()) == true

    fun start(context: Context, duration: Duration) {
        val app = context.applicationContext
        if (isBatteryLow(app)) {
            Toast.makeText(app, R.string.low_battery, Toast.LENGTH_LONG).show()
            return
        }
        val session = duration.toSession(System.currentTimeMillis())
        CaffeineState.session.value = session
        try {
            ContextCompat.startForegroundService(
                app,
                Intent(app, CaffeineService::class.java)
                    .putExtra(EXTRA_DURATION, duration.encode())
                    .putExtra(EXTRA_START_AT, session.startAt)
            )
        } catch (e: Exception) {
            CaffeineState.session.value = null
        }
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        CaffeineState.session.value = null
        app.stopService(Intent(app, CaffeineService::class.java))
    }

    /** +[EXTEND_MINUTES] on a running timed session. The service decides what to do if it has expired. */
    fun extend(context: Context) {
        val app = context.applicationContext
        app.startService(Intent(app, CaffeineService::class.java).setAction(ACTION_EXTEND))
    }

    /** Tile tap: Off → 10 → 15 → 20 → ∞ → Off. */
    fun cycle(context: Context) {
        val current = CaffeineState.session.value
            ?.takeIf { it.isActive(System.currentTimeMillis()) }?.duration
        val next = TileCycle.next(current)
        if (next == null) stop(context) else start(context, next)
    }

    fun toggle(context: Context) =
        if (isActive()) stop(context) else start(context, Prefs.selected(context))

    private fun isBatteryLow(app: Context): Boolean {
        // Sticky broadcast: null receiver just reads the last value, nothing stays registered.
        val battery = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        return isBatteryLow(
            level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
            plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
            lowFlag = if (Build.VERSION.SDK_INT >= 28) {
                battery.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
            } else null
        )
    }
}
