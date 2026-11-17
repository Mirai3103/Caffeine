package com.laffy.caffeine

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateFormat
import java.util.Date

/**
 * Quick Settings tile. Tap cycles Off → 10 → 15 → 20 → ∞ → Off ([CaffeineController.cycle]);
 * long-press opens the app (QS_TILE_PREFERENCES on MainActivity).
 * Declared ACTIVE_TILE so the system only binds it on click / explicit update.
 */
class CaffeineTileService : TileService() {

    override fun onTileAdded() {
        Prefs.setTileAdded(this, true)
        refresh()
    }

    override fun onTileRemoved() = Prefs.setTileAdded(this, false)

    override fun onStartListening() = refresh()

    override fun onClick() {
        CaffeineController.cycle(this)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val session = CaffeineState.session.value?.takeIf { it.isActive(System.currentTimeMillis()) }
        tile.state = if (session != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) {
            val endAt = session?.endAt
            tile.subtitle = when {
                session == null -> getString(R.string.tile_off)
                endAt == null -> getString(R.string.tile_infinite)
                else -> getString(R.string.tile_until, DateFormat.getTimeFormat(this).format(Date(endAt)))
            }
        }
        tile.updateTile()
    }

    companion object {
        fun requestUpdate(context: Context) {
            requestListeningState(context, ComponentName(context, CaffeineTileService::class.java))
        }
    }
}
