/*
 * Copyright (C) 2025 XperiaLabs Project
 * Copyright (C) 2023 The LineageOS Project
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

package com.xperia.settings.charger.hspc

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlin.math.roundToInt

class HSPCQSTileService : TileService() {

    companion object {
        private const val TAG = "HSPCQSTileService"
        private const val ACTION_HSPC = "com.sonymobile.smartcharger.GE_CHARGE"
        private const val PREFS_NAME = "HSPC_Prefs"
        private const val KEY_IS_ACTIVE = "is_active"
        private const val EXTRA_SUSPEND = "SUSPEND"
        private const val EXTRA_THRESHOLD = "THRESHOLD"

        fun onBoot(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_ACTIVE, false).apply()
        }

        fun setHspcState(context: Context, enable: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_ACTIVE, enable).apply()

            val intent = Intent(ACTION_HSPC)
                .putExtra(EXTRA_SUSPEND, enable)
                .putExtra(EXTRA_THRESHOLD, calculateThreshold(context))
            context.sendBroadcast(intent, ACTION_HSPC)
            Log.i(TAG, "Broadcast GE_CHARGE with: $enable, THRESHOLD: ${calculateThreshold(context)}")
        }

        fun calculateThreshold(context: Context): Int {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 0

            val percentage = if (scale > 0) {
                ((level * 100.0f) / scale).roundToInt()
            } else {
                0
            }

            return when {
                percentage > 19 -> 23
                percentage > 5 -> percentage + 3
                else -> 8
            }
        }
    }

    override fun onStartListening() {
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        val isActive = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ACTIVE, false)

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        val newState = !getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ACTIVE, false)
        setHspcState(this, newState)
        updateTileState()
    }
}
