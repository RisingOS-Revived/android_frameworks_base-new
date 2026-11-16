/*
 * Copyright (C) 2026 RisingOS (revived) Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.android.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.keyguard.ui.animation

import android.view.animation.PathInterpolator

object AodContentMotion {
    val UNLOCK_EASING = PathInterpolator(0.22f, 0f, 0.18f, 1f)

    fun progress(linear: Float): Float {
        val t = if (linear.isFinite()) linear.coerceIn(0f, 1f) else 0f
        return t * t * (3f - 2f * t)
    }

    fun clockScale(progress: Float): Float = 1f - 0.08f * progress
    fun widgetScale(progress: Float): Float = 1f - 0.04f * progress
    fun clockAlpha(progress: Float, opacity: Float, aodOpacity: Float): Float =
        opacity + (minOf(opacity, aodOpacity) - opacity) * progress

    fun awakeAlpha(linear: Float): Float = 1f - progress(linear / 0.65f)
}
