/*
 * Copyright (C) 2023-2024 XperiaLabs Project
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.display

import android.app.ActivityTaskManager
import android.content.Context
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import android.view.View

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.core.content.ContextCompat
import android.hardware.display.ColorDisplayManager

import vendor.semc.hardware.display.V2_0.IDisplay
import vendor.semc.hardware.display.V2_0.IDisplayCallback
import vendor.semc.hardware.display.V2_0.PccMatrix

import com.xperia.settings.display.KcalUtils

class CreatorModeUtils(private val context: Context) : IDisplayCallback.Stub() {
    private val colorDisplayManager: ColorDisplayManager =
            context.getSystemService(ColorDisplayManager::class.java)
                    ?: throw Exception("Display manager is NULL")
    private val semcDisplayService: IDisplay by lazy {
        val service = IDisplay.getService() ?: throw Exception("SEMC Display HIDL not found")

        // Register itself as callback for HIDL
        service.registerCallback(this)

        service.setup()
        service
    }

    private val kcalUtils = KcalUtils()

    val isEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, CREATOR_MODE_ENABLE, 0) != 0

    fun setMode(enabled: Boolean) {
        semcDisplayService.set_sspp_color_mode(if (enabled) 0 else 1)
        colorDisplayManager.setColorMode(if (enabled) 0 else 3)
        semcDisplayService.set_color_mode(if (enabled) 0 else 1)

        Settings.Global.putInt(context.contentResolver, CREATOR_MODE_ENABLE, if (enabled) 1 else 0)
    }

    fun initialize() {
        Log.i(TAG, "Creator Mode controller setup")
        try {
            setMode(isEnabled)
        } catch (e: Exception) {
        }
    }

    override fun onWhiteBalanceMatrixChanged(matrix: PccMatrix) {
        // Convert float values (0.0-1.0) to 0-255 integers
        val r = (matrix.red * 255).toInt().coerceIn(0, 255)
        val g = (matrix.green * 255).toInt().coerceIn(0, 255)
        val b = (matrix.blue * 255).toInt().coerceIn(0, 255)

        kcalUtils.applyValues(r, g, b)
        updateConfiguration()

        Log.i(TAG, "New white balance: ${r}, ${g}, ${b}")
    }

    fun updateConfiguration() {
        try {
            ActivityTaskManager.getService().updateConfiguration(null)
        } catch (e: RemoteException) {
            Log.e(TAG, "Could not update configuration", e)
        }
    }

    companion object {
        private const val TAG = "CreatorModeUtils"
        const val CREATOR_MODE_ENABLE = "cm_enable"
    }
}
