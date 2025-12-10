/*
 * Copyright (C) 2025-2026 RisingOS (Revived) Android Project
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

package com.android.systemui.statusbar.pipeline.wifi.ui.dagger

import com.android.systemui.CoreStartable
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.pipeline.wifi.ui.WifiIconBroadcastReceiver
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Inject

@Module
interface WifiIconModule {
    @Binds
    @IntoMap
    @ClassKey(WifiIconStartable::class)
    fun bindWifiIconStartable(impl: WifiIconStartable): CoreStartable
}

@SysUISingleton
class WifiIconStartable @Inject constructor(
    private val wifiIconBroadcastReceiver: WifiIconBroadcastReceiver
) : CoreStartable {
    override fun start() {
    }
}
