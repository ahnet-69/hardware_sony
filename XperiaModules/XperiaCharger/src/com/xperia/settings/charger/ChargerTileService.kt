/*
 * Copyright (C) 2025 XperiaLabs Project
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xperia.settings.charger

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.xperia.settings.charger.R

class ChargerTileService : TileService() {

    private lateinit var chargerUtils: ChargerUtils

    override fun onCreate() {
        super.onCreate()
        chargerUtils = ChargerUtils(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        if (plugged == 0) {
            Toast.makeText(this, R.string.charger_hs_unplugged, Toast.LENGTH_SHORT).show()
            updateTileState()
            return
        }

        if (!chargerUtils.mainSwitch) {
            Toast.makeText(this, R.string.charger_hs_toast, Toast.LENGTH_SHORT).show()
            updateTileState()
            return
        }

        chargerUtils.isHSPCEnabled = !chargerUtils.isHSPCEnabled
        updateTileState()
    }

    private fun updateTileState() {
        qsTile.state = if (chargerUtils.mainSwitch) {
            if (chargerUtils.isHSPCEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

}
