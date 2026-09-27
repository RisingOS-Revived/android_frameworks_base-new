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

import android.content.res.ColorStateList
import android.os.CountDownTimer
import android.os.SystemClock
import android.uilatencystats.UiLatencyStatsManager
import android.util.Log
import android.util.PluralsMessageFormatter
import androidx.compose.ui.graphics.Color
import com.android.internal.util.LatencyTracker
import com.android.internal.widget.LockPatternUtils
import com.android.keyguard.KeyguardSecurityModel.SecurityMode
import com.android.systemui.authentication.shared.model.AuthenticationMethodModel
import com.android.systemui.bouncer.shared.model.BouncerMessageStrings
import com.android.systemui.bouncer.ui.helper.BouncerHapticPlayer
import com.android.systemui.classifier.FalsingCollector
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.res.R
import com.android.systemui.user.domain.interactor.SelectedUserInteractor
import com.android.systemui.util.wrapper.LockPatternCheckerWrapper
import java.time.Duration
import java.util.Optional
import kotlin.math.ceil

class KeyguardKnockCodeViewController
constructor(
    view: KeyguardKnockCodeView,
    keyguardUpdateMonitor: KeyguardUpdateMonitor,
    securityMode: SecurityMode,
    lockPatternUtils: LockPatternUtils,
    keyguardSecurityCallback: KeyguardSecurityCallback,
    messageAreaControllerFactory: KeyguardMessageAreaController.Factory,
    latencyTracker: LatencyTracker,
    private val falsingCollector: FalsingCollector,
    emergencyButtonController: EmergencyButtonController,
    featureFlags: FeatureFlags,
    selectedUserInteractor: SelectedUserInteractor,
    bouncerHapticPlayer: BouncerHapticPlayer,
    userActivityNotifier: UserActivityNotifier,
    lockPatternCheckerWrapper: LockPatternCheckerWrapper,
    uiLatencyStatsManager: Optional<UiLatencyStatsManager>,
) :
    KeyguardAbsKeyInputViewController<KeyguardKnockCodeView>(
        view,
        keyguardUpdateMonitor,
        securityMode,
        lockPatternUtils,
        keyguardSecurityCallback,
        messageAreaControllerFactory,
        latencyTracker,
        falsingCollector,
        emergencyButtonController,
        featureFlags,
        selectedUserInteractor,
        bouncerHapticPlayer,
        userActivityNotifier,
        lockPatternCheckerWrapper,
        uiLatencyStatsManager,
    ) {

    private var lockoutCountdown: CountDownTimer? = null

    override fun onViewAttached() {
        super.onViewAttached()
        mView.setOnSequenceCompleteListener { onSequenceComplete() }
        mView.setOnKnockTimedOutListener { onKnockTimedOut() }
        mView.setOnTapListener { onUserInput() }
        mView.uiState.onTouchDown = { falsingCollector.avoidGesture() }
        updatePadConfiguration()
        if (mView.uiState.message == null) {
            mView.uiState.showMessage(mView.context.getString(getInitialMessageResId()))
        }
    }

    override fun onResume(reason: Int) {
        super.onResume(reason)
        updatePadConfiguration()
    }

    override fun onPause() {
        super.onPause()
        lockoutCountdown?.cancel()
        lockoutCountdown = null
    }

    override fun reset() {
        super.reset()
        mView.uiState.clearMessage()
    }

    override fun resetState() {
        mView.setPasswordEntryEnabled(true)
        updatePadConfiguration()
    }

    override fun showMessage(message: CharSequence?, colorState: ColorStateList?, animated: Boolean) {
        mView.uiState.showMessage(message, colorState?.let { Color(it.defaultColor) })
    }

    override fun showPromptReason(reason: Int) {
        if (reason != KeyguardSecurityView.PROMPT_REASON_NONE) {
            val promptReasonStringRes = mView.getPromptReasonStringRes(reason)
            if (promptReasonStringRes != 0) {
                mView.uiState.showMessage(mView.context.getString(promptReasonStringRes))
            }
        }
    }

    override fun onUserInput() {
        super.onUserInput()
        mView.uiState.clearMessage()
    }

    override fun handleAttemptLockout(lockoutEndTime: Duration) {
        mView.setPasswordEntryEnabled(false)
        mView.setPasswordEntryInputEnabled(false)
        mLockedOut = true
        val secondsInFuture =
            ceil((lockoutEndTime.toMillis() - SystemClock.elapsedRealtime()) / 1000.0).toLong()
        getKeyguardSecurityCallback().onAttemptLockoutStart(secondsInFuture)
        val countdown =
            object : CountDownTimer(secondsInFuture * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = (millisUntilFinished / 1000.0).toLong()
                    val lockoutMessageModel =
                        BouncerMessageStrings.primaryAuthLockedOut(
                            AuthenticationMethodModel.Password,
                            secondsRemaining,
                        )
                    mView.uiState.showMessage(
                        PluralsMessageFormatter.format(
                            mView.resources,
                            lockoutMessageModel.primaryFormatterArgs(),
                            lockoutMessageModel.primaryMessage,
                        )
                    )
                }

                override fun onFinish() {
                    mView.uiState.clearMessage()
                    mLockedOut = false
                    resetState()
                }
            }
        lockoutCountdown = countdown
        countdown.start()
    }

    override fun onPasswordChecked(
        userId: Int,
        matched: Boolean,
        timeout: Duration,
        isValidPassword: Boolean,
        isDuplicate: Boolean,
    ) {
        super.onPasswordChecked(userId, matched, timeout, isValidPassword, isDuplicate)
        if (!matched && timeout.isZero) {
            mView.uiState.showMessage(
                mView.context.getString(mView.getWrongPasswordStringId(isDuplicate))
            )
        }
    }

    override fun startErrorAnimation() {
        mView.uiState.showError()
    }

    override fun getInitialMessageResId(): Int {
        return R.string.kg_knock_code_instructions
    }

    private fun updatePadConfiguration() {
        val userId = mSelectedUserInteractor.getSelectedUserId()
        val length = mLockPatternUtils.getKnockCodeLength(userId)
        val gridSize = mLockPatternUtils.getKnockCodeGridSize(userId)
        if (DEBUG) {
            Log.d(
                TAG,
                "knock pad for user $userId: ${gridSize}x$gridSize, $length taps",
            )
        }
        mView.setGridSize(gridSize)
        mView.setExpectedLength(length)
    }

    private fun onSequenceComplete() {
        val length =
            mLockPatternUtils.getKnockCodeLength(mSelectedUserInteractor.getSelectedUserId())
        if (mView.uiState.tapCount != length) {
            if (DEBUG) {
                Log.d(
                    TAG,
                    "pad was set to ${mView.uiState.tapCount} taps but the stored code is $length; "
                        + "reconfiguring instead of checking",
                )
            }
            updatePadConfiguration()
            return
        }
        verifyPasswordAndUnlock()
    }

    private fun onKnockTimedOut() {
        if (DEBUG) Log.d(TAG, "partial knock sequence discarded after timeout")
        if (!mLockedOut) {
            mView.uiState.showMessage(mView.context.getString(R.string.kg_knock_code_instructions))
        }
    }

    private companion object {
        const val TAG = "KeyguardKnockCodeViewController"
        val DEBUG = KeyguardConstants.DEBUG
    }
}
