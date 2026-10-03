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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.systemui.axion.volume.domain.model.AxionAppVolumeModel
import com.android.systemui.axion.volume.domain.model.AxionVolumeStreamModel
import com.android.systemui.axion.volume.domain.model.VolumeSliderItem
import com.android.systemui.axion.volume.ui.viewmodel.AxionVolumeDialogViewModel
import com.android.systemui.haptics.slider.SeekableSliderTrackerConfig
import com.android.systemui.haptics.slider.SliderHapticFeedbackConfig
import com.android.systemui.lifecycle.rememberViewModel

@Composable
fun TouchWizVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    viewModel: AxionVolumeDialogViewModel,
    streamType: Int? = null,
    icon: @Composable () -> Unit,
    onIconClick: () -> Unit,
    iconSize: Dp = TouchWizIconSize,
    showPercentage: Boolean = true,
) {
    var sliderValue by remember { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(value) { if (!isDragging) sliderValue = value }

    val interactionSource = remember { MutableInteractionSource() }
    val hapticsViewModel = key(viewModel) {
        rememberViewModel(traceName = "TouchWizVolumeSliderHaptics") {
            viewModel.sliderHapticsViewModelFactory.create(
                interactionSource,
                0f..1f,
                Orientation.Horizontal,
                SliderHapticFeedbackConfig(),
                SeekableSliderTrackerConfig()
            )
        }
    }

    val animatedValue by animateFloatAsState(
        targetValue = sliderValue,
        animationSpec = if (isDragging) snap() else tween(60, easing = FastOutSlowInEasing),
        label = "touchwiz_slider_value"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (isMuted) 0.4f else 1f,
        label = "touchwiz_icon_alpha"
    )

    val primary = MaterialTheme.colorScheme.primary
    val trackColor = lerp(MaterialTheme.colorScheme.surfaceVariant, Color.Black, 0.2f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TouchWizRowSpacing)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize + 8.dp)
                .clip(CircleShape)
                .graphicsLayer { alpha = iconAlpha }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIconClick
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(TouchWizSliderTrackHeight)
                .clip(RoundedCornerShape(percent = TouchWizTrackCornerPercent))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            viewModel.isInteracting = true
                            sliderValue = (offset.x / size.width).coerceIn(0f, 1f)
                            onValueChange(sliderValue)
                            viewModel.isInteracting = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            streamType?.let { viewModel.setActiveStream(it) }
                            sliderValue = (offset.x / size.width).coerceIn(0f, 1f)
                            onValueChange(sliderValue)
                            if (isHapticEnabled) hapticsViewModel.onValueChange(sliderValue)
                            viewModel.isInteracting = true
                        },
                        onDragEnd = {
                            isDragging = false
                            if (isHapticEnabled) hapticsViewModel.onValueChangeEnded()
                            viewModel.isInteracting = false
                            viewModel.setOverscrollOffset(0f)
                        },
                        onDragCancel = {
                            isDragging = false
                            if (isHapticEnabled) hapticsViewModel.onValueChangeEnded()
                            viewModel.isInteracting = false
                            viewModel.setOverscrollOffset(0f)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val raw = change.position.x / size.width
                            when {
                                raw < 0f -> {
                                    sliderValue = 0f
                                    viewModel.setOverscrollOffset((raw * 30f).coerceAtLeast(-10f))
                                }
                                raw > 1f -> {
                                    sliderValue = 1f
                                    viewModel.setOverscrollOffset(((raw - 1f) * 30f).coerceAtMost(10f))
                                }
                                else -> {
                                    sliderValue = raw
                                    viewModel.setOverscrollOffset(0f)
                                }
                            }
                            if (isHapticEnabled) {
                                hapticsViewModel.addVelocityDataPoint(sliderValue)
                                hapticsViewModel.onValueChange(sliderValue)
                            }
                            onValueChange(sliderValue)
                        }
                    )
                }
                .drawBehind {
                    val h = size.height
                    val w = size.width
                    val r = h / 2f
                    drawRoundRect(color = trackColor, size = Size(w, h), cornerRadius = CornerRadius(r))
                    val fillW = w * animatedValue
                    drawRoundRect(color = primary, size = Size(fillW, h), cornerRadius = CornerRadius(r))
                }
        )

        if (showPercentage) {
            Text(
                text = "${(sliderValue * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.width(38.dp)
            )
        }
    }
}

@Composable
fun TouchWizSliderRow(
    stream: AxionVolumeStreamModel,
    viewModel: AxionVolumeDialogViewModel,
    modifier: Modifier = Modifier,
    iconSize: Dp = TouchWizIconSize,
    showPercentage: Boolean = true,
) {
    val muted = stream.isMuted
    TouchWizVolumeSlider(
        value = if (muted) 0f else stream.level,
        onValueChange = { viewModel.setVolume(stream.streamType, it) },
        isMuted = muted,
        modifier = modifier,
        viewModel = viewModel,
        streamType = stream.streamType,
        iconSize = iconSize,
        showPercentage = showPercentage,
        icon = {
            Icon(
                imageVector = if (muted) stream.mutedIcon else stream.icon,
                contentDescription = stream.streamInfo.label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(iconSize)
            )
        },
        onIconClick = { viewModel.toggleMute(stream.streamType) }
    )
}

@Composable
fun TouchWizAppVolumeSlider(
    appVolume: AxionAppVolumeModel,
    viewModel: AxionVolumeDialogViewModel,
    modifier: Modifier = Modifier,
    iconSize: Dp = TouchWizIconSize,
    showPercentage: Boolean = true,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appInfo = remember(appVolume.packageName) {
        try { pm.getApplicationInfo(appVolume.packageName, 0) } catch (e: Exception) { null }
    }
    val icon = remember(appInfo) { appInfo?.loadIcon(pm) }
    val label = remember(appInfo) { appInfo?.loadLabel(pm)?.toString() ?: appVolume.packageName }
    val isMutedOrZero = appVolume.isMuted || appVolume.volume == 0f

    TouchWizVolumeSlider(
        value = if (appVolume.isMuted) 0f else appVolume.volume,
        onValueChange = { viewModel.setAppVolume(appVolume.packageName, it) },
        isMuted = appVolume.isMuted,
        modifier = modifier,
        viewModel = viewModel,
        iconSize = iconSize,
        showPercentage = showPercentage,
        icon = {
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = label,
                    colorFilter = if (isMutedOrZero) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else null,
                    modifier = Modifier.size(iconSize).clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(iconSize)
                )
            }
        },
        onIconClick = { viewModel.setAppMute(appVolume.packageName, !appVolume.isMuted) }
    )
}

@Composable
fun TouchWizVolumeSlidersColumn(
    viewModel: AxionVolumeDialogViewModel,
    sliderItems: List<VolumeSliderItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TouchWizRowSpacing)
    ) {
        sliderItems.forEach { item ->
            when (item) {
                is VolumeSliderItem.Stream -> key(item.model.streamType) {
                    TouchWizSliderRow(
                        stream = item.model,
                        viewModel = viewModel,
                        modifier = Modifier.height(TouchWizRowHeight)
                    )
                }
                is VolumeSliderItem.AppVolume -> key(item.model.packageName) {
                    TouchWizAppVolumeSlider(
                        appVolume = item.model,
                        viewModel = viewModel,
                        modifier = Modifier.height(TouchWizRowHeight)
                    )
                }
            }
        }
    }
}
