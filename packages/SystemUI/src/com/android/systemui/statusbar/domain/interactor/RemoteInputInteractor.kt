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

package com.android.systemui.statusbar.domain.interactor

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.data.repository.RemoteInputRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Interactor used for business logic pertaining to the notification remote input (e.g. when the
 * user presses "reply" on a notification and the keyboard opens).
 */
@SysUISingleton
class RemoteInputInteractor
@Inject
constructor(private val remoteInputRepository: RemoteInputRepository) {
    /** Is remote input currently active for a notification? */
    val isRemoteInputActive: Flow<Boolean> = remoteInputRepository.isRemoteInputActive

    /** The bottom bound of the currently focused remote input notification row. */
    val remoteInputRowBottomBound: Flow<Float> =
        remoteInputRepository.remoteInputRowBottomBound.mapNotNull { it }

    fun setRemoteInputRowBottomBound(bottom: Float?) {
        remoteInputRepository.setRemoteInputRowBottomBound(bottom)
    }

    /** Close any active remote inputs */
    fun closeRemoteInputs() {
        remoteInputRepository.closeRemoteInputs()
    }
}
