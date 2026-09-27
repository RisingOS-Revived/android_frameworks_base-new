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

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.android.compose.theme.PlatformTheme
import com.android.internal.widget.LockscreenCredential
import com.android.systemui.res.R

class KeyguardKnockCodeView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    KeyguardAbsKeyInputView(context, attrs) {

    val uiState = KnockCodeUiState()

    private var pendingSequence: String? = null

    private var onSequenceCompleteListener: Runnable? = null

    private var onKnockTimedOutListener: Runnable? = null

    private var onTapListener: Runnable? = null

    override fun onFinishInflate() {
        super.onFinishInflate()

        uiState.onSequenceComplete = { sequence ->
            pendingSequence = sequence
            onSequenceCompleteListener?.run()
        }
        uiState.onTimedOut = { onKnockTimedOutListener?.run() }
        uiState.onTap = { onTapListener?.run() }

        val host = requireViewById<FrameLayout>(R.id.knock_code_compose_host)
        host.addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    PlatformTheme(isDarkTheme = true) {
                        KeyguardKnockCodeScreen(uiState, Modifier.fillMaxSize())
                    }
                }
            },
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    fun setExpectedLength(expectedLength: Int) {
        uiState.configure(gridSize = uiState.gridSize, expectedLength = expectedLength)
    }

    fun setGridSize(gridSize: Int) {
        uiState.configure(gridSize = gridSize, expectedLength = uiState.expectedLength)
    }

    fun setOnSequenceCompleteListener(listener: Runnable?) {
        onSequenceCompleteListener = listener
    }

    fun setOnKnockTimedOutListener(listener: Runnable?) {
        onKnockTimedOutListener = listener
    }

    fun setOnTapListener(listener: Runnable?) {
        onTapListener = listener
    }

    override fun getPasswordTextViewId(): Int {
        return 0
    }

    public override fun getPromptReasonStringRes(reason: Int): Int =
        when (reason) {
            KeyguardSecurityView.PROMPT_REASON_RESTART -> R.string.kg_knock_code_prompt_restart
            KeyguardSecurityView.PROMPT_REASON_TIMEOUT -> R.string.kg_knock_code_prompt_timeout
            KeyguardSecurityView.PROMPT_REASON_DEVICE_ADMIN ->
                R.string.kg_knock_code_prompt_device_admin
            KeyguardSecurityView.PROMPT_REASON_PREPARE_FOR_UPDATE ->
                R.string.kg_knock_code_prompt_prepare_for_update
            KeyguardSecurityView.PROMPT_REASON_USER_REQUEST ->
                R.string.kg_knock_code_prompt_user_request
            KeyguardSecurityView.PROMPT_REASON_NON_STRONG_BIOMETRIC_TIMEOUT ->
                R.string.kg_knock_code_prompt_non_strong_biometric_timeout
            else -> R.string.kg_knock_code_instructions
        }

    public override fun getWrongPasswordStringId(isDuplicate: Boolean): Int =
        if (isDuplicate) R.string.kg_knock_code_duplicate_guess else R.string.kg_knock_code_wrong

    override fun resetState() {
        pendingSequence = null
        uiState.clearSequence()
    }

    override fun resetPasswordText(animate: Boolean, announce: Boolean) {
        pendingSequence = null
        uiState.clearSequence()
    }

    override fun getEnteredCredential(): LockscreenCredential {
        return LockscreenCredential.createPin(pendingSequence.orEmpty())
    }

    public override fun setPasswordEntryEnabled(enabled: Boolean) {
        uiState.setInputEnabled(enabled)
    }

    public override fun setPasswordEntryInputEnabled(enabled: Boolean) {
        uiState.setInputEnabled(enabled)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
            else -> return true
        }
    }

    public override fun disallowBouncerSwipe(): Boolean = true

    public override fun getTitle(): CharSequence =
        resources.getString(R.string.keyguard_accessibility_knock_code_area)
}
