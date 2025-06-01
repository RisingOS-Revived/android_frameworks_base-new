/*
 * Copyright (C) 2025 the RisingOS Revived Android Project
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
 *
 */
package com.android.systemui.keyguard.ui.view.layout.sections

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.systemui.keyguard.MigrateClocksToBlueprint
import com.android.systemui.keyguard.shared.model.KeyguardSection
import com.android.systemui.res.R
import javax.inject.Inject
import com.android.systemui.lockscreen.LockScreenWidgets

class KeyguardWidgetViewSection
@Inject
constructor(
    private val context: Context,
) : KeyguardSection() {

    private var widgetView: LockScreenWidgets? = null

    override fun addViews(constraintLayout: ConstraintLayout) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        constraintLayout.findViewById<View?>(R.id.keyguard_widgets)?.let { existingView ->
            (existingView.parent as? ViewGroup)?.removeView(existingView)
            constraintLayout.addView(existingView)
            widgetView = existingView as? LockScreenWidgets
            
            existingView.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun bindData(constraintLayout: ConstraintLayout) {
        // Data binding handled through controller pattern
    }

    override fun applyConstraints(constraintSet: ConstraintSet) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        constraintSet.apply {
            // LS widgets positioning - below INFO WIDGETS (4th in hierarchy)
            connect(R.id.keyguard_widgets, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(R.id.keyguard_widgets, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            
            // Chain to info widgets (primary) or fallback hierarchy
            when {
                constraintSet.getConstraint(R.id.keyguard_info_widgets) != null -> {
                    connect(R.id.keyguard_widgets, ConstraintSet.TOP, R.id.keyguard_info_widgets, ConstraintSet.BOTTOM, 8)
                }
                constraintSet.getConstraint(R.id.keyguard_weather) != null -> {
                    connect(R.id.keyguard_widgets, ConstraintSet.TOP, R.id.keyguard_weather, ConstraintSet.BOTTOM, 8)
                }
                constraintSet.getConstraint(R.id.default_weather_image) != null -> {
                    connect(R.id.keyguard_widgets, ConstraintSet.TOP, R.id.default_weather_image, ConstraintSet.BOTTOM, 8)
                }
                constraintSet.getConstraint(R.id.clock_ls) != null -> {
                    connect(R.id.keyguard_widgets, ConstraintSet.TOP, R.id.clock_ls, ConstraintSet.BOTTOM, 8)
                }
                constraintSet.getConstraint(R.id.keyguard_slice_view) != null -> {
                    connect(R.id.keyguard_widgets, ConstraintSet.TOP, R.id.keyguard_slice_view, ConstraintSet.BOTTOM, 8)
                }
                else -> {
                    connect(R.id.keyguard_widgets, ConstraintSet.TOP, R.id.lockscreen_clock_view, ConstraintSet.BOTTOM, 8)
                }
            }
            
            constrainHeight(R.id.keyguard_widgets, ConstraintSet.WRAP_CONTENT)
            constrainWidth(R.id.keyguard_widgets, ConstraintSet.MATCH_CONSTRAINT)
            setMargin(R.id.keyguard_widgets, ConstraintSet.START, 0)
            setMargin(R.id.keyguard_widgets, ConstraintSet.END, 0)
            setElevation(R.id.keyguard_widgets, 2f)
            
            // UNIFIED BARRIER - Create barrier in every section that could be last
            createUnifiedBarrierAndNotificationConstraints(constraintSet)
        }
    }
    
    private fun createUnifiedBarrierAndNotificationConstraints(constraintSet: ConstraintSet) {
        constraintSet.apply {
            // UNIFIED BARRIER - All elements above notifications (FINAL barrier)
            createBarrier(
                R.id.smart_space_barrier_bottom,
                Barrier.BOTTOM,
                0,
                *intArrayOf(
                    R.id.keyguard_slice_view,
                    R.id.keyguard_weather,
                    R.id.default_weather_image,
                    R.id.default_weather_text,
                    R.id.clock_ls,
                    R.id.keyguard_info_widgets,
                    R.id.keyguard_widgets,
                    R.id.lockscreen_clock_view // Include fallback clock
                )
            )
            
            // Notifications positioned below EVERYTHING via barrier
            if (constraintSet.getConstraint(R.id.left_aligned_notification_icon_container) != null) {
                connect(
                    R.id.left_aligned_notification_icon_container,
                    ConstraintSet.TOP,
                    R.id.smart_space_barrier_bottom,
                    ConstraintSet.BOTTOM,
                    context.resources.getDimensionPixelSize(R.dimen.below_clock_padding_start_icons)
                )
            }
        }
    }

    override fun removeViews(constraintLayout: ConstraintLayout) {
        widgetView?.let { view ->
            constraintLayout.removeView(view)
        }
        widgetView = null
    }
}
