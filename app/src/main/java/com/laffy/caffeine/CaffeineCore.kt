package com.laffy.caffeine

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** Duration setting, stored in SharedPreferences (cheapest option, read synchronously and rarely). */
object Prefs {
    val OPTIONS_MINUTES = listOf(10, 15, 20)
    private const val FILE = "caffeine"
    private const val KEY_MINUTES = "minutes"

    fun minutes(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_MINUTES, OPTIONS_MINUTES.first())

    fun setMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MINUTES, minutes).apply()
    }
}

/** Process-wide state shared by the service, the tile and the UI. 0 = off, else wall-clock end time. */
object CaffeineState {
    val endAt = MutableStateFlow(0L)
}

object CaffeineController {
    const val EXTRA_END_AT = "end_at"

    fun isActive(): Boolean = CaffeineState.endAt.value > System.currentTimeMillis()

    fun start(context: Context) {
        val app = context.applicationContext
        val endAt = System.currentTimeMillis() + Prefs.minutes(app) * 60_000L
        CaffeineState.endAt.value = endAt
        try {
            ContextCompat.startForegroundService(
                app,
                Intent(app, CaffeineService::class.java).putExtra(EXTRA_END_AT, endAt)
            )
        } catch (e: Exception) {
            CaffeineState.endAt.value = 0L
        }
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        CaffeineState.endAt.value = 0L
        app.stopService(Intent(app, CaffeineService::class.java))
    }

    fun toggle(context: Context) = if (isActive()) stop(context) else start(context)
}
