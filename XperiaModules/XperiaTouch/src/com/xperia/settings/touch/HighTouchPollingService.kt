/*
 * Copyright (C) 2025 XperiaLabs
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.touch

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.*
import android.provider.Settings
import android.util.Log
import java.io.FileOutputStream
import java.io.IOException

class HighTouchPollingService : Service() {

    companion object {
        private const val TAG = "HighTouchPollingService"
        private const val SETTING_KEY = Settings.Secure.TOUCH_SENSITIVITY_ENABLED
        private const val TS_NODE = "/sys/devices/virtual/sec/tsp/cmd"
        private const val SET_REPORT_RATE_CMD = "doze_mode_change,"
        private const val HIGH_POLLING_RATE = "2"
        private const val LOW_POLLING_RATE = "1"

        fun startService(context: Context) {
            context.startServiceAsUser(
                Intent(context, HighTouchPollingService::class.java),
                UserHandle.SYSTEM
            )
        }
    }

    private var mEnabled: Boolean = false
    private var mScreenOn: Boolean = true
    private lateinit var mPowerManager: PowerManager
    private var isPowerSaveCached: Boolean = false

    private val mSettingObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            updateTouchPollingState(true)
        }
    }

    private val mIntentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    mScreenOn = true
                    updateTouchPollingState(false)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    mScreenOn = false
                    updateTouchPollingState(false)
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    isPowerSaveCached = mPowerManager.isPowerSaveMode
                    updateTouchPollingState(false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mPowerManager = getSystemService(PowerManager::class.java)
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(SETTING_KEY),
            false,
            mSettingObserver
        )
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON).apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(mIntentReceiver, filter)
        isPowerSaveCached = mPowerManager.isPowerSaveMode
        updateTouchPollingState(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(mSettingObserver)
        unregisterReceiver(mIntentReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateTouchPollingState(readSetting: Boolean) {
        if (readSetting) {
            mEnabled = Settings.Secure.getInt(contentResolver, SETTING_KEY, 0) == 1
        }

        try {
            FileOutputStream(TS_NODE).use { fos ->
                val valueToWrite = if (mScreenOn && mEnabled && !isPowerSaveCached) {
                    SET_REPORT_RATE_CMD + HIGH_POLLING_RATE
                } else {
                    SET_REPORT_RATE_CMD + LOW_POLLING_RATE
                }
                fos.write(valueToWrite.toByteArray())
                fos.flush()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to touch node", e)
        }
    }
}
