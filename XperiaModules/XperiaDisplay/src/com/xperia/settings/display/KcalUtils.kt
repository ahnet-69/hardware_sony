/*
 * Copyright (C) 2025 XperiaLabs Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.display

import android.util.Log
import java.io.File

class KcalUtils {
    companion object {
        private const val TAG = "KcalUtils"
        const val KCAL_RED = "/sys/module/msm_drm/parameters/kcal_red"
        const val KCAL_GREEN = "/sys/module/msm_drm/parameters/kcal_green"
        const val KCAL_BLUE = "/sys/module/msm_drm/parameters/kcal_blue"
    }

    fun applyValues(r: Int, g: Int, b: Int) {
        writeSysFs(KCAL_RED, r.toString())
        writeSysFs(KCAL_GREEN, g.toString())
        writeSysFs(KCAL_BLUE, b.toString())
        Log.i(TAG, "Set Kcal: r: $r, g: $g, b: $b")
    }

    private fun writeSysFs(path: String, value: String) {
        try {
            File(path).takeIf { it.canWrite() }?.writeText(value)
        } catch (e: Exception) {
            Log.e(TAG, "Write failed: ${e.message}")
        }
    }
}
