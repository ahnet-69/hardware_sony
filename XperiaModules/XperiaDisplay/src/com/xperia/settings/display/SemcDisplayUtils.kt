/*
 * Copyright (C) 2023-2024 XperiaLabs Project
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.display

import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.os.ServiceManager
import android.provider.Settings
import android.util.Log
import com.xperia.settings.display.KcalUtils
import vendor.semc.hardware.display.V2_0.IDisplay
import vendor.semc.hardware.display.V2_0.IDisplayCallback
import vendor.semc.hardware.display.V2_0.PccMatrix
import vendor.semc.hardware.display.V2_2.IFramerateController
import vendor.semc.hardware.extlight.IExtLight

class SemcDisplayUtils(private val context: Context) : IDisplayCallback.Stub() {

    companion object {
        private const val TAG = "SemcDisplayUtils"
        private val WhiteBalanceProfiles = arrayListOf(
            RGBProfile(0.8627451f, 0.6666667f, 0.0f),
            RGBProfile(0.627451f, 0.5098039f, 0.0f),
            RGBProfile(0.3137255f, 0.35294116f, 0.0f),
            RGBProfile(0.09019607f, 0.21568628f, 0.0f),
            RGBProfile(0.0f, 0.0f, 0.0f),
            RGBProfile(0.0f, 0.11764706f, 0.2352941f),
            RGBProfile(0.0f, 0.1372549f, 0.35294116f),
            RGBProfile(0.0f, 0.1372549f, 0.4509804f),
            RGBProfile(0.0f, 0.17647058f, 0.5686275f),
            RGBProfile(0.0f, 0.25490195f, 0.6078431f)
        )

        const val CREATOR_MODE_ENABLE = "cm_enable"
        const val MOTION_BLUR_REDUCTION_ENABLE = "motion_blur_reduction_enable"
        const val VIDEO_IMAGE_ENHANCE_ENABLE = "video_image_enhance_enable"
        const val WHITE_BALANCE_PROF = "white_balance_profile"
    }

    private val colorDisplayManager: ColorDisplayManager =
        context.getSystemService(ColorDisplayManager::class.java)
            ?: throw Exception("Display manager is NULL")

    private val semcDisplayService by lazy {
        IDisplay.getService()?.apply {
            registerCallback(this@SemcDisplayUtils)
            setup()
        } ?: throw Exception("SEMC Display HIDL not found")
    }

    private val semcFramerateControllerService by lazy {
        IFramerateController.getService()
            ?: throw Exception("SEMC FramerateController HIDL not found")
    }

    private val extLightService by lazy {
        IExtLight.Stub.asInterface(
            ServiceManager.waitForDeclaredService(IExtLight.DESCRIPTOR + "/default")
        ) ?: throw Exception("SEMC ExtLight AIDL not found")
    }

    private val kcalUtils = KcalUtils()

    fun isCMEnabled(): Boolean =
        Settings.Global.getInt(context.contentResolver, CREATOR_MODE_ENABLE, 0) != 0

    fun getWbProfile(): Int =
        Settings.Global.getInt(context.contentResolver, WHITE_BALANCE_PROF, 4)

    fun isVideoImageEnhanceEnabled(): Boolean =
        Settings.Global.getInt(context.contentResolver, VIDEO_IMAGE_ENHANCE_ENABLE , 0) != 0

    fun isMotionBlurReductionEnabled(): Boolean =
        Settings.Global.getInt(context.contentResolver, MOTION_BLUR_REDUCTION_ENABLE, 0) != 0

    fun setCMMode(enable: Boolean) {
        semcDisplayService.set_sspp_color_mode(if (enable) 0 else 1)
        colorDisplayManager.setColorMode(if (enable) 0 else 3)
        semcDisplayService.set_color_mode(if (enable) 0 else 1)

        // Set old white balance once creator mode is disabled, since it should be on all the time (based on the selected setting)
        if(!enable) setWhiteBalance(getWbProfile())

        Settings.Global.putInt(context.contentResolver, CREATOR_MODE_ENABLE, if (enable) 1 else 0)
    }

    fun getVideoImageEnhanceEnabled(): Boolean = isVideoImageEnhanceEnabled()

    fun setVideoEnhanceEnabled(enable: Boolean) {
        extLightService.setExtHdr(if (enable) 1 else 0)
        Settings.Global.putInt(
            context.contentResolver,
            VIDEO_IMAGE_ENHANCE_ENABLE ,
            if (enable) 1 else 0
        )
    }

    fun setMotionBlurReductionEnabled(enable: Boolean) {
        semcFramerateControllerService.set_opec_mode(if (enable) 1 else 0)
        Settings.Global.putInt(
            context.contentResolver,
            MOTION_BLUR_REDUCTION_ENABLE,
            if (enable) 1 else 0
        )
    }

    fun initialize() {
        Log.e(TAG, "Creator Mode controller setup")
        if (isCMEnabled()) {
            setCMMode(true)
        } else {
            setWhiteBalance(getWbProfile())
        }
        if (isVideoImageEnhanceEnabled()) {
            setVideoEnhanceEnabled(true)
        }
        if (isMotionBlurReductionEnabled()) {
            setMotionBlurReductionEnabled(true)
        }
    }

    fun setWhiteBalance(profileIndex: Int) {
        val profile = WhiteBalanceProfiles.getOrNull(profileIndex)
            ?: throw IllegalArgumentException("Invalid white balance profile index: $profileIndex")
        val pccMatrix = PccMatrix().apply {
            red = profile.r
            green = profile.g
            blue = profile.b
        }
        Log.i(
            TAG,
            "New white balance: ${pccMatrix.red}, ${pccMatrix.green}, ${pccMatrix.blue}"
        )
        semcDisplayService.set_primary_pcc_matrix(pccMatrix)
        Settings.Global.putInt(context.contentResolver, WHITE_BALANCE_PROF, profileIndex)
    }

    override fun onWhiteBalanceMatrixChanged(matrix: PccMatrix?) {
        matrix ?: return

        // Convert float values (0.0-1.0) to 0-255 integers
        val r = (matrix.red * 255).toInt().coerceIn(0, 255)
        val g = (matrix.green * 255).toInt().coerceIn(0, 255)
        val b = (matrix.blue * 255).toInt().coerceIn(0, 255)

        kcalUtils.applyValues(r, g, b)
        Log.i(TAG, "New white balance: ${matrix.red}, ${matrix.green}, ${matrix.blue}")
    }

    data class RGBProfile(val r: Float, val g: Float, val b: Float)
}
