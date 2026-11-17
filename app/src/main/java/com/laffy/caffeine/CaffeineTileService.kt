package com.laffy.caffeine

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateFormat
import java.util.Date

/** Quick Settings tile. Declared ACTIVE_TILE so the system only binds it on click / explicit update. */
class CaffeineTileService : TileService() {

    override fun onTileAdded() = refresh()

    override fun onStartListening() = refresh()

    override fun onClick() {
        CaffeineController.toggle(this)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val endAt = CaffeineState.endAt.value
        val active = endAt > System.currentTimeMillis()
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (active) {
                getString(R.string.tile_until, DateFormat.getTimeFormat(this).format(Date(endAt)))
            } else {
                getString(R.string.tile_off)
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
