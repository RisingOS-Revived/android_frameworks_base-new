/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.systemui.statusbar.ui

import android.content.Context
import com.android.internal.policy.SystemBarUtils
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.res.R
import dagger.Binds
import javax.inject.Inject
import kotlin.math.max

/**
 * Proxy interface to [SystemBarUtils], allowing injection of different logic for testing.
 *
 * Developers should almost always prefer [SystemBarUtilsState] instead.
 */
interface SystemBarUtilsProxy {
    fun getStatusBarHeight(): Int
    fun getStatusBarHeaderHeightKeyguard(): Int
}

class SystemBarUtilsProxyImpl
@Inject
constructor(
    @Application private val context: Context,
) : SystemBarUtilsProxy {
    override fun getStatusBarHeight(): Int = SystemBarUtils.getStatusBarHeight(context)
    override fun getStatusBarHeaderHeightKeyguard(): Int {
        val cutout = context.display.cutout
        val ignoreLockscreenCutout = context.resources.getBoolean(R.bool.kg_ignore_lockscreen_cutout)
        val waterfallInsetTop = if (cutout == null) 0 else cutout.waterfallInsets.top
        val statusBarHeaderHeightKeyguard =
            context.resources.getDimensionPixelSize(R.dimen.status_bar_header_height_keyguard)
        val defaultHeight = max(getStatusBarHeight(), statusBarHeaderHeightKeyguard + waterfallInsetTop)
        return if (ignoreLockscreenCutout) statusBarHeaderHeightKeyguard else defaultHeight
    }

    @dagger.Module
    interface Module {
        @Binds fun bindImpl(impl: SystemBarUtilsProxyImpl): SystemBarUtilsProxy
    }
}
