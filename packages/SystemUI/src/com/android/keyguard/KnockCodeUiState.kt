/*
 * Copyright (C) 2026 RisingOS (revived) Android Project
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

package com.android.keyguard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.android.internal.widget.LockPatternUtils

class KnockCodeUiState {

    var gridSize by mutableIntStateOf(LockPatternUtils.KNOCK_CODE_GRID_SIZE_DEFAULT)
        private set

    var expectedLength by mutableIntStateOf(0)
        private set

    var tapCount by mutableIntStateOf(0)
        private set

    var inputEnabled by mutableStateOf(true)
        @JvmName("setInputEnabledInternal") private set

    var message by mutableStateOf<CharSequence?>(null)
        private set

    var messageColor by mutableStateOf<Color?>(null)
        private set

    var errorSignal by mutableIntStateOf(0)
        private set

    var onTap: (() -> Unit)? = null

    var onTouchDown: (() -> Unit)? = null

    var onSequenceComplete: ((String) -> Unit)? = null

    var onTimedOut: (() -> Unit)? = null

    private val sequence = StringBuilder()

    fun configure(gridSize: Int, expectedLength: Int) {
        if (this.gridSize == gridSize && this.expectedLength == expectedLength) {
            return
        }
        this.gridSize = gridSize
        this.expectedLength = expectedLength
        clearSequence()
    }

    fun setInputEnabled(enabled: Boolean) {
        if (inputEnabled == enabled) {
            return
        }
        inputEnabled = enabled
        if (!enabled) {
            clearSequence()
        }
    }

    fun clearSequence() {
        sequence.setLength(0)
        tapCount = 0
    }

    fun recordTap(row: Int, col: Int): Boolean {
        if (!inputEnabled || expectedLength <= 0 || tapCount >= expectedLength) {
            return false
        }

        sequence.append(LockPatternUtils.encodeKnockCodeCell(gridSize, row, col))
        tapCount++
        onTap?.invoke()

        if (tapCount >= expectedLength) {
            onSequenceComplete?.invoke(sequence.toString())
            clearSequence()
        }
        return true
    }

    fun onTapTimeout() {
        if (tapCount == 0) {
            return
        }
        clearSequence()
        onTimedOut?.invoke()
    }

    fun showMessage(message: CharSequence?, color: Color? = null) {
        this.message = message
        messageColor = color
    }

    fun clearMessage() {
        showMessage(null)
    }

    fun showError() {
        errorSignal++
    }

    companion object {
        const val TAP_TIMEOUT_MS = 2000L
    }
}
