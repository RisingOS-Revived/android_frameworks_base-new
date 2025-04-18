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

import com.android.systemui.display.data.repository.displayRepository
import com.android.systemui.kosmos.Kosmos
import com.android.systemui.kosmos.applicationCoroutineScope
import org.mockito.kotlin.mock

val Kosmos.fakeStatusBarModePerDisplayRepository by
    Kosmos.Fixture { FakeStatusBarModePerDisplayRepository() }

val Kosmos.statusBarModeRepository: StatusBarModeRepositoryStore by
    Kosmos.Fixture { fakeStatusBarModeRepository }
val Kosmos.fakeStatusBarModeRepository by Kosmos.Fixture { FakeStatusBarModeRepository() }
val Kosmos.fakeStatusBarModePerDisplayRepositoryFactory by
    Kosmos.Fixture { FakeStatusBarModePerDisplayRepositoryFactory() }

val Kosmos.multiDisplayStatusBarModeRepositoryStore by
    Kosmos.Fixture {
        MultiDisplayStatusBarModeRepositoryStore(
            applicationCoroutineScope,
            fakeStatusBarModePerDisplayRepositoryFactory,
            displayRepository,
        )
    }

class FakeStatusBarModePerDisplayRepositoryFactory : StatusBarModePerDisplayRepositoryFactory {

    override fun create(displayId: Int): StatusBarModePerDisplayRepositoryImpl {
        return mock<StatusBarModePerDisplayRepositoryImpl>()
    }
}
