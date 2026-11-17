package com.laffy.caffeine

import android.content.Context

/** Settings in SharedPreferences (cheapest option, read synchronously and rarely). */
object Prefs {
    private const val FILE = "caffeine"

    // Same key and encoding as v1 (a plain minute count), so existing installs keep their choice.
    private const val KEY_SELECTED = "minutes"
    private const val KEY_STOP_ON_SCREEN_OFF = "stop_on_screen_off"
    private const val KEY_STOP_ON_UNPLUG = "stop_on_unplug"
    private const val KEY_DIM_SCREEN = "dim_screen"
    private const val KEY_TILE_ADDED = "tile_added"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun selected(context: Context): Duration =
        decodeDuration(prefs(context).getInt(KEY_SELECTED, PRESET_MINUTES.first()))

    fun setSelected(context: Context, duration: Duration) =
        prefs(context).edit().putInt(KEY_SELECTED, duration.encode()).apply()

    fun stopOnScreenOff(context: Context) = prefs(context).getBoolean(KEY_STOP_ON_SCREEN_OFF, true)
    fun setStopOnScreenOff(context: Context, value: Boolean) = put(context, KEY_STOP_ON_SCREEN_OFF, value)

    fun stopOnUnplug(context: Context) = prefs(context).getBoolean(KEY_STOP_ON_UNPLUG, false)
    fun setStopOnUnplug(context: Context, value: Boolean) = put(context, KEY_STOP_ON_UNPLUG, value)

    fun dimScreen(context: Context) = prefs(context).getBoolean(KEY_DIM_SCREEN, false)
    fun setDimScreen(context: Context, value: Boolean) = put(context, KEY_DIM_SCREEN, value)

    fun tileAdded(context: Context) = prefs(context).getBoolean(KEY_TILE_ADDED, false)
    fun setTileAdded(context: Context, value: Boolean) = put(context, KEY_TILE_ADDED, value)

    private fun put(context: Context, key: String, value: Boolean) =
        prefs(context).edit().putBoolean(key, value).apply()
}
