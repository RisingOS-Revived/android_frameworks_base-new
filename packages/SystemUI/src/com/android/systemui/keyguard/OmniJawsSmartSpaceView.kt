/*
 * Copyright (C) 2025 RisingOS Revived Android Project
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
package com.android.systemui.keyguard

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.android.systemui.res.R

/**
 * Container for the SmartSpace OmniJaws weather integration in Keyguard
 */
class OmniJawsSmartSpaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "OmniJawsSmartSpaceView"
    private val DEBUG = false
    
    private var smartSpaceProvider: OmniJawsSmartSpaceProvider? = null
    private var contentView: View? = null
    private var settingsObserver: SettingsObserver? = null
    
    private var smartSpaceEnabled = false

    init {
        contentView = LayoutInflater.from(context)
            .inflate(R.layout.keyguard_weather, this, true)
        
        smartSpaceProvider = OmniJawsSmartSpaceProvider(context)
        smartSpaceProvider?.init(contentView!!)
        
        settingsObserver = SettingsObserver(Handler()).apply {
            observe()
        }
    }

    /**
     * Update the view based on current settings
     */
    private fun updateSettings() {
        smartSpaceEnabled = Settings.System.getIntForUser(
            context.contentResolver,
            SMARTSPACE_ENABLED,
            0, UserHandle.USER_CURRENT
        ) != 0
        
        visibility = if (smartSpaceEnabled) View.VISIBLE else View.GONE
        
        if (smartSpaceEnabled) {
            smartSpaceProvider?.refresh()
        }
        
        if (DEBUG) Log.d(TAG, "SmartSpace enabled: $smartSpaceEnabled")
    }
    
    /**
     * Observer to monitor changes to settings
     */
    inner class SettingsObserver(handler: Handler) : ContentObserver(handler) {
        
        fun observe() {
            context.contentResolver.apply {
                registerContentObserver(
                    Settings.System.getUriFor(SMARTSPACE_ENABLED),
                    false, this@SettingsObserver, UserHandle.USER_ALL
                )
                registerContentObserver(
                    Settings.System.getUriFor(LOCKSCREEN_WEATHER_ENABLED),
                    false, this@SettingsObserver, UserHandle.USER_ALL
                )
            }
            updateSettings()
        }
        
        fun unobserve() {
            context.contentResolver.unregisterContentObserver(this)
        }
        
        override fun onChange(selfChange: Boolean) {
            updateSettings()
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateSettings()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        smartSpaceProvider?.onDestroy()
        settingsObserver?.unobserve()
    }
    
    companion object {
        private const val SMARTSPACE_ENABLED = "smartspace_enabled"
        private const val LOCKSCREEN_WEATHER_ENABLED = "lockscreen_weather_enabled"
    }
}
