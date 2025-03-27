/*
 * Copyright (C) 2024 XperiaLabs
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.touch.polling;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class HighTouchPollingService extends Service {
    private static final String TAG = "HighTouchPollingService";
    private static final String SETTING_KEY = Settings.Secure.TOUCH_SENSITIVITY_ENABLED;
    private static final String TS_NODE = "/sys/devices/virtual/sec/tsp/cmd";
    private static final String SET_REPORT_RATE_CMD = "doze_mode_change,";
    private static final String HIGH_POLLING_RATE = "2";
    private static final String LOW_POLLING_RATE = "1";

    private boolean mEnabled;
    private boolean mScreenOn = true;
    private PowerManager mPowerManager;
    private boolean isPowerSaveCached = false;

    private final ContentObserver mSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateTouchPollingState(true);
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    mScreenOn = true;
                    updateTouchPollingState(false);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    mScreenOn = false;
                    updateTouchPollingState(false);
                    break;
                case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED:
                    isPowerSaveCached = mPowerManager.isPowerSaveMode();
                    updateTouchPollingState(false);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mPowerManager = getSystemService(PowerManager.class);
        getContentResolver().registerContentObserver(
            Settings.Secure.getUriFor(SETTING_KEY),
            false, mSettingObserver);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        registerReceiver(mIntentReceiver, filter);
        isPowerSaveCached = mPowerManager.isPowerSaveMode();
        updateTouchPollingState(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mSettingObserver);
        unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, HighTouchPollingService.class),
                UserHandle.SYSTEM);
    }

    private void updateTouchPollingState(boolean readSetting) {
        if (readSetting) {
            mEnabled = Settings.Secure.getInt(getContentResolver(), SETTING_KEY, 0) == 1;
        }
        try (FileOutputStream fos = new FileOutputStream(TS_NODE)) {
            String valueToWrite;
            if (mScreenOn && mEnabled && !isPowerSaveCached) {
                valueToWrite = SET_REPORT_RATE_CMD + HIGH_POLLING_RATE;
            } else {
                valueToWrite = SET_REPORT_RATE_CMD + LOW_POLLING_RATE;
            }
            fos.write(valueToWrite.getBytes());
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to touch node", e);
        }
    }
}
