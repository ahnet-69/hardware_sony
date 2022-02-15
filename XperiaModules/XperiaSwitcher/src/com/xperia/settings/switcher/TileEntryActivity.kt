/*
 * Copyright (C) 2021 Chaldeaprjkt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xperia.settings.switcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log

public class TileEntryActivity: Activity() {
    private val TAG: String = "TileEntryActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceClass: ComponentName? =
            intent?.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        when (sourceClass?.className) {
            else -> {
                finish()
            }
        }
    }

    fun openActivitySafely(dest: Intent) {
        try {
            dest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
            finish()
            startActivity(dest)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity found for " + dest)
            finish()
        }
    }
}
