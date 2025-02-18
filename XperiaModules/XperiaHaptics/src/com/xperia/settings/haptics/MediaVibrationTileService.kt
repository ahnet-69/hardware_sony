/*
 * Copyright (c) 2025 Alcatraz323 <alcatraz32323@gmail.com>
 * Copyright (C) 2025 The LineageOS Project
 * Copyright (C) 2025 The XperiaLabs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.haptics

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xperia.settings.haptics.MediaVibration.getMediaVibrationLevel
import com.xperia.settings.haptics.MediaVibration.getMediaVibrationState
import com.xperia.settings.haptics.MediaVibration.setMediaVibrationLevel
import com.xperia.settings.haptics.MediaVibration.switchMediaVibration
import com.xperia.settings.haptics.R

class MediaVibrationTileService : TileService() {

    override fun onClick() {
        super.onClick()
        updateTileState(toggleMediaVibration())
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(getCurrentTileState())
    }

    private fun toggleMediaVibration(): TileState {
        val isEnabled = getMediaVibrationState(this)
        val currentLevel = getMediaVibrationLevel(this)

        return if (!isEnabled) {
            switchMediaVibration(this, true)
            setMediaVibrationLevel(this, 0)
            TileState(true, 0)
        } else {
            when (currentLevel) {
                0 -> updateLevel(1)
                1 -> updateLevel(2)
                2 -> updateLevel(3)
                else -> { // Level 3
                    switchMediaVibration(this, false)
                    TileState(false, 0)
                }
            }
        }
    }

    private fun updateLevel(newLevel: Int): TileState {
        setMediaVibrationLevel(this, newLevel)
        return TileState(true, newLevel)
    }

    private fun getCurrentTileState(): TileState {
        return TileState(
            getMediaVibrationState(this),
            getMediaVibrationLevel(this)
        )
    }

    private fun updateTileState(tileState: TileState) {
        qsTile?.apply {
            this.state = if (tileState.isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            this.label = getString(R.string.mediavib_title)
            this.subtitle = getString(
                if (tileState.isEnabled) R.string.mediavib_tile_enable
                else R.string.mediavib_tile_disable
            ).let {
                if (tileState.isEnabled) it.format(tileState.level) else it
            }
            updateTile()
        }
    }

    private data class TileState(val isEnabled: Boolean, val level: Int)
}
