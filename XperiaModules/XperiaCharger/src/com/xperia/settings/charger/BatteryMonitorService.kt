/*
 * Copyright (C) 2025 XperiaLabs Project
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.charger

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.os.UserHandle
import android.util.Log
import android.widget.Toast

class BatteryMonitorService : Service() {

    private lateinit var chargerUtils: ChargerUtils

    private var lastBatteryLevel = -1
    private var lastPluggedState = -1
    private var lastHspcEnabledState = false

    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                if (level != -1 && level != lastBatteryLevel) {
                    lastBatteryLevel = level
                    Log.d(TAG, "Battery level updated: $level%")
                    chargerUtils.updateChargingState(level)
                }

                val currentPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                if (currentPlugged != lastPluggedState) {
                    lastPluggedState = currentPlugged

                    if (currentPlugged != 0) {
                        if (lastHspcEnabledState && chargerUtils.mainSwitch) {
                            chargerUtils.isHSPCEnabled = true
                            Toast.makeText(context, context.getString(R.string.charger_hs_reconnected), Toast.LENGTH_SHORT).show()
                        }
                        lastHspcEnabledState = false
                    } else {
                        if (chargerUtils.isHSPCEnabled) {
                            if (chargerUtils.mainSwitch) {
                                lastHspcEnabledState = true
                            } else {
                                lastHspcEnabledState = false
                            }
                            chargerUtils.isHSPCEnabled = false
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        chargerUtils = ChargerUtils(this)

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        Log.d(TAG, "BatteryMonitorService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        Log.d(TAG, "onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "BatteryMonitorService"

        fun startService(context: Context) {
            val serviceIntent = Intent(context, BatteryMonitorService::class.java)
            context.startServiceAsUser(serviceIntent, UserHandle.SYSTEM)
        }
    }
}
