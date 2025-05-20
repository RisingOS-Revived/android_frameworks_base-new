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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.android.systemui.res.R
import com.android.systemui.weather.OmniJawsSmartSpaceController
import com.android.systemui.weather.WeatherViewController

/**
 * Integrates OmniJaws weather with SmartSpace data for the Keyguard
 */
class OmniJawsSmartSpaceProvider(private val context: Context) {
    
    private val TAG = "OmniJawsSmartSpace"
    private val DEBUG = false
    
    private var weatherIcon: ImageView? = null
    private var weatherText: TextView? = null
    private var smartSpaceCardIcon: ImageView? = null
    private var smartSpaceCardTitle: TextView? = null
    
    private var smartSpaceController: OmniJawsSmartSpaceController? = null
    private var weatherController: WeatherViewController? = null
    
    /**
     * Initializes the provider with the keyguard view
     */
    fun init(view: View) {
        try {
            weatherIcon = view.findViewById(R.id.smartspace_weather_icon)
            weatherText = view.findViewById(R.id.smartspace_weather_text)
            smartSpaceCardIcon = view.findViewById(R.id.smartspace_card_icon)
            smartSpaceCardTitle = view.findViewById(R.id.smartspace_card_title)
            
            if (weatherIcon != null && weatherText != null) {
                if (weatherIcon is com.android.systemui.weather.WeatherImageView && 
                    weatherText is com.android.systemui.weather.WeatherTextView) {
                    weatherController = WeatherViewController(
                        context,
                        weatherIcon as com.android.systemui.weather.WeatherImageView,
                        weatherText as com.android.systemui.weather.WeatherTextView,
                        null
                    )
                    weatherController?.updateWeatherSettings()
                }
            }
            
            if (DEBUG) Log.d(TAG, "OmniJaws SmartSpace provider initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OmniJaws SmartSpace provider", e)
        }
    }
    
    /**
     * Add calendar or reminder event data to the SmartSpace card
     */
    fun updateSmartSpaceData(title: String?, icon: Drawable?, intent: Intent?) {
        Handler(Looper.getMainLooper()).post {
            try {
                if (!title.isNullOrEmpty()) {
                    smartSpaceCardTitle?.text = title
                    smartSpaceCardTitle?.visibility = View.VISIBLE
                    
                    if (icon != null) {
                        smartSpaceCardIcon?.setImageDrawable(icon)
                        smartSpaceCardIcon?.visibility = View.VISIBLE
                    } else {
                        smartSpaceCardIcon?.visibility = View.GONE
                    }
                } else {
                    smartSpaceCardTitle?.visibility = View.GONE
                    smartSpaceCardIcon?.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating SmartSpace data", e)
            }
        }
    }
    
    /**
     * Refresh the weather data
     */
    fun refresh() {
        weatherController?.updateWeatherSettings()
    }
    
    /**
     * Clean up resources when view is detached
     */
    fun onDestroy() {
        weatherController?.disableUpdates()
        weatherController?.removeObserver()
    }
}
