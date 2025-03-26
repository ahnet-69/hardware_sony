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

import com.xperia.settings.display.server.DisplayTransformManager

class CreatorModeUtils(private val context: Context) : IDisplayCallback.Stub() {
    private val colorDisplayManager: ColorDisplayManager =
            context.getSystemService(ColorDisplayManager::class.java)
                    ?: throw Exception("Display manager is NULL")
    private val semcDisplayService: IDisplay by lazy {
        val service = IDisplay.getService() ?: throw Exception("SEMC Display HIDL not found")
        service.setup()
        service
    }

    private val LEVEL_COLOR_MATRIX_CREATOR_MODE = 5690

    private val dtm: DisplayTransformManager = DisplayTransformManager()

    private val MAX = 255f

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
            if (!isEnabled) {
                semcDisplayService.set_sspp_color_mode(1)
                colorDisplayManager.setColorMode(3)
                semcDisplayService.set_color_mode(1)
            }

            semcDisplayService.registerCallback(this)
    
            if (isEnabled) {
                setMode(true)
            }
        } catch (e: Exception) {
        }
    }

    override fun onWhiteBalanceMatrixChanged(matrix: PccMatrix) {
        val r = matrix.red
        val g = matrix.green
        val b = matrix.blue

        try {
            dtm.setColorMatrix(LEVEL_COLOR_MATRIX_CREATOR_MODE,
                floatArrayOf(
                        r,  0f, 0f, 0f,
                        0f, g,  0f, 0f,
                        0f, 0f, b,  0f,
                        0f, 0f, 0f, 1f
                    ))
        } catch (e: Exception) {
            Log.e(TAG, "Could not apply setColorMatrix", e)
            return;
        }

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
