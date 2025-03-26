/*
 * Copyright (C) 2025 XperiaLabs Project
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.xperia.settings.charger

import android.content.Context
import android.provider.Settings
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ChargerUtils(private val context: Context) {

    var chargingLimit: Int
        get() = Settings.Global.getInt(context.contentResolver, CHARGER_LIMIT_ENABLE, 100)
        set(value) {
            Settings.Global.putInt(context.contentResolver, CHARGER_LIMIT_ENABLE, value)
            Log.i(TAG, "Charging limit updated to: $value")
        }

    var isHSPCEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, CHARGER_HS_ENABLE, 0) > 0
        set(value) {
            Settings.Global.putInt(context.contentResolver, CHARGER_HS_ENABLE, if (value) 1 else 0)
            val nodeValue = if (value) "1" else "0"
            if (writeSysfs(chargingInterruptionNode, nodeValue)) {
                Log.i(TAG, "HSPC toggled: $nodeValue")
            }
        }

    var mainSwitch: Boolean
        get() = Settings.Global.getInt(context.contentResolver, CHARGER_MAIN_ENABLE, 0) > 0
        set(value) {
            Settings.Global.putInt(
                context.contentResolver,
                CHARGER_MAIN_ENABLE,
                if (value) 1 else 0
            )
        }

    private val chargingInterruptionNode: String by lazy {
        determineChargingInterruptionNode()
    }
    private fun determineChargingInterruptionNode(): String {
        val board = getSystemProperty("ro.board.platform")
            ?: throw IllegalStateException("Failed to determine board platform")

        val boardIndex = getBoardIndex(board)
        if (boardIndex == -1) {
            throw IllegalArgumentException("Unsupported board detected: $board")
        }

        val boardPath = CHG_INTERRUPTION_PATHS[boardIndex]
        if (File(boardPath).exists()) {
            return boardPath
        } else {
            throw IllegalStateException("No valid charging node found for board: $board")
        }
    }

    private fun getBoardIndex(board: String): Int {
        return when (board) {
            "kona" -> 0
            "lahaina" -> 1
            "taro" -> 2
            "kalama" -> 3
            "lagoon" -> 4
            else -> -1 // Unsupported
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            get.invoke(null, key) as String?
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system property: $key", e)
            null
        }
    }

    fun updateChargingState(currentBatteryLevel: Int) {
        Log.i(TAG, "Battery level: $currentBatteryLevel, Charging limit: $chargingLimit")
        if (!isHSPCEnabled) {
            if (chargingLimit < 100) {
                val enableCharging = currentBatteryLevel < chargingLimit
                val nodeValue = if (enableCharging) "0" else "1"
                if (writeSysfs(chargingInterruptionNode, nodeValue)) {
                    Log.i(
                        TAG,
                        "Charging ${if (enableCharging) "resumed" else "suspended"} (level: $currentBatteryLevel, limit: $chargingLimit)"
                    )
                }
            } else {
                if (writeSysfs(chargingInterruptionNode, "0")) {
                    Log.i(TAG, "Charging resumed (limit: 100)")
                }
            }
        }
    }

    fun isChargingLimitEnabled(): Boolean = chargingLimit in 1..99

    fun applyOnBoot() {
        chargingLimit = Settings.Global.getInt(context.contentResolver, CHARGER_LIMIT_ENABLE, 100)
        // Disable HSPC Pref on Boot
        Settings.Global.putInt(context.contentResolver, CHARGER_HS_ENABLE, 0)
    }

    private fun writeSysfs(path: String, value: String): Boolean {
        return try {
            File(path).takeIf { it.exists() }?.let { file ->
                FileOutputStream(file).use { fos ->
                    fos.write(value.toByteArray())
                    fos.flush()
                }
                true
            } ?: run {
                Log.e(TAG, "Sysfs node $path does not exist")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to $path: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "ChargerUtils"
        const val CHARGER_MAIN_ENABLE = "device_charging_main_enable"
        const val CHARGER_LIMIT_ENABLE = "device_charging_limit_enable"
        const val CHARGER_HS_ENABLE = "device_charging_enable"

        private val CHG_INTERRUPTION_PATHS = arrayOf(
            "/sys/class/power_supply/battery_ext/smart_charging_interruption",
            "/sys/class/battchg_ext/smart_charging_interruption",
            "/sys/class/battchg_ext/smart_charging_interruption",
            "/sys/class/battchg_ext/smart_charging_interruption",
            "/sys/class/power_supply/battery_ext/smart_charging_interruption"
        )
    }

    object ChargerUtilsHolder {
        private var instance: ChargerUtils? = null

        fun init(context: Context) {
            if (instance == null) {
                instance = ChargerUtils(context.applicationContext)
            }
        }

        fun getInstance(): ChargerUtils {
            return instance
                ?: throw IllegalStateException("ChargerUtilsHolder not initialized. Call init(context) first.")
        }
    }
}
