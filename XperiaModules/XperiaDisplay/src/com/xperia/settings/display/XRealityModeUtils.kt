/*
 * Copyright (C) 2024 XperiaLabs Project
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.display

import android.app.Activity
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

class XRealityModeUtils(private val context: Context) : IDisplayCallback.Stub() {
    private val colorDisplayManager: ColorDisplayManager =
            context.getSystemService(ColorDisplayManager::class.java)
                    ?: throw Exception("Display manager is NULL")
    private val semcDisplayService: IDisplay by lazy {
        val service = IDisplay.getService() ?: throw Exception("SEMC Display HIDL not found")

        service.registerCallback(this)

        service.setup()
        service
    }

    val isEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, XREALITY_MODE_ENABLE, 0) != 0

    fun setMode(enabled: Boolean) {
        semcDisplayService.set_sspp_color_mode(if (enabled) 2 else 1)
        colorDisplayManager.setColorMode(if (enabled) 1 else 3)
        semcDisplayService.set_color_mode(if (enabled) 2 else 1)

        Settings.Global.putInt(context.contentResolver, XREALITY_MODE_ENABLE, if (enabled) 1 else 0)
    }

    fun initialize() {
        Log.e(TAG, "XReality Mode controller setup")

        if (isEnabled) {
            setMode(true)
        }
    }

    override fun onWhiteBalanceMatrixChanged(matrix: PccMatrix) {
        val r = matrix.red
        val g = matrix.green
        val b = matrix.blue
        val saturationAdjustment = 2f
        val brightnessAdjustment = 0.1f

        val adjustedRed = (saturationAdjustment * r).coerceIn(0f, 1f)
        val adjustedGreen = (saturationAdjustment * g).coerceIn(0f, 1f)
        val adjustedBlue = (saturationAdjustment * b).coerceIn(0f, 1f)
        
    try {
        val saturationAdjustment = 1.5f
        val brightnessAdjustment = 0.9f

        val adjustedRed = (saturationAdjustment * r).coerceIn(0f, 1f)
        val adjustedGreen = (saturationAdjustment * g).coerceIn(0f, 1f)
        val adjustedBlue = (saturationAdjustment * b).coerceIn(0f, 1f)

        val colorMatrix: ColorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                adjustedRed, 0f, 0f, 0f, brightnessAdjustment * 255, // Red
                0f, adjustedGreen, 0f, 0f, brightnessAdjustment * 255, // Green
                0f, 0f, adjustedBlue, 0f, brightnessAdjustment * 255, // Blue
                0f, 0f, 0f, 1f, 0f // Alpha
            ))
        }

            updateConfiguration()
        } catch (e: Exception) {
            Log.e(TAG, "Could not apply setColorMatrix", e)
        }
    }

    fun updateConfiguration() {
        try {
            ActivityTaskManager.getService().updateConfiguration(null)
        } catch (e: RemoteException) {
            Log.e(TAG, "Could not update configuration", e)
        }
    }

    companion object {
        private const val TAG = "XRealityUtils"
        const val XREALITY_MODE_ENABLE = "xr_enable"
    }
}
