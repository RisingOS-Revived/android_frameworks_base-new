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

package com.android.systemui.volume.panel.component.volume.slider.ui.viewmodel

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.STREAM_ALARM
import android.media.AudioManager.STREAM_MUSIC
import android.media.AudioManager.STREAM_NOTIFICATION
import android.util.Log
import com.android.app.tracing.coroutines.launchTraced as launch
import com.android.internal.logging.UiEventLogger
import com.android.settingslib.volume.domain.interactor.AudioVolumeInteractor
import com.android.settingslib.volume.shared.model.AudioStream
import com.android.settingslib.volume.shared.model.AudioStreamModel
import com.android.settingslib.volume.shared.model.RingerMode
import com.android.systemui.Flags
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.haptics.slider.compose.ui.SliderHapticsViewModel
import com.android.systemui.modes.shared.ModesUiIcons
import com.android.systemui.res.R
import com.android.systemui.statusbar.policy.domain.interactor.ZenModeInteractor
import com.android.systemui.statusbar.policy.domain.model.ActiveZenModes
import com.android.systemui.util.kotlin.combine
import com.android.systemui.volume.panel.shared.VolumePanelLogger
import com.android.systemui.volume.panel.ui.VolumePanelUiEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Models a particular slider state. */
class AudioStreamSliderViewModel
@AssistedInject
constructor(
    @Assisted private val audioStreamWrapper: FactoryAudioStreamWrapper,
    @Assisted private val coroutineScope: CoroutineScope,
    private val context: Context,
    private val audioVolumeInteractor: AudioVolumeInteractor,
    private val zenModeInteractor: ZenModeInteractor,
    private val uiEventLogger: UiEventLogger,
    private val volumePanelLogger: VolumePanelLogger,
    private val hapticsViewModelFactory: SliderHapticsViewModel.Factory,
) : SliderViewModel {

    private val volumeChanges = MutableStateFlow<Int?>(null)
    private val streamsAffectedByRing =
        setOf(AudioManager.STREAM_RING, AudioManager.STREAM_NOTIFICATION)
    private val audioStream = audioStreamWrapper.audioStream
    private val iconsByStream =
        mapOf(
            AudioStream(AudioManager.STREAM_MUSIC) to R.drawable.ic_music_note,
            AudioStream(AudioManager.STREAM_VOICE_CALL) to R.drawable.ic_call,
            AudioStream(AudioManager.STREAM_RING) to R.drawable.ic_ring_volume,
            AudioStream(AudioManager.STREAM_NOTIFICATION) to R.drawable.ic_volume_ringer,
            AudioStream(AudioManager.STREAM_ALARM) to R.drawable.ic_volume_alarm,
        )
    private val labelsByStream =
        mapOf(
            AudioStream(AudioManager.STREAM_MUSIC) to R.string.stream_music,
            AudioStream(AudioManager.STREAM_VOICE_CALL) to R.string.stream_voice_call,
            AudioStream(AudioManager.STREAM_RING) to R.string.stream_ring,
            AudioStream(AudioManager.STREAM_NOTIFICATION) to R.string.stream_notification,
            AudioStream(AudioManager.STREAM_ALARM) to R.string.stream_alarm,
        )
    private val uiEventByStream =
        mapOf(
            AudioStream(AudioManager.STREAM_MUSIC) to
                VolumePanelUiEvent.VOLUME_PANEL_MUSIC_SLIDER_TOUCHED,
            AudioStream(AudioManager.STREAM_VOICE_CALL) to
                VolumePanelUiEvent.VOLUME_PANEL_VOICE_CALL_SLIDER_TOUCHED,
            AudioStream(AudioManager.STREAM_RING) to
                VolumePanelUiEvent.VOLUME_PANEL_RING_SLIDER_TOUCHED,
            AudioStream(AudioManager.STREAM_NOTIFICATION) to
                VolumePanelUiEvent.VOLUME_PANEL_NOTIFICATION_SLIDER_TOUCHED,
            AudioStream(AudioManager.STREAM_ALARM) to
                VolumePanelUiEvent.VOLUME_PANEL_ALARM_SLIDER_TOUCHED,
        )

    override val slider: StateFlow<SliderState> =
        if (ModesUiIcons.isEnabled) {
            combine(
                    audioVolumeInteractor.getAudioStream(audioStream),
                    audioVolumeInteractor.canChangeVolume(audioStream),
                    audioVolumeInteractor.ringerMode,
                    zenModeInteractor.activeModesBlockingEverything,
                    zenModeInteractor.activeModesBlockingAlarms,
                    zenModeInteractor.activeModesBlockingMedia,
                ) {
                    model,
                    isEnabled,
                    ringerMode,
                    modesBlockingEverything,
                    modesBlockingAlarms,
                    modesBlockingMedia ->
                    volumePanelLogger.onVolumeUpdateReceived(audioStream, model.volume)
                    model.toState(
                        isEnabled,
                        ringerMode,
                        getStreamDisabledMessage(
                            modesBlockingEverything,
                            modesBlockingAlarms,
                            modesBlockingMedia,
                        ),
                    )
                }
                .stateIn(coroutineScope, SharingStarted.Eagerly, SliderState.Empty)
        } else {
            combine(
                    audioVolumeInteractor.getAudioStream(audioStream),
                    audioVolumeInteractor.canChangeVolume(audioStream),
                    audioVolumeInteractor.ringerMode,
                ) { model, isEnabled, ringerMode ->
                    volumePanelLogger.onVolumeUpdateReceived(audioStream, model.volume)
                    model.toState(
                        isEnabled,
                        ringerMode,
                        getStreamDisabledMessageWithoutModes(audioStream),
                    )
                }
                .stateIn(coroutineScope, SharingStarted.Eagerly, SliderState.Empty)
        }

    init {
        volumeChanges
            .filterNotNull()
            .onEach {
                volumePanelLogger.onSetVolumeRequested(audioStream, it)
                audioVolumeInteractor.setVolume(audioStream, it)
            }
            .launchIn(coroutineScope)
    }

    override fun onValueChanged(state: SliderState, newValue: Float) {
        val audioViewModel = state as? State
        audioViewModel ?: return
        volumeChanges.tryEmit(newValue.roundToInt())
    }

    override fun onValueChangeFinished() {
        uiEventByStream[audioStream]?.let { uiEventLogger.log(it) }
    }

    override fun toggleMuted(state: SliderState) {
        val audioViewModel = state as? State
        audioViewModel ?: return
        coroutineScope.launch {
            audioVolumeInteractor.setMuted(audioStream, !audioViewModel.audioStreamModel.isMuted)
        }
    }

    override fun getSliderHapticsViewModelFactory(): SliderHapticsViewModel.Factory? =
        if (Flags.hapticsForComposeSliders() && slider.value != SliderState.Empty) {
            hapticsViewModelFactory
        } else {
            null
        }

    private fun AudioStreamModel.toState(
        isEnabled: Boolean,
        ringerMode: RingerMode,
        disabledMessage: String?,
    ): State {
        val label =
            labelsByStream[audioStream]?.let(context::getString)
                ?: error("No label for the stream: $audioStream")
        return State(
            value = volume.toFloat(),
            valueRange = volumeRange.first.toFloat()..volumeRange.last.toFloat(),
            icon = getIcon(ringerMode),
            label = label,
            disabledMessage = disabledMessage,
            isEnabled = isEnabled,
            a11yStep = volumeRange.step,
            a11yClickDescription =
                if (isAffectedByMute) {
                    context.getString(
                        if (isMuted) {
                            R.string.volume_panel_hint_unmute
                        } else {
                            R.string.volume_panel_hint_mute
                        },
                        label,
                    )
                } else {
                    null
                },
            a11yStateDescription =
                if (volume == volumeRange.first) {
                    context.getString(
                        if (audioStream.value in streamsAffectedByRing) {
                            if (ringerMode.value == AudioManager.RINGER_MODE_VIBRATE) {
                                R.string.volume_panel_hint_vibrate
                            } else {
                                R.string.volume_panel_hint_muted
                            }
                        } else {
                            R.string.volume_panel_hint_muted
                        }
                    )
                } else {
                    null
                },
            audioStreamModel = this,
            isMutable = isAffectedByMute,
        )
    }

    private fun getStreamDisabledMessage(
        blockingEverything: ActiveZenModes,
        blockingAlarms: ActiveZenModes,
        blockingMedia: ActiveZenModes,
    ): String {
        // TODO: b/372213356 - Figure out the correct messages for VOICE_CALL and RING.
        //  In fact, VOICE_CALL should not be affected by interruption filtering at all.
        return if (audioStream.value == STREAM_NOTIFICATION) {
            context.getString(R.string.stream_notification_unavailable)
        } else {
            val blockingModeName =
                when {
                    blockingEverything.mainMode != null -> blockingEverything.mainMode.name
                    audioStream.value == STREAM_ALARM -> blockingAlarms.mainMode?.name
                    audioStream.value == STREAM_MUSIC -> blockingMedia.mainMode?.name
                    else -> null
                }

            if (blockingModeName != null) {
                context.getString(R.string.stream_unavailable_by_modes, blockingModeName)
            } else {
                // Should not actually be visible, but as a catch-all.
                context.getString(R.string.stream_unavailable_by_unknown)
            }
        }
    }

    private fun getStreamDisabledMessageWithoutModes(audioStream: AudioStream): String {
        // TODO: b/372213356 - Figure out the correct messages for VOICE_CALL and RING.
        //  In fact, VOICE_CALL should not be affected by interruption filtering at all.
        return if (audioStream.value == STREAM_NOTIFICATION) {
            context.getString(R.string.stream_notification_unavailable)
        } else {
            context.getString(R.string.stream_alarm_unavailable)
        }
    }

    private fun AudioStreamModel.getIcon(ringerMode: RingerMode): Icon {
        val iconRes =
            if (isAffectedByMute && isMuted) {
                if (audioStream.value in streamsAffectedByRing) {
                    if (ringerMode.value == AudioManager.RINGER_MODE_VIBRATE) {
                        R.drawable.ic_volume_ringer_vibrate
                    } else {
                        R.drawable.ic_volume_off
                    }
                } else {
                    R.drawable.ic_volume_off
                }
            } else {
                iconsByStream[audioStream]
                    ?: run {
                        Log.wtf(TAG, "No icon for the stream: $audioStream")
                        R.drawable.ic_music_note
                    }
            }
        return Icon.Resource(iconRes, null)
    }

    private val AudioStreamModel.volumeRange: IntRange
        get() = minVolume..maxVolume

    private data class State(
        override val value: Float,
        override val valueRange: ClosedFloatingPointRange<Float>,
        override val icon: Icon,
        override val label: String,
        override val disabledMessage: String?,
        override val isEnabled: Boolean,
        override val a11yStep: Int,
        override val a11yClickDescription: String?,
        override val a11yStateDescription: String?,
        override val isMutable: Boolean,
        val audioStreamModel: AudioStreamModel,
    ) : SliderState

    @AssistedFactory
    interface Factory {

        fun create(
            audioStream: FactoryAudioStreamWrapper,
            coroutineScope: CoroutineScope,
        ): AudioStreamSliderViewModel
    }

    /**
     * AudioStream is a value class that compiles into a primitive. This fail AssistedFactory build
     * when using [AudioStream] directly because it expects another type.
     */
    class FactoryAudioStreamWrapper(val audioStream: AudioStream)

    private companion object {
        const val TAG = "AudioStreamSliderViewModel"
    }
}
