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

package com.android.systemui.statusbar.pipeline.mobile.ui

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
import javax.inject.Inject

@SysUISingleton
class SignalIconLoader @Inject constructor(
    private val context: Context
) {
    private var useOverlayIcons: Boolean = false
    private var useLevelList: Boolean = false
    private var overlayResourceIds: IntArray? = null
    private var numberOfLevels: Int = 0
    private var iconSize: Int = 0

    private val contentResolver: ContentResolver = context.contentResolver
    private val reloadCallbacks = mutableListOf<() -> Unit>()

    private var userPreferenceEnabled: Boolean = true

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
                ACTION_RELOAD_MOBILE_ICONS -> {
                    reload()
                    notifyReloadCallbacks()
                }
            }
        }
    }

    init {
        loadUserPreference()
        determineIconSize()

        if (userPreferenceEnabled) {
            detectOverlayIcons()
        }

        try {
            val filter = IntentFilter(ACTION_RELOAD_MOBILE_ICONS)
            context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register broadcast receiver", e)
        }

        try {
            contentResolver.registerContentObserver(
                Settings.System.getUriFor(SETTING_KEY),
                false,
                settingsObserver
            )
        } catch (e: Exception) {
        }
    }

    private fun determineIconSize() {
        val resources = context.resources
        try {
            val id = resources.getIdentifier(
                "status_bar_mobile_signal_size",
                "dimen",
                context.packageName
            )
            if (id != 0) {
                iconSize = resources.getDimensionPixelSize(id)
                return
            }
        } catch (e: Exception) {
        }

        iconSize = (24 * resources.displayMetrics.density).toInt()
    }

    private fun loadUserPreference() {
        try {
            val setting = Settings.System.getInt(
                contentResolver,
                SETTING_KEY,
                if (DEFAULT_USE_OVERLAYS) 1 else 0
            )
            userPreferenceEnabled = setting == 1
        } catch (e: Exception) {
            userPreferenceEnabled = DEFAULT_USE_OVERLAYS
        }
    }

    private fun detectOverlayIcons() {
        try {
            val resources = context.resources

            val levelListId = resources.getIdentifier(
                "ic_mobile_level_list",
                "drawable",
                "android"
            )

            if (levelListId != 0) {
                useOverlayIcons = true
                useLevelList = true
                return
            }

            if (tryLoadIndividualIcons(5)) {
                return
            }

            if (tryLoadIndividualIcons(4)) {
                return
            }

            useOverlayIcons = false
        } catch (e: Exception) {
            useOverlayIcons = false
        }
    }

    private fun tryLoadIndividualIcons(barCount: Int): Boolean {
        val resources = context.resources
        val iconIds = IntArray(barCount + 1) { level ->
            val name = "ic_signal_cellular_${level}_${barCount}_bar"
            resources.getIdentifier(name, "drawable", "android")
        }

        val allFound = iconIds.all { it != 0 }

        if (allFound) {
            useOverlayIcons = true
            useLevelList = false
            numberOfLevels = barCount
            overlayResourceIds = iconIds
            return true
        }

        return false
    }

    fun hasOverlayIcons(): Boolean {
        return userPreferenceEnabled && useOverlayIcons
    }

    fun usesLevelList(): Boolean {
        return useLevelList
    }

    fun getOverlayLevelCount(): Int {
        return numberOfLevels
    }

    fun loadSignalIcon(level: Int, numberOfLevels: Int, targetSize: Int? = null): Drawable? {
        if (!hasOverlayIcons()) {
            return null
        }

        val boundsSize = targetSize ?: iconSize

        try {
            val resources = context.resources

            if (useLevelList) {
                val levelListId = resources.getIdentifier(
                    "ic_mobile_level_list",
                    "drawable",
                    "android"
                )

                if (levelListId != 0) {
                    val drawable = resources.getDrawable(levelListId, context.theme)
                        ?.constantState?.newDrawable(resources)?.mutate()

                    if (drawable != null) {
                        val mappedLevel = mapToLevelListValue(level, numberOfLevels)
                        drawable.level = mappedLevel
                        drawable.setBounds(0, 0, boundsSize, boundsSize)
                        return drawable
                    }
                }
            }

            if (overlayResourceIds != null) {
                val mappedLevel = if (this.numberOfLevels == numberOfLevels) {
                    level
                } else {
                    (level * this.numberOfLevels.toFloat() / numberOfLevels.toFloat()).toInt()
                        .coerceIn(0, this.numberOfLevels)
                }

                if (mappedLevel >= 0 && mappedLevel < overlayResourceIds!!.size) {
                    val resourceId = overlayResourceIds!![mappedLevel]
                    if (resourceId != 0) {
                        val drawable = resources.getDrawable(resourceId, context.theme)
                            ?.constantState?.newDrawable(resources)?.mutate()
                        drawable?.setBounds(0, 0, boundsSize, boundsSize)
                        return drawable
                    }
                }
            }
        } catch (e: Resources.NotFoundException) {
        } catch (e: Exception) {
        }

        return null
    }

    private fun mapToLevelListValue(level: Int, numberOfLevels: Int): Int {
        return when (numberOfLevels) {
            4 -> level + 10
            5 -> level
            6 -> level
            else -> level
        }
    }

    fun hasSignalIconForLevel(level: Int, numberOfLevels: Int): Boolean {
        if (!hasOverlayIcons()) {
            return false
        }

        try {
            val drawable = loadSignalIcon(level, numberOfLevels)
            return drawable != null
        } catch (e: Exception) {
            return false
        }
    }

    fun reload() {
        useOverlayIcons = false
        useLevelList = false
        overlayResourceIds = null
        numberOfLevels = 0
        determineIconSize()
        loadUserPreference()
        if (userPreferenceEnabled) {
            detectOverlayIcons()
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
        private const val TAG = "SignalIconLoader"
        const val ACTION_RELOAD_MOBILE_ICONS = "com.android.systemui.RELOAD_MOBILE_ICONS"
        const val SETTING_KEY = "signal_icon_use_overlays"
        const val DEFAULT_USE_OVERLAYS = true

        fun hasOverlayIcons(context: Context): Boolean {
            val enabled = try {
                Settings.System.getInt(
                    context.contentResolver,
                    SETTING_KEY,
                    if (DEFAULT_USE_OVERLAYS) 1 else 0
                ) == 1
            } catch (e: Exception) {
                DEFAULT_USE_OVERLAYS
            }
            if (!enabled) return false
            val res = context.resources
            return res.getIdentifier("ic_mobile_level_list", "drawable", "android") != 0 ||
                res.getIdentifier("ic_signal_cellular_0_4_bar", "drawable", "android") != 0 ||
                res.getIdentifier("ic_signal_cellular_0_5_bar", "drawable", "android") != 0
        }

        fun sendReloadBroadcast(context: Context) {
            val intent = Intent(ACTION_RELOAD_MOBILE_ICONS)
            context.sendBroadcast(intent)
        }

        fun setOverlaysEnabled(context: Context, enabled: Boolean) {
            Settings.System.putInt(
                context.contentResolver,
                SETTING_KEY,
                if (enabled) 1 else 0
            )
        }
    }
}
