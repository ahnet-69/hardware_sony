/*
 * Copyright (C) 2024 XperiaLabs Project
 * Copyright (C) 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.audio

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.xperia.settings.audio.R

class AudioSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private var dseePref: SwitchPreferenceCompat? = null
    private var windNrPref: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.audio_settings, rootKey)

        dseePref = findPreference("audio_dsee")
        windNrPref = findPreference("audio_windnr")

        dseePref?.setOnPreferenceChangeListener(this)
        windNrPref?.setOnPreferenceChangeListener(this)

        updateSwitchStates()
    }

    private fun updateSwitchStates() {
        val context = requireContext()
        dseePref?.isChecked = getDseeState(context)
        windNrPref?.isChecked = getWindNrState(context)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val enabled = newValue as Boolean
        val context = requireContext()

        when (preference.key) {
            "audio_dsee" -> {
                setDseeState(context, enabled)
                return true
            }
            "audio_windnr" -> {
                setWindNrState(context, enabled)
                return true
            }
        }
        return false
    }

    companion object {
        fun getDseeState(context: Context): Boolean {
            return Settings.Secure.getInt(
                context.contentResolver,
                "audio_dsee",
                0
            ) == 1
        }

        fun getWindNrState(context: Context): Boolean {
            return Settings.Secure.getInt(
                context.contentResolver,
                "audio_windnr",
                0
            ) == 1
        }

        private fun setDseeState(context: Context, enabled: Boolean) {
            context.getSystemService(AudioManager::class.java).apply {
                setParameters("dsee_hx_state=${if (enabled) 1 else 0}")
            }
            Settings.Secure.putInt(
                context.contentResolver,
                "audio_dsee",
                if (enabled) 1 else 0
            )
        }

        private fun setWindNrState(context: Context, enabled: Boolean) {
            context.getSystemService(AudioManager::class.java).apply {
                setParameters("wind_nr_enabled=${if (enabled) 1 else 0}")
            }
            Settings.Secure.putInt(
                context.contentResolver,
                "audio_windnr",
                if (enabled) 1 else 0
            )
        }

        fun applySettingsOnBoot(context: Context) {
            setDseeState(context, getDseeState(context))
            setWindNrState(context, getWindNrState(context))
        }
    }
}
