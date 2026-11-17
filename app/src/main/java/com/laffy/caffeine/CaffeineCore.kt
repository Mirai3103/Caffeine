package com.laffy.caffeine

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** Process-wide state shared by the service, the tile and the UI. 0 = off, else wall-clock end time. */
object CaffeineState {
    val endAt = MutableStateFlow(0L)
}

object CaffeineController {
    const val EXTRA_END_AT = "end_at"

    fun isActive(): Boolean = CaffeineState.endAt.value > System.currentTimeMillis()

    fun start(context: Context) {
        val app = context.applicationContext
        // Temporary until Task 3 rewrites the controller around Session/Duration.
        val minutes = (Prefs.selected(app) as? Duration.Minutes)?.value ?: PRESET_MINUTES.first()
        val endAt = System.currentTimeMillis() + minutes * 60_000L
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
