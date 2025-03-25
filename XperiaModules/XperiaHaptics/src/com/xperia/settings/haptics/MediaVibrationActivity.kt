/*
 * Copyright (c) 2025 Alcatraz323 <alcatraz32323@gmail.com>
 * Copyright (C) 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.haptics

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

class MediaVibrationActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(
            R.id.content_frame,
            MediaVibrationSettingsFragment(), TAG_MEDIAVIB
        ).commit()
    }

    companion object {
        private const val TAG_MEDIAVIB = "MediaVibration"
    }
}
