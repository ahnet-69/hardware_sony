/*
 * Copyright (C) 2023 XperiaLabs Project
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

package com.xperia.settings.switcher

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.Display
import java.util.ArrayList
import java.util.Locale

import com.xperia.settings.switcher.R

class RefreshRateTileService : TileService() {

    private lateinit var context: Context

    private lateinit var availableRates: List<Int>
    private var activeRateMin = 0
    private var activeRateMax = 0

    private lateinit var displayManager: DisplayManager

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            syncFromSettings()
            updateTileView()
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        displayManager = context.getSystemService(DisplayManager::class.java)
            ?: throw Exception("Display manager is NULL")

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            ?: throw Exception("Can not find default display")

        val supportedModes = display.supportedModes
        val currentMode = display.mode

        availableRates = supportedModes
            .filter { mode ->
                mode.physicalWidth == currentMode.physicalWidth &&
                mode.physicalHeight == currentMode.physicalHeight
            }
            .map { mode -> mode.refreshRate.toInt() }
            .distinct()
            .sorted()

        syncFromSettings()
    }

    private fun getSettingOf(key: String): Int {
        val defaultRate = resources.getInteger(R.integer.config_defaultRefreshRate)
        val rate = Settings.System.getInt(context.contentResolver, key, defaultRate)
        val active = availableRates.indexOf(rate)
        return active.coerceAtLeast(0)
    }

    private fun syncFromSettings() {
        activeRateMin = getSettingOf(Settings.System.MIN_REFRESH_RATE)
        activeRateMax = getSettingOf(Settings.System.PEAK_REFRESH_RATE)
    }

    private fun cycleRefreshRate() {
        activeRateMin = (activeRateMin + 1) % availableRates.size

        val rate = availableRates[activeRateMin]
        Settings.System.putInt(context.contentResolver, Settings.System.MIN_REFRESH_RATE, rate)
        Settings.System.putInt(context.contentResolver, Settings.System.PEAK_REFRESH_RATE, rate)
    }

    private fun updateTileView() {
        val tile = qsTile
        val displayText: String
        val min = availableRates[activeRateMin]
        val max = availableRates[activeRateMax]

        displayText = if (min == max) "%d Hz".format(Locale.US, min) else "%d - %d Hz".format(Locale.US, min, max)
        tile.contentDescription = displayText
        tile.subtitle = displayText
        tile.state = if (min == max) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.MIN_REFRESH_RATE),
            false,
            settingsObserver
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.PEAK_REFRESH_RATE),
            false,
            settingsObserver
        )
        syncFromSettings()
        updateTileView()
        checkRefreshRateAvailable()
    }

    override fun onStopListening() {
        super.onStopListening()
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    private fun checkRefreshRateAvailable() {
        val tile = qsTile
        val hasDefaultRefreshRate = resources.getInteger(R.integer.config_defaultRefreshRate)
        val hasDefaultPeakRefreshRate = resources.getInteger(R.integer.config_defaultPeakRefreshRate)

        if (hasDefaultRefreshRate != -1 || hasDefaultPeakRefreshRate != 0) {
            stopSelf()
            tile.state = Tile.STATE_UNAVAILABLE
            tile.subtitle = "Not Supported"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile
        if (tile.state != Tile.STATE_UNAVAILABLE) {
            cycleRefreshRate()
        }
    }
}
