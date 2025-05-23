/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.systemui.screenrecord

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.os.UserHandle
import android.view.Display
import android.view.MotionEvent.ACTION_MOVE
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import com.android.systemui.Prefs
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.mediaprojection.MediaProjectionCaptureTarget
import com.android.systemui.mediaprojection.MediaProjectionMetricsLogger
import com.android.systemui.mediaprojection.appselector.MediaProjectionAppSelectorActivity
import com.android.systemui.mediaprojection.permission.BaseMediaProjectionPermissionDialogDelegate
import com.android.systemui.mediaprojection.permission.BaseMediaProjectionPermissionViewBinder
import com.android.systemui.mediaprojection.permission.ENTIRE_SCREEN
import com.android.systemui.mediaprojection.permission.SINGLE_APP
import com.android.systemui.mediaprojection.permission.ScreenShareMode
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.res.R
import com.android.systemui.settings.UserContextProvider
import com.android.systemui.statusbar.phone.SystemUIDialog
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Dialog to select screen recording options */
class ScreenRecordPermissionDialogDelegate(
    private val hostUserHandle: UserHandle,
    private val hostUid: Int,
    private val controller: RecordingController,
    private val activityStarter: ActivityStarter,
    private val userContextProvider: UserContextProvider,
    private val onStartRecordingClicked: Runnable?,
    private val mediaProjectionMetricsLogger: MediaProjectionMetricsLogger,
    private val systemUIDialogFactory: SystemUIDialog.Factory,
    @ScreenShareMode defaultSelectedMode: Int,
    @StyleRes private val theme: Int,
    private val context: Context,
    private val displayManager: DisplayManager,
) :
    BaseMediaProjectionPermissionDialogDelegate<SystemUIDialog>(
        ScreenRecordPermissionViewBinder.createOptionList(displayManager),
        appName = null,
        hostUid = hostUid,
        mediaProjectionMetricsLogger,
        R.drawable.ic_screenrecord,
        R.color.screenrecord_icon_color,
        defaultSelectedMode,
    ),
    SystemUIDialog.Delegate {
    @AssistedInject
    constructor(
        @Assisted hostUserHandle: UserHandle,
        @Assisted hostUid: Int,
        @Assisted controller: RecordingController,
        activityStarter: ActivityStarter,
        userContextProvider: UserContextProvider,
        @Assisted onStartRecordingClicked: Runnable?,
        mediaProjectionMetricsLogger: MediaProjectionMetricsLogger,
        systemUIDialogFactory: SystemUIDialog.Factory,
        @Application context: Context,
        displayManager: DisplayManager,
    ) : this(
        hostUserHandle,
        hostUid,
        controller,
        activityStarter,
        userContextProvider,
        onStartRecordingClicked,
        mediaProjectionMetricsLogger,
        systemUIDialogFactory,
        defaultSelectedMode = ENTIRE_SCREEN,
        theme = SystemUIDialog.DEFAULT_THEME,
        context,
        displayManager,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            recordingController: RecordingController,
            hostUserHandle: UserHandle,
            hostUid: Int,
            onStartRecordingClicked: Runnable?,
        ): ScreenRecordPermissionDialogDelegate
    }

    private lateinit var tapsSwitch: Switch
    private lateinit var audioSwitch: Switch
    private lateinit var lowQualitySwitch: Switch
    private lateinit var longerDurationSwitch: Switch
    private lateinit var skipTimeSwitch: Switch
    private lateinit var hevcSwitch: Switch
    private lateinit var options: Spinner

    override fun createViewBinder(): BaseMediaProjectionPermissionViewBinder {
        return ScreenRecordPermissionViewBinder(
            hostUid,
            mediaProjectionMetricsLogger,
            defaultSelectedMode,
            displayManager,
            dialog,
        )
    }

    override fun createDialog(): SystemUIDialog {
        return systemUIDialogFactory.create(this, context, theme)
    }

    override fun onCreate(dialog: SystemUIDialog, savedInstanceState: Bundle?) {
        super<BaseMediaProjectionPermissionDialogDelegate>.onCreate(dialog, savedInstanceState)
        setDialogTitle(R.string.screenrecord_permission_dialog_title)
        dialog.setTitle(R.string.screenrecord_title)
        setStartButtonOnClickListener { v: View? ->
            onStartRecordingClicked?.run()
            val selectedScreenShareOption = getSelectedScreenShareOption()
            if (selectedScreenShareOption.mode == ENTIRE_SCREEN) {
                requestScreenCapture(/* captureTarget= */ null, selectedScreenShareOption.displayId)
            }
            if (selectedScreenShareOption.mode == SINGLE_APP) {
                val intent = Intent(dialog.context, MediaProjectionAppSelectorActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // We can't start activity for result here so we use result receiver to get
                // the selected target to capture
                intent.putExtra(
                    MediaProjectionAppSelectorActivity.EXTRA_CAPTURE_REGION_RESULT_RECEIVER,
                    CaptureTargetResultReceiver(),
                )

                intent.putExtra(
                    MediaProjectionAppSelectorActivity.EXTRA_HOST_APP_USER_HANDLE,
                    hostUserHandle,
                )
                intent.putExtra(MediaProjectionAppSelectorActivity.EXTRA_HOST_APP_UID, hostUid)
                intent.putExtra(
                    MediaProjectionAppSelectorActivity.EXTRA_SCREEN_SHARE_TYPE,
                    MediaProjectionAppSelectorActivity.ScreenShareType.ScreenRecord.name,
                )
                activityStarter.startActivity(intent, /* dismissShade= */ true)
            }
            dialog.dismiss()
        }
        setCancelButtonOnClickListener { dialog.dismiss() }
        initRecordOptionsView()
    }

    @LayoutRes override fun getOptionsViewLayoutId(): Int = R.layout.screen_record_options

    @SuppressLint("ClickableViewAccessibility")
    private fun initRecordOptionsView() {
        // TODO(b/378514312): Move this function to ScreenRecordPermissionViewBinder
        audioSwitch = dialog.requireViewById(R.id.screenrecord_audio_switch)
        tapsSwitch = dialog.requireViewById(R.id.screenrecord_taps_switch)
        lowQualitySwitch = dialog.requireViewById(R.id.screenrecord_lowquality_switch)
        longerDurationSwitch = dialog.requireViewById(R.id.screenrecord_longer_timeout_switch)
        skipTimeSwitch = dialog.requireViewById(R.id.screenrecord_skip_time_switch)
        hevcSwitch = dialog.requireViewById(R.id.screenrecord_hevc_switch)

        // Add these listeners so that the switch only responds to movement
        // within its target region, to meet accessibility requirements
        audioSwitch.setOnTouchListener { _, event -> event.action == ACTION_MOVE }
        tapsSwitch.setOnTouchListener { _, event -> event.action == ACTION_MOVE }
        lowQualitySwitch.setOnTouchListener { _, event -> event.action == ACTION_MOVE }
        longerDurationSwitch.setOnTouchListener { _, event -> event.action == ACTION_MOVE }
        skipTimeSwitch.setOnTouchListener { _, event -> event.action == ACTION_MOVE }
        hevcSwitch.setOnTouchListener { _, event -> event.action == ACTION_MOVE }

        options = dialog.requireViewById(R.id.screen_recording_options)
        val a: ArrayAdapter<*> =
            ScreenRecordingAdapter(
                dialog.context,
                android.R.layout.simple_spinner_dropdown_item,
                MODES,
            )
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        options.adapter = a
        options.setOnItemClickListenerInt { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
            audioSwitch.isChecked = true
        }

        // disable redundant Touch & Hold accessibility action for Switch Access
        options.accessibilityDelegate =
            object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK)
                    super.onInitializeAccessibilityNodeInfo(host, info)
                }
            }
        options.isLongClickable = false

        loadPrefs();
    }

    /**
     * Starts screen capture after some countdown
     *
     * @param captureTarget target to capture (could be e.g. a task) or null to record the whole
     *   screen
     */
    private fun requestScreenCapture(
        captureTarget: MediaProjectionCaptureTarget?,
        displayId: Int = Display.DEFAULT_DISPLAY,
    ) {
        val userContext = userContextProvider.userContext
        val showTaps = getSelectedScreenShareOption().mode != SINGLE_APP && tapsSwitch.isChecked
        val audioMode =
            if (audioSwitch.isChecked) options.selectedItem as ScreenRecordingAudioSource
            else ScreenRecordingAudioSource.NONE
        val lowQuality = lowQualitySwitch.isChecked
        val longerDuration = longerDurationSwitch.isChecked
        val hevc = hevcSwitch.isChecked
        val startIntent =
            PendingIntent.getForegroundService(
                userContext,
                RecordingService.REQUEST_CODE,
                RecordingService.getStartIntent(
                    userContext,
                    Activity.RESULT_OK,
                    audioMode.ordinal,
                    showTaps,
                    displayId,
                    captureTarget,
                    lowQuality,
                    longerDuration,
                    hevc,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val stopIntent =
            PendingIntent.getService(
                userContext,
                RecordingService.REQUEST_CODE,
                RecordingService.getStopIntent(userContext),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        savePrefs();
        controller.startCountdown(if (skipTimeSwitch.isChecked) NO_DELAY else DELAY_MS,
                INTERVAL_MS, startIntent, stopIntent)
    }

    private fun savePrefs() {
        val userContext = userContextProvider.userContext
        Prefs.putInt(userContext, PREF_TAPS, if (tapsSwitch.isChecked) 1 else 0)
        Prefs.putInt(userContext, PREF_LOW, if (lowQualitySwitch.isChecked) 1 else 0)
        Prefs.putInt(userContext, PREF_LONGER, if (longerDurationSwitch.isChecked) 1 else 0)
        Prefs.putInt(userContext, PREF_AUDIO, if (audioSwitch.isChecked) 1 else 0)
        Prefs.putInt(userContext, PREF_AUDIO_SOURCE, options.selectedItemPosition)
        Prefs.putInt(userContext, PREF_SKIP, if (skipTimeSwitch.isChecked) 1 else 0)
        Prefs.putInt(userContext, PREF_HEVC, if (hevcSwitch.isChecked) 1 else 0)
    }

    private fun loadPrefs() {
        val userContext = userContextProvider.userContext
        tapsSwitch.isChecked = Prefs.getInt(userContext, PREF_TAPS, 0) == 1
        lowQualitySwitch.isChecked = Prefs.getInt(userContext, PREF_LOW, 0) == 1
        longerDurationSwitch.isChecked = Prefs.getInt(userContext, PREF_LONGER, 0) == 1
        audioSwitch.isChecked = Prefs.getInt(userContext, PREF_AUDIO, 0) == 1
        options.setSelection(Prefs.getInt(userContext, PREF_AUDIO_SOURCE, 0))
        skipTimeSwitch.isChecked = Prefs.getInt(userContext, PREF_SKIP, 0) == 1
        hevcSwitch.isChecked = Prefs.getInt(userContext, PREF_HEVC, 1) == 1
    }

    private inner class CaptureTargetResultReceiver :
        ResultReceiver(Handler(Looper.getMainLooper())) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            if (resultCode == Activity.RESULT_OK) {
                val captureTarget =
                    resultData.getParcelable(
                        MediaProjectionAppSelectorActivity.KEY_CAPTURE_TARGET,
                        MediaProjectionCaptureTarget::class.java,
                    )

                // Start recording of the selected target
                requestScreenCapture(captureTarget)
            }
        }
    }

    companion object {
        private val MODES =
            listOf(
                ScreenRecordingAudioSource.INTERNAL,
                ScreenRecordingAudioSource.MIC,
                ScreenRecordingAudioSource.MIC_AND_INTERNAL,
            )
        private const val DELAY_MS: Long = 3000
        private const val NO_DELAY: Long = 100
        private const val INTERVAL_MS: Long = 1000

        private const val PREF_TAPS = "screenrecord_show_taps"
        private const val PREF_LOW = "screenrecord_use_low_quality"
        private const val PREF_LONGER = "screenrecord_use_longer_timeout"
        private const val PREF_AUDIO = "screenrecord_use_audio"
        private const val PREF_AUDIO_SOURCE = "screenrecord_audio_source"
        private const val PREF_SKIP = "screenrecord_skip_timer"
        private const val PREF_HEVC = "screenrecord_use_hevc"
    }
}
