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
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.connectivity.WifiIcons
import javax.inject.Inject

@SysUISingleton
class WifiIconLoader @Inject constructor(
    private val context: Context
) {
    private var useOverlayIcons: Boolean = false
    private var overlayResourceIds: IntArray? = null
    private var numberOfLevels: Int = 0
    private var initialized: Boolean = false
    private var iconSize: Int = 0

    private val contentResolver: ContentResolver = context.contentResolver
    private val reloadCallbacks = mutableListOf<() -> Unit>()

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            loadUserPreference()
            reload()
            notifyReloadCallbacks()
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_RELOAD_WIFI_ICONS -> {
                    reload()
                    notifyReloadCallbacks()
                }
            }
        }
    }

    init {
        loadUserPreference()
        determineIconSize()

        try {
            val filter = IntentFilter(ACTION_RELOAD_WIFI_ICONS)
            context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register broadcast receiver", e)
        }

        initialize()

        try {
            contentResolver.registerContentObserver(
                Settings.System.getUriFor(SETTING_KEY),
                false,
                settingsObserver
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register settings observer", e)
        }
    }

    private fun determineIconSize() {
        val resources = context.resources
        try {
            val id = resources.getIdentifier(
                "status_bar_wifi_signal_size",
                "dimen",
                context.packageName
            )
            if (id != 0) {
                iconSize = resources.getDimensionPixelSize(id)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve status_bar_wifi_signal_size", e)
        }

        try {
            val fullSignalLevel = WifiIcons.WIFI_FULL_ICONS.size - 1
            val referenceResId = WifiIcons.WIFI_FULL_ICONS[fullSignalLevel]
            val referenceDrawable = resources.getDrawable(referenceResId, context.theme)
            if (referenceDrawable != null && referenceDrawable.intrinsicWidth > 0) {
                iconSize = referenceDrawable.intrinsicWidth
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve reference icon size", e)
        }

        iconSize = (24 * resources.displayMetrics.density).toInt()
    }

    private fun initialize() {
        if (initialized) return

        if (loadUserPreference()) {
            detectOverlayIcons()
        } else {
            useOverlayIcons = false
            overlayResourceIds = null
            numberOfLevels = 0
        }

        initialized = true
    }

    private fun loadUserPreference(): Boolean {
        try {
            val setting = Settings.System.getInt(contentResolver, SETTING_KEY, 0)
            return setting == 1
        } catch (e: Exception) {
            return false
        }
    }

    private fun detectOverlayIcons() {
        try {
            if (tryLoadIndividualIcons(5)) return
            if (tryLoadIndividualIcons(4)) return
            useOverlayIcons = false
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting overlay icons", e)
            useOverlayIcons = false
        }
    }

    private fun tryLoadIndividualIcons(levelCount: Int): Boolean {
        val resources = context.resources
        val iconIds = IntArray(levelCount) { level ->
            resources.getIdentifier("ic_wifi_signal_$level", "drawable", "android")
        }

        val allFound = iconIds.all { it != 0 }
        if (allFound) {
            useOverlayIcons = true
            numberOfLevels = levelCount
            overlayResourceIds = iconIds
            return true
        }
        return false
    }

    fun hasOverlayIcons(): Boolean {
        if (!initialized) initialize()
        return loadUserPreference() && useOverlayIcons
    }

    fun loadWifiIcon(level: Int, maxLevel: Int): Drawable? {
        if (!hasOverlayIcons()) return null

        try {
            val resources = context.resources
            overlayResourceIds?.let { ids ->
                val mappedLevel = if (numberOfLevels == maxLevel + 1) {
                    level
                } else {
                    (level * (numberOfLevels - 1).toFloat() / maxLevel.toFloat()).toInt()
                        .coerceIn(0, numberOfLevels - 1)
                }

                if (mappedLevel in ids.indices) {
                    val resourceId = ids[mappedLevel]
                    if (resourceId != 0) {
                        val drawable = resources.getDrawable(resourceId, context.theme)
                            ?.constantState?.newDrawable(resources)?.mutate()
                        drawable?.setBounds(0, 0, iconSize, iconSize)
                        return drawable
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading wifi icon", e)
        }
        return null
    }

    fun reload() {
        useOverlayIcons = false
        overlayResourceIds = null
        numberOfLevels = 0
        initialized = false
        determineIconSize()
        if (loadUserPreference()) {
            initialize()
        } else {
            initialized = true
        }
    }

    fun addReloadCallback(callback: () -> Unit) {
        reloadCallbacks.add(callback)
    }

    fun removeReloadCallback(callback: () -> Unit) {
        reloadCallbacks.remove(callback)
    }

    private fun notifyReloadCallbacks() {
        reloadCallbacks.forEach { it.invoke() }
    }

    fun destroy() {
        try {
            contentResolver.unregisterContentObserver(settingsObserver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister settings observer", e)
        }

        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister broadcast receiver", e)
        }

        reloadCallbacks.clear()
    }

    companion object {
        private const val TAG = "WifiIconLoader"
        private const val SETTING_KEY = "wifi_icon_use_overlays"
        private const val ACTION_RELOAD_WIFI_ICONS = "com.android.systemui.RELOAD_WIFI_ICONS"

        fun setOverlaysEnabled(context: Context, enabled: Boolean) {
            Settings.System.putInt(
                context.contentResolver,
                SETTING_KEY,
                if (enabled) 1 else 0
            )
        }
    }
}
