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
 */

package com.android.systemui.statusbar.data.repository

import android.view.Display
import com.android.systemui.statusbar.events.PrivacyDotWindowController
import org.mockito.kotlin.mock

class FakePrivacyDotWindowControllerStore : PrivacyDotWindowControllerStore {

    private val perDisplayMockControllers = mutableMapOf<Int, PrivacyDotWindowController>()

    override val defaultDisplay: PrivacyDotWindowController
        get() = forDisplay(Display.DEFAULT_DISPLAY)

    override fun forDisplay(displayId: Int): PrivacyDotWindowController {
        return perDisplayMockControllers.computeIfAbsent(displayId) { mock() }
    }
}
