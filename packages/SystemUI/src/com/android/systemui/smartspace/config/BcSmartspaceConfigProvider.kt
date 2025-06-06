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

package com.android.systemui.smartspace.config

import com.android.systemui.Flags.smartspaceSwipeEventLoggingFix
import com.android.systemui.Flags.smartspaceViewpager2
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.plugins.BcSmartspaceConfigPlugin

class BcSmartspaceConfigProvider(private val featureFlags: FeatureFlags) :
    BcSmartspaceConfigPlugin {
    override val isDefaultDateWeatherDisabled: Boolean
        get() = true

    override val isViewPager2Enabled: Boolean
        get() = smartspaceViewpager2()

    override val isSwipeEventLoggingEnabled: Boolean
        get() = smartspaceSwipeEventLoggingFix()
}
