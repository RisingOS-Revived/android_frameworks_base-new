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
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.keyguard.NowBarHolder
import com.android.systemui.keyguard.MigrateClocksToBlueprint
import com.android.systemui.keyguard.shared.model.KeyguardSection
import com.android.systemui.res.R
import javax.inject.Inject

class NowBarSection
@Inject
constructor(
    private val context: Context,
) : KeyguardSection() {

    private var nowBarHolder: NowBarHolder? = null

    override fun addViews(constraintLayout: ConstraintLayout) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        // Remove existing view with the same ID if it exists (regardless of type)
        constraintLayout.findViewById<View?>(R.id.now_bar_area)?.let { existingView ->
            (existingView.parent as? ViewGroup)?.removeView(existingView)
        }

        // Create and add new NowBarHolder
        nowBarHolder = NowBarHolder(context).apply {
            id = R.id.now_bar_area
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        constraintLayout.addView(nowBarHolder)
    }

    override fun bindData(constraintLayout: ConstraintLayout) {
        // No specific data binding needed for NowBar
    }

    override fun applyConstraints(constraintSet: ConstraintSet) {
        if (!MigrateClocksToBlueprint.isEnabled) return
        
        constraintSet.apply {
            // Position the now bar at the bottom of the screen
            connect(
                R.id.now_bar_area,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            connect(
                R.id.now_bar_area,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
            connect(
                R.id.now_bar_area,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )
            
            // Set height to wrap content
            constrainHeight(R.id.now_bar_area, ConstraintSet.WRAP_CONTENT)
            constrainWidth(R.id.now_bar_area, ConstraintSet.MATCH_CONSTRAINT)
        }
    }

    override fun removeViews(constraintLayout: ConstraintLayout) {
        nowBarHolder?.let { holder ->
            (holder.parent as? ViewGroup)?.removeView(holder)
        }
        nowBarHolder = null
    }
}
