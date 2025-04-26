/*
 * Copyright (C) 2025 XperiaLabs Project
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.charger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.content.SharedPreferences
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.MainSwitchPreference
import androidx.viewpager.widget.ViewPager
import com.xperia.settings.charger.R
import com.xperia.settings.charger.widgets.CustomSeekBarPreference
import com.xperia.settings.charger.ChargerUtils.Companion.CHARGER_MAIN_ENABLE
import com.xperia.settings.charger.ChargerUtils.Companion.CHARGER_LIMIT_ENABLE
import com.xperia.settings.charger.ChargerUtils.Companion.CHARGER_HS_ENABLE

const val CHARGER_SETTING_ENABLE_KEY = "device_charging_main_enable"
const val CHARGER_CHARGING_ENABLE_KEY = "device_charging_enable"
const val CHARGER_CHARGING_ENABLE_BACKUP = "device_charging_enable_backup"
const val CHARGER_CHARGING_LIMIT_KEY = "device_charging_control"
const val CHARGER_CHARGING_LIMIT_BACKUP = "device_charging_control_backup"

class ChargerSettingsFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener {

    private lateinit var chargerUtils: ChargerUtils

    private var mSwitch: MainSwitchPreference? = null
    private var mChargingSwitch: SwitchPreferenceCompat? = null
    private var mChargingLimit: CustomSeekBarPreference? = null

    private lateinit var settingsObserver: ContentObserver

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.charger_settings, rootKey)

        chargerUtils = ChargerUtils(requireContext())

        mSwitch = findPreference<MainSwitchPreference>(CHARGER_SETTING_ENABLE_KEY)?.apply {
            isChecked = chargerUtils.mainSwitch
            onPreferenceChangeListener = this@ChargerSettingsFragment
        }

        mChargingSwitch = findPreference<SwitchPreferenceCompat>(CHARGER_CHARGING_ENABLE_KEY)?.apply {
            isChecked = chargerUtils.isHSPCEnabled
            onPreferenceChangeListener = this@ChargerSettingsFragment
        }

        mChargingLimit = findPreference<CustomSeekBarPreference>(CHARGER_CHARGING_LIMIT_KEY)?.apply {
            value = chargerUtils.chargingLimit
            onPreferenceChangeListener = this@ChargerSettingsFragment
        }

        addViewPager()

        settingsObserver = object : ContentObserver(Handler(requireContext().mainLooper)) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                mSwitch?.isChecked = chargerUtils.mainSwitch
                mChargingSwitch?.isChecked = chargerUtils.isHSPCEnabled
                mChargingLimit?.setValue(chargerUtils.chargingLimit, true)
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            CHARGER_SETTING_ENABLE_KEY -> {
                val isChecked = newValue as? Boolean ?: return false
                handleMainSwitchChange(isChecked)
                return true
            }
            CHARGER_CHARGING_ENABLE_KEY -> {
                Log.i(TAG, "H.S. Charging status changed: $newValue")
                chargerUtils.isHSPCEnabled = newValue as Boolean
            }
            CHARGER_CHARGING_LIMIT_KEY -> {
                chargerUtils.chargingLimit = newValue as Int
                val batteryStatus = requireContext().registerReceiver(
                    null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                batteryStatus?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    if (level != -1) {
                        chargerUtils.updateChargingState(level)
                    }
                }
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(chargerStateReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

         val resolver = requireContext().contentResolver
         resolver.registerContentObserver(
             Settings.Global.getUriFor(CHARGER_MAIN_ENABLE),
             true,
             settingsObserver
         )
         resolver.registerContentObserver(
             Settings.Global.getUriFor(CHARGER_LIMIT_ENABLE),
             true,
             settingsObserver
         )
         resolver.registerContentObserver(
             Settings.Global.getUriFor(CHARGER_HS_ENABLE),
             true,
             settingsObserver
         )
        settingsObserver.onChange(false, null)
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(chargerStateReceiver)
        requireContext().contentResolver.unregisterContentObserver(settingsObserver)
    }

    private fun handleMainSwitchChange(isChecked: Boolean) {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())

        if (!isChecked) {
            val prefChargingEnabled = sharedPreferences.getBoolean(CHARGER_CHARGING_ENABLE_KEY, false)
            val prefChargingLimit = sharedPreferences.getInt(CHARGER_CHARGING_LIMIT_KEY, 100)
            sharedPreferences.edit().putBoolean(CHARGER_CHARGING_ENABLE_BACKUP, prefChargingEnabled).apply()
            sharedPreferences.edit().putInt(CHARGER_CHARGING_LIMIT_BACKUP, prefChargingLimit).apply()

            chargerUtils.isHSPCEnabled = false
            chargerUtils.chargingLimit = 100

            mChargingSwitch?.isChecked = false
            mChargingLimit?.setValue(100, true)
        } else {
            val prefChargingEnabled = sharedPreferences.getBoolean(CHARGER_CHARGING_ENABLE_BACKUP, false)
            val prefChargingLimit = sharedPreferences.getInt(CHARGER_CHARGING_LIMIT_BACKUP, 100)

            chargerUtils.isHSPCEnabled = prefChargingEnabled
            chargerUtils.chargingLimit = prefChargingLimit

            mChargingSwitch?.isChecked = prefChargingEnabled
            mChargingLimit?.setValue(prefChargingLimit, true)
        }

        chargerUtils.mainSwitch = isChecked
    }

    private val chargerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val isPlugged = plugged != 0

                mChargingSwitch?.isEnabled = isPlugged
            }
        }
    }

    companion object {
        private const val TAG = "ChargerSettings"
    }

    private fun addViewPager() {
    }
}
