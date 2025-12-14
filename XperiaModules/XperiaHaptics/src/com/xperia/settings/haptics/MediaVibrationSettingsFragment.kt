/*
 * Copyright (c) 2025 Alcatraz323 <alcatraz32323@gmail.com>
 * Copyright (C) 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.haptics

import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.widget.CompoundButton
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.android.settingslib.widget.MainSwitchPreference
import com.xperia.settings.haptics.R
import com.xperia.settings.haptics.MediaVibration.getMediaVibrationLevel
import com.xperia.settings.haptics.MediaVibration.getMediaVibrationState
import com.xperia.settings.haptics.MediaVibration.setMediaVibrationLevel
import com.xperia.settings.haptics.MediaVibration.switchMediaVibration

import com.android.settingslib.widget.SettingsBasePreferenceFragment

class MediaVibrationSettingsFragment : SettingsBasePreferenceFragment(),
    Preference.OnPreferenceChangeListener,
    CompoundButton.OnCheckedChangeListener {
    private lateinit var mSwitchBar: MainSwitchPreference
    private lateinit var mLevelPref: ListPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.media_vibration_settings, rootKey)
        val enable = getMediaVibrationState(requireContext())

        mSwitchBar = findPreference<MainSwitchPreference>(PREF_MEDIA_VIB_ENABLE)!!
        mSwitchBar.addOnSwitchChangeListener(this)
        mSwitchBar.isChecked = enable

        mLevelPref = findPreference<ListPreference>(PREF_MEDIA_VIB_LEVEL)!!
        mLevelPref.onPreferenceChangeListener = this
        mLevelPref.isEnabled = enable
        mLevelPref.setValueIndex(getMediaVibrationLevel(requireContext()))
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.key) {
            PREF_MEDIA_VIB_LEVEL -> {
                setMediaVibrationLevel(requireContext(), newValue.toString().toInt())
                return true
            }
            else -> return false
        }
    }

    override fun onResume() {
        super.onResume()
        val resolver = requireContext().contentResolver
        resolver.registerContentObserver(
            Settings.Global.getUriFor(PREF_MEDIA_VIB_ENABLE),
            true,
            settingsObserver
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(PREF_MEDIA_VIB_LEVEL),
            true,
            settingsObserver
        )
        settingsObserver.onChange(false, null)
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(settingsObserver)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        mSwitchBar.isChecked = isChecked
        mLevelPref.isEnabled = isChecked
        switchMediaVibration(requireContext(), isChecked)
    }

    private val settingsObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            val ctx = requireContext()
            val enable = getMediaVibrationState(ctx)
            val level = getMediaVibrationLevel(ctx)
            mSwitchBar.isChecked = enable
            mLevelPref.isEnabled = enable
            mLevelPref.setValueIndex(level)
        }
    }

    companion object {
        const val PREF_MEDIA_VIB_ENABLE: String = "mediavib_enable"
        const val PREF_MEDIA_VIB_LEVEL: String = "mediavib_level"
    }
}
