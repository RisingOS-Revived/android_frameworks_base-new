/*
 * Copyright (C) 2023-2024 The risingOS Android Project
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
package com.android.systemui.keyguard.ui.view.layout.sections
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Barrier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.systemui.keyguard.MigrateClocksToBlueprint
import com.android.systemui.keyguard.shared.model.KeyguardSection
import com.android.systemui.res.R
import javax.inject.Inject

class InfoWidgetsSection
@Inject
constructor(
    private val context: Context,
) : KeyguardSection() {
    
    private var infoWidgetsView: View? = null
    
    override fun addViews(constraintLayout: ConstraintLayout) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        // Remove existing view with the same ID if it exists
        constraintLayout.findViewById<View?>(R.id.keyguard_info_widgets)?.let { existingView ->
            (existingView.parent as? ViewGroup)?.removeView(existingView)
        }
        
        // Inflate the info widgets layout
        infoWidgetsView = LayoutInflater.from(context).inflate(
            R.layout.keyguard_info_widgets,
            constraintLayout,
            false
        ).apply {
            id = R.id.keyguard_info_widgets
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        constraintLayout.addView(infoWidgetsView)
    }
    
    override fun bindData(constraintLayout: ConstraintLayout) {
        // The ProgressImageView components handle their own data binding
        // through their onAttachedToWindow/onDetachedFromWindow lifecycle
    }
    
    override fun applyConstraints(constraintSet: ConstraintSet) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        constraintSet.apply {
            // Position the info widgets below the smart space barrier (which is either the clock or other elements)
            connect(
                R.id.keyguard_info_widgets,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            connect(
                R.id.keyguard_info_widgets,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
            
            // Connect to the bottom of the smart space barrier created by the clock sections
            connect(
                R.id.keyguard_info_widgets,
                ConstraintSet.TOP,
                R.id.smart_space_barrier_bottom,
                ConstraintSet.BOTTOM
            )
            
            // Set appropriate margins
            setMargin(R.id.keyguard_info_widgets, ConstraintSet.TOP, 24)
            setMargin(R.id.keyguard_info_widgets, ConstraintSet.START, 16)
            setMargin(R.id.keyguard_info_widgets, ConstraintSet.END, 16)
            
            // Set height to wrap content
            constrainHeight(R.id.keyguard_info_widgets, ConstraintSet.WRAP_CONTENT)
            constrainWidth(R.id.keyguard_info_widgets, ConstraintSet.MATCH_CONSTRAINT)
            
            // Set the elevation to ensure proper layering
            setElevation(R.id.keyguard_info_widgets, 1f)
            
            // Create a barrier for notifications to reference
            createBarrier(
                R.id.info_widgets_barrier_bottom,
                Barrier.BOTTOM,
                0,
                *intArrayOf(R.id.keyguard_info_widgets)
            )
        }
    }
    
    override fun removeViews(constraintLayout: ConstraintLayout) {
        infoWidgetsView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        infoWidgetsView = null
    }
}
