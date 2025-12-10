/*
 * Copyright (C) 2025-2026 RisingOS (Revived) Android Project
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

package com.android.systemui.statusbar.pipeline.wifi.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@SysUISingleton
class WifiIconBroadcastReceiver @Inject constructor(
    private val context: Context,
    private val wifiIconLoader: WifiIconLoader,
    @Background private val scope: CoroutineScope,
) {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_RELOAD_WIFI_ICONS -> {
                    scope.launch {
                        wifiIconLoader.reload()
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_RELOAD_WIFI_ICONS)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun destroy() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    companion object {
        private const val TAG = "WifiIconBroadcastReceiver"
        const val ACTION_RELOAD_WIFI_ICONS = "com.android.systemui.RELOAD_WIFI_ICONS"

        fun sendReloadBroadcast(context: Context) {
            val intent = Intent(ACTION_RELOAD_WIFI_ICONS)
            context.sendBroadcast(intent)
        }
    }
}
