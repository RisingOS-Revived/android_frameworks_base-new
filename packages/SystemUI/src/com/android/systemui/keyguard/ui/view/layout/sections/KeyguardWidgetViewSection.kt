/*
 * Copyright (C) 2024 The Android Open Source Project
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
    
    // Use the widget ID that is actually in your R.id resources
    // For now, we'll use the same ID as in your original implementation
    private val KEYGUARD_WIDGETS_ID = R.id.keyguard_widgets

    override fun addViews(constraintLayout: ConstraintLayout) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        constraintLayout.findViewById<View?>(KEYGUARD_WIDGETS_ID)?.let { existingView ->
            // Remove from current parent if it exists
            (existingView.parent as? ViewGroup)?.removeView(existingView)
            
            // Add to constraint layout
            constraintLayout.addView(existingView)
            
            // Cast to LockScreenWidgets and store reference
            widgetView = existingView as? LockScreenWidgets
            
            // The view will handle initialization through its lifecycle methods
            // (onAttachedToWindow, onFinishInflate) since your implementation
            // uses the controller pattern with automatic callback registration
        }
    }

    override fun bindData(constraintLayout: ConstraintLayout) {
        // Data binding can be handled here if needed
        // Your current implementation handles this through the controller
    }

    override fun applyConstraints(constraintSet: ConstraintSet) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        constraintSet.apply {
            // Position widgets area to span full width
            connect(
                KEYGUARD_WIDGETS_ID,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                0,
            )
            connect(
                KEYGUARD_WIDGETS_ID,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                0
            )
            
            // Set height to wrap content
            constrainHeight(KEYGUARD_WIDGETS_ID, ConstraintSet.WRAP_CONTENT)
            
            // Try to position below the info widgets if the resource ID exists
            if (R.id.keyguard_info_widgets != 0) {
                connect(
                    KEYGUARD_WIDGETS_ID,
                    ConstraintSet.TOP,
                    R.id.keyguard_info_widgets,
                    ConstraintSet.BOTTOM,
                    16 // Add some margin
                )
            } else if (R.id.keyguard_slice_view != 0) {
                // Fall back to the slice view
                connect(
                    KEYGUARD_WIDGETS_ID,
                    ConstraintSet.TOP,
                    R.id.keyguard_slice_view,
                    ConstraintSet.BOTTOM,
                    16
                )
            } else {
                // Last resort: position below the status view
                connect(
                    KEYGUARD_WIDGETS_ID,
                    ConstraintSet.TOP,
                    R.id.keyguard_status_view,
                    ConstraintSet.BOTTOM,
                    16
                )
            }
            
            // Set elevation to ensure proper z-order
            setElevation(KEYGUARD_WIDGETS_ID, 2f)
        }
    }

    override fun removeViews(constraintLayout: ConstraintLayout) {
        widgetView?.let { view ->
            // Your LockScreenWidgets will handle cleanup through onDetachedFromWindow
            // which calls mViewController?.unregisterCallbacks()
            constraintLayout.removeView(view)
        }
        widgetView = null
    }
}
