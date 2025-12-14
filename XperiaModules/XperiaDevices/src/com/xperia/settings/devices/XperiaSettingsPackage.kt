/*
 * Copyright (C) 2023 XperiaLabs Project
 * Copyright (C) 2022 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.devices

import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import androidx.preference.*
import androidx.core.content.pm.PackageInfoCompat

import com.xperia.settings.devices.R

import com.android.settingslib.widget.SettingsBasePreferenceFragment

class XperiaSettingsPackage(private val fragment: SettingsBasePreferenceFragment) {
    private val pm = fragment.activity?.packageManager
    private val extmonPackageName = "com.sonymobile.extmonitorapp"
    private val extmonClassName = "com.sonymobile.extmonitorapp.settings.SettingsAppLauncherActivity"
    private val usbaPackageName = "jp.co.sony.mc.usbextoutaudio"
    private val usbaClassName = "jp.co.sony.mc.usbextoutaudio.AudioSettingsActivity"
    private val dsmPackageName = "com.sonymobile.dualshockmanager"
    private val dsmClassName = "com.sonymobile.dualshockmanager.Ds4SetUpActivity"

    fun setupExtMonSettings() {
        try {
            val packageInfo = pm?.getPackageInfo(extmonPackageName, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null && PackageInfoCompat.getLongVersionCode(packageInfo) >= 1) {
                // Package exists and has a version code greater than or equal to 1, set preference to visible
                fragment.findPreference<Preference>("usb_extmon_settings")?.isVisible = true
                val intent = Intent().apply {
                    setClassName(extmonPackageName, extmonClassName)
                }
                fragment.findPreference<Preference>("usb_extmon_settings")?.intent = intent
            } else {
                // Package does not exist or has a version code less than 1, set preference to hidden
                val category = fragment.findPreference<PreferenceCategory>("usb")
                fragment.findPreference<Preference>("usb_extmon_settings")?.isVisible = false
                category?.isVisible = false
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Package does not exist, set preference to hidden
            val category = fragment.findPreference<PreferenceCategory>("usb")
            fragment.findPreference<Preference>("usb_extmon_settings")?.isVisible = false
            category?.isVisible = false
        }
    }
    fun setupUSBASettings() {
        try {
            val packageInfo = pm?.getPackageInfo(usbaPackageName, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null && PackageInfoCompat.getLongVersionCode(packageInfo) >= 1) {
                // Package exists and has a version code greater than or equal to 1, set preference to visible
                fragment.findPreference<Preference>("usb_audio_settings")?.isVisible = true
                val intent = Intent().apply {
                    setClassName(usbaPackageName, usbaClassName)
                }
                fragment.findPreference<Preference>("usb_audio_settings")?.intent = intent
            } else {
                // Package does not exist or has a version code less than 1, set preference to hidden
                val category = fragment.findPreference<PreferenceCategory>("usb")
                fragment.findPreference<Preference>("usb_audio_settings")?.isVisible = false
                category?.isVisible = false
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Package does not exist, set preference to hidden
            val category = fragment.findPreference<PreferenceCategory>("usb")
            fragment.findPreference<Preference>("usb_audio_settings")?.isVisible = false
            category?.isVisible = false
        }
    }
    fun setupDSMSettings() {
        try {
            val packageInfo = pm?.getPackageInfo(dsmPackageName, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null && PackageInfoCompat.getLongVersionCode(packageInfo) >= 1) {
                // Package exists and has a version code greater than or equal to 1, set preference to visible
                fragment.findPreference<Preference>("dsm_settings")?.isVisible = true
                val intent = Intent().apply {
                    setClassName(dsmPackageName, dsmClassName)
                }
                fragment.findPreference<Preference>("dsm_settings")?.intent = intent
            } else {
                // Package does not exist or has a version code less than 1, set preference to hidden
                val category = fragment.findPreference<PreferenceCategory>("gaming")
                fragment.findPreference<Preference>("dsm_settings")?.isVisible = false
                category?.isVisible = false
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Package does not exist, set preference to hidden
            val category = fragment.findPreference<PreferenceCategory>("gaming")
            fragment.findPreference<Preference>("dsm_settings")?.isVisible = false
            category?.isVisible = false
        }
    }
}
