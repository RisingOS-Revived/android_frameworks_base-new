/*
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

package com.android.systemui.axion.volume.ui.composable.touchwiz

import android.content.res.Configuration
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.systemui.axdynamicbar.shared.AxBlurBackdrop
import com.android.systemui.axdynamicbar.shared.LocalBlurAlpha
import com.android.systemui.axion.volume.domain.model.AxionVolumeDialogState
import com.android.systemui.axion.volume.domain.model.VolumeSliderItem
import com.android.systemui.axion.volume.ui.composable.lunaris.LunarisCollapsedRingerButton
import com.android.systemui.axion.volume.ui.viewmodel.AxionVolumeDialogViewModel
import kotlinx.coroutines.delay

@Composable
fun TouchWizVolumeDialogContent(viewModel: AxionVolumeDialogViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isVisible = uiState.isVisible
    val isExpanded = uiState.isExpanded

    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { animateIn = isVisible }

    val visibilityProgress by animateFloatAsState(
        targetValue = if (animateIn && isVisible) 1f else 0f,
        animationSpec = tween(TouchWizAnimDuration, easing = FastOutSlowInEasing),
        label = "touchwiz_visibility"
    )

    val overscrollOffset by viewModel.overscrollOffset.collectAsStateWithLifecycle()
    val animatedOverscroll by animateFloatAsState(
        targetValue = overscrollOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "touchwiz_overscroll"
    )

    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
    val view = LocalView.current
    LaunchedEffect(Unit) {
        viewModel.volumeKeyHapticTrigger.collect {
            if (isHapticEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    var showCollapsed by remember { mutableStateOf(!isExpanded) }
    var showExpanded by remember { mutableStateOf(isExpanded) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            showCollapsed = false
            delay(TouchWizAnimDuration.toLong())
            showExpanded = true
        } else {
            showExpanded = false
            delay(TouchWizAnimDuration.toLong())
            showCollapsed = true
        }
    }

    CompositionLocalProvider(LocalBlurAlpha provides visibilityProgress) {
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = visibilityProgress
                translationY = -24f * (1f - visibilityProgress)
                translationX = animatedOverscroll
            },
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = showCollapsed,
                enter = slideInVertically(
                    animationSpec = tween(TouchWizAnimDuration),
                    initialOffsetY = { -it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(TouchWizAnimDuration),
                    targetOffsetY = { -it }
                )
            ) {
                TouchWizCollapsedBar(viewModel)
            }

            AnimatedVisibility(
                visible = showExpanded,
                enter = slideInVertically(
                    animationSpec = tween(TouchWizAnimDuration),
                    initialOffsetY = { -it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(TouchWizAnimDuration),
                    targetOffsetY = { -it }
                )
            ) {
                TouchWizExpandedPanel(viewModel)
            }
        }
    }
}

@Composable
private fun touchWizPanelWidth(): Dp {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val wide = configuration.smallestScreenWidthDp >= 600 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val available = screenWidth - TouchWizPanelHorizontalMargin * 2
    return if (wide) minOf(available, TouchWizPanelMaxWidth) else available
}

@Composable
private fun TouchWizPill(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val blurAlpha = LocalBlurAlpha.current

    var blurHostReady by remember { mutableStateOf(blurAlpha > 0f) }
    LaunchedEffect(blurAlpha) {
        if (blurAlpha > 0f) blurHostReady = true
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(TouchWizPanelCornerRadius))
    ) {
        if (blurHostReady) {
            AxBlurBackdrop(
                cornerRadius = TouchWizPanelCornerRadius,
                fallbackColor = MaterialTheme.colorScheme.surfaceBright,
                modifier = Modifier.matchParentSize()
            )
        }
        content()
    }
}

@Composable
private fun TouchWizCollapsedBar(viewModel: AxionVolumeDialogViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialogState = uiState.dialogState
    val activeStream = dialogState.activeStream

    val streamModel = dialogState.volumeStreams.find { it.streamType == activeStream }
        ?: dialogState.volumeStreams.find { it.streamType == AudioManager.STREAM_MUSIC }

    TouchWizPill(
        modifier = Modifier
            .width(touchWizPanelWidth())
            .height(TouchWizBarHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TouchWizBarHeight)
                .padding(horizontal = TouchWizPanelPaddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TouchWizRowSpacing)
        ) {
            LunarisCollapsedRingerButton(
                ringerMode = dialogState.ringerMode,
                onClick = {
                    viewModel.rescheduleTimeout()
                    viewModel.toggleExpanded()
                },
                size = TouchWizRingerButtonSize
            )

            if (streamModel != null) {
                Box(modifier = Modifier.weight(1f)) {
                    TouchWizSliderRow(
                        stream = streamModel,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .size(TouchWizRingerButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.rescheduleTimeout()
                        viewModel.toggleExpanded()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(TouchWizIconSize)
                )
            }
        }
    }
}

@Composable
private fun TouchWizExpandedPanel(viewModel: AxionVolumeDialogViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialogState = uiState.dialogState
    val panelWidth = touchWizPanelWidth()

    Column(
        modifier = Modifier.width(panelWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TouchWizRowSpacing)
    ) {
        TouchWizRingerControl(
            viewModel = viewModel,
            ringerMode = dialogState.ringerMode,
            supportedModes = dialogState.supportedRingerModes
        )

        TouchWizPill(modifier = Modifier.fillMaxWidth()) {
            TouchWizVolumeSlidersColumn(
                viewModel = viewModel,
                sliderItems = touchWizSliderItems(dialogState),
                modifier = Modifier.padding(
                    horizontal = TouchWizPanelPaddingH,
                    vertical = TouchWizPanelPaddingV
                )
            )
        }
    }
}

private fun touchWizSliderItems(dialogState: AxionVolumeDialogState): List<VolumeSliderItem> {
    val streamOrder = listOf(
        AudioManager.STREAM_VOICE_CALL,
        AudioManager.STREAM_BLUETOOTH_SCO,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM
    )

    val musicStream = dialogState.volumeStreams.find { it.streamType == AudioManager.STREAM_MUSIC }
    val otherStreams = dialogState.volumeStreams
        .filter { it.streamType != AudioManager.STREAM_MUSIC }
        .sortedBy { streamOrder.indexOf(it.streamType) }

    return buildList {
        musicStream?.let { add(VolumeSliderItem.Stream(it)) }
        otherStreams.forEach { add(VolumeSliderItem.Stream(it)) }
        dialogState.appVolumes.forEach { add(VolumeSliderItem.AppVolume(it)) }
    }
}
