/*
 * Copyright (C) 2023 XperiaLabs Project
 * Copyright (C) 2022 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.devices

import android.os.Bundle
import androidx.preference.*
import android.content.pm.PackageManager

import com.xperia.settings.devices.R
import com.xperia.settings.devices.XperiaSettingsPackage

import com.android.settingslib.widget.SettingsBasePreferenceFragment

class XperiaSettingsFragment : SettingsBasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.xperia_settings, rootKey)

        val xperiaSettingsPackage = XperiaSettingsPackage(this)
        xperiaSettingsPackage.setupExtMonSettings()
        xperiaSettingsPackage.setupUSBASettings()
        xperiaSettingsPackage.setupDSMSettings()
    }
}
