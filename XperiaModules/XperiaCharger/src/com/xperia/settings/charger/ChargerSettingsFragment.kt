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
import android.util.Log
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.android.settingslib.widget.MainSwitchPreference
import androidx.viewpager.widget.ViewPager
import com.xperia.settings.charger.R
import com.xperia.settings.charger.widgets.CustomSeekBarPreference
import com.xperia.settings.charger.ChargerUtils.ChargerUtilsHolder

const val CHARGER_SETTING_ENABLE_KEY = "device_charging_main_enable"
const val CHARGER_CHARGING_ENABLE_KEY = "device_charging_enable"
const val CHARGER_CHARGING_ENABLE_BACKUP = "device_charging_enable_backup"
const val CHARGER_CHARGING_LIMIT_KEY = "device_charging_control"
const val CHARGER_CHARGING_LIMIT_BACKUP = "device_charging_control_backup"

class ChargerSettingsFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener {

    private lateinit var chargerUtils: ChargerUtils

    private var mSwitch: MainSwitchPreference? = null
    private var mChargingSwitch: SwitchPreference? = null
    private var mChargingLimit: CustomSeekBarPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.charger_settings, rootKey)

        ChargerUtilsHolder.init(requireContext())
        chargerUtils = ChargerUtilsHolder.getInstance()

        mSwitch = findPreference<MainSwitchPreference>(CHARGER_SETTING_ENABLE_KEY)?.apply {
            isChecked = chargerUtils.mainSwitch
            onPreferenceChangeListener = this@ChargerSettingsFragment
        }

        mChargingSwitch = findPreference<SwitchPreference>(CHARGER_CHARGING_ENABLE_KEY)?.apply {
            isChecked = chargerUtils.isHSPCEnabled
            onPreferenceChangeListener = this@ChargerSettingsFragment
        }

        mChargingLimit = findPreference<CustomSeekBarPreference>(CHARGER_CHARGING_LIMIT_KEY)?.apply {
            value = chargerUtils.chargingLimit
            onPreferenceChangeListener = this@ChargerSettingsFragment
        }

        addViewPager()
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
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(chargerStateReceiver)
    }

    private fun handleMainSwitchChange(isChecked: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        if (!isChecked) {
            sharedPreferences.edit().putBoolean(CHARGER_CHARGING_ENABLE_BACKUP, mChargingSwitch?.isChecked ?: false).apply()
            sharedPreferences.edit().putInt(CHARGER_CHARGING_LIMIT_BACKUP, mChargingLimit?.value ?: 100).apply()

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
