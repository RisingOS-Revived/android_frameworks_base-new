@file:Suppress("DEPRECATION")
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

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.Slice
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.SliceAction
import com.android.internal.util.android.OmniJawsClient
import com.android.systemui.keyguard.KeyguardSliceProvider
import com.android.systemui.res.R
import java.lang.ref.WeakReference

/**
 * OmniJaws implementation for Keyguard slice provider
 */
class KeyguardSliceProviderOmniJaws : KeyguardSliceProvider(), OmniJawsClient.OmniJawsObserver {

    private val TAG = "KeyguardSliceOmniJaws"
    private val DEBUG = false
    
    private val weatherClient by lazy { OmniJawsClient(context!!) }
    private var weatherInfo: OmniJawsClient.WeatherInfo? = null
    private var weatherIconWithShadow: Bitmap? = null
    private var hideSensitiveContent = false
    
    override fun onCreateSliceProvider(): Boolean {
        val created = super.onCreateSliceProvider()
        if (created) {
            weatherClient.addObserver(this)
            queryAndUpdateWeather()
        }
        return created
    }
    
    override fun onDestroy() {
        super.onDestroy()
        weatherClient.removeObserver(this)
    }
    
    override fun onBindSlice(sliceUri: Uri): Slice {
        val builder = ListBuilder(context!!, mSliceUri, ListBuilder.INFINITY)
        synchronized(this) {
            if (needsMediaLocked()) {
                addMediaLocked(builder)
            } else {
                builder.addRow(
                    ListBuilder.RowBuilder(mDateUri)
                        .setTitle(formattedDateLocked)
                )
            }
            
            addWeather(builder)
            addNextAlarmLocked(builder)
            addZenModeLocked(builder)
            addPrimaryActionLocked(builder)
            
            return builder.build()
        }
    }
    
    private fun addWeather(listBuilder: ListBuilder) {
        weatherInfo?.let { info ->
            if (!info.city.isNullOrEmpty() && weatherIconWithShadow != null) {
                val formattedTemperature = "${info.temp}${info.tempUnits}"
                val weatherText = if (hideSensitiveContent) {
                    formattedTemperature
                } else {
                    "$formattedTemperature · ${info.city}"
                }
                
                val builder = ListBuilder.RowBuilder(weatherUri)
                    .setTitle(weatherText)
                
                val icon = IconCompat.createWithBitmap(weatherIconWithShadow!!)
                builder.addEndItem(icon, ListBuilder.SMALL_IMAGE)
                
                // Add click action if available - using settingsIntent instead of weatherStatusIntent
                weatherClient.settingsIntent?.let { intent ->
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                    )
                    val sliceAction = SliceAction(
                        pendingIntent,
                        icon,
                        ListBuilder.SMALL_IMAGE,
                        info.condition
                    )
                    builder.setPrimaryAction(sliceAction)
                }
                
                listBuilder.addRow(builder)
            }
        }
    }
    
    private fun updateSensitiveContent(hideSensitive: Boolean) {
        var notify = false
        synchronized(this) {
            if (hideSensitiveContent != hideSensitive) {
                hideSensitiveContent = hideSensitive
                if (DEBUG) Log.d(TAG, "Public mode changed, hide data: $hideSensitive")
                notify = true
            }
        }
        if (notify) notifyChange()
    }
    
    override fun weatherError(errorReason: Int) {
        if (errorReason == OmniJawsClient.EXTRA_ERROR_DISABLED) {
            weatherInfo = null
            weatherIconWithShadow = null
            notifyChange()
        }
    }
    
    override fun weatherUpdated() {
        queryAndUpdateWeather()
    }
    
    private fun queryAndUpdateWeather() {
        try {
            weatherClient.queryWeather()
            weatherInfo = weatherClient.weatherInfo
            
            if (weatherInfo != null) {
                val icon = weatherClient.getWeatherConditionImage(weatherInfo!!.conditionCode)
                if (icon != null) {
                    AddWeatherIconShadowTask(this).execute(icon)
                } else {
                    notifyChange()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating weather", e)
            notifyChange()
        }
    }
    
    fun onWeatherIconWithShadowReady(bitmap: Bitmap) {
        weatherIconWithShadow = bitmap
        notifyChange()
    }
    
    class AddWeatherIconShadowTask(provider: KeyguardSliceProviderOmniJaws) : AsyncTask<Drawable, Void, Bitmap>() {
        private val providerReference = WeakReference(provider)
        private val blurRadius = 4f // 4dp fallback
        
        override fun doInBackground(vararg drawables: Drawable?): Bitmap {
            val drawable = drawables[0] ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            
            val bitmapWidth = drawable.intrinsicWidth
            val bitmapHeight = drawable.intrinsicHeight
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            shadowPaint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            
            val offset = IntArray(2)
            val alpha = bitmap.extractAlpha(shadowPaint, offset)
            
            val result = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )
            
            val resultCanvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.alpha = 70
            resultCanvas.drawBitmap(
                alpha,
                offset[0].toFloat(),
                offset[1].toFloat() + blurRadius / 2f,
                paint
            )
            alpha.recycle()
            
            paint.alpha = 255
            resultCanvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            return result
        }
        
        override fun onPostExecute(result: Bitmap?) {
            result?.let { bitmap ->
                providerReference.get()?.onWeatherIconWithShadowReady(bitmap)
            }
        }
    }
    
    companion object {
        private val weatherUri = Uri.parse("content://com.android.systemui.keyguard/weather")
    }
}
