/*
 * Copyright (C) 2024 XperiaLabs Project
 * Copyright (C) 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.audio

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.xperia.settings.audio.R

class AudioSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private var dseePref: SwitchPreferenceCompat? = null
    private var windNrPref: SwitchPreferenceCompat? = null

    private val settingsObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            updateSwitchStates()
        }
    }

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
            "audio_dsee" -> setDseeState(context, enabled)
            "audio_windnr" -> setWindNrState(context, enabled)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val resolver = requireContext().contentResolver
        resolver.registerContentObserver(Settings.Global.getUriFor("audio_dsee"), true, settingsObserver)
        resolver.registerContentObserver(Settings.Global.getUriFor("audio_windnr"), true, settingsObserver)
        updateSwitchStates()
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(settingsObserver)
    }

    companion object {
        fun getDseeState(context: Context): Boolean {
            return Settings.Global.getInt(context.contentResolver, "audio_dsee", 0) == 1
        }

        fun getWindNrState(context: Context): Boolean {
            return Settings.Global.getInt(context.contentResolver, "audio_windnr", 0) == 1
        }

        private fun setDseeState(context: Context, enabled: Boolean) {
            context.getSystemService(AudioManager::class.java).apply {
                setParameters("dsee_hx_state=${if (enabled) 1 else 0}")
            }
            Settings.Global.putInt(context.contentResolver, "audio_dsee", if (enabled) 1 else 0)
        }

        private fun setWindNrState(context: Context, enabled: Boolean) {
            context.getSystemService(AudioManager::class.java).apply {
                setParameters("wind_nr_enabled=${if (enabled) 1 else 0}")
            }
            Settings.Global.putInt(context.contentResolver, "audio_windnr", if (enabled) 1 else 0)
        }

        fun applySettingsOnBoot(context: Context) {
            setDseeState(context, getDseeState(context))
            setWindNrState(context, getWindNrState(context))
        }
    }
}
