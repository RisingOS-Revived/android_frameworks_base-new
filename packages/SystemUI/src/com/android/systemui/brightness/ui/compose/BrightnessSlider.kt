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

package com.android.systemui.brightness.ui.compose

import android.content.Context
import android.view.MotionEvent
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.internal.graphics.ColorUtils
import com.android.systemui.biometrics.Utils.toBitmap
import com.android.systemui.brightness.shared.model.GammaBrightness
import com.android.systemui.brightness.ui.viewmodel.BrightnessSliderViewModel
import com.android.systemui.brightness.ui.viewmodel.Drag
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.haptics.slider.SeekableSliderTrackerConfig
import com.android.systemui.haptics.slider.SliderHapticFeedbackConfig
import com.android.systemui.haptics.slider.compose.ui.SliderHapticsViewModel
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.qs.ui.compose.borderOnFocus
import com.android.systemui.res.R
import com.android.systemui.utils.PolicyRestriction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@VisibleForTesting
fun BrightnessSlider(
    gammaValue: Int,
    valueRange: IntRange,
    autoMode: Boolean,
    iconResProvider: (Float, Boolean) -> Int,
    imageLoader: suspend (Int, Context) -> Icon.Loaded?,
    restriction: PolicyRestriction,
    onRestrictedClick: (PolicyRestriction.Restricted) -> Unit,
    onDrag: (Int) -> Unit,
    onStop: (Int) -> Unit,
    onIconClick: suspend () -> Unit,
    overriddenByAppState: Boolean,
    modifier: Modifier = Modifier,
    showToast: () -> Unit = {},
    hapticsViewModelFactory: SliderHapticsViewModel.Factory,
    isDragging: Boolean = false,
    showEditBorder: Boolean = false,
) {
    var value by remember(gammaValue) { mutableIntStateOf(gammaValue) }
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(), 
        label = "BrightnessSliderAnimatedValue",
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val floatValueRange = valueRange.first.toFloat()..valueRange.last.toFloat()
    val isRestricted = restriction is PolicyRestriction.Restricted
    val enabled = !isRestricted
    val interactionSource = remember { MutableInteractionSource() }
    
    val hapticsViewModel: SliderHapticsViewModel =
        rememberViewModel(traceName = "SliderHapticsViewModel") {
            hapticsViewModelFactory.create(
                interactionSource,
                floatValueRange,
                Orientation.Horizontal,
                SliderHapticFeedbackConfig(
                    maxVelocityToScale = 1f 
                ),
                SeekableSliderTrackerConfig(),
            )
        }

    val iconRes by
        remember(gammaValue, valueRange, autoMode) {
            derivedStateOf {
                val percentage =
                    (value - valueRange.first) * 100f / (valueRange.last - valueRange.first)
                iconResProvider(percentage, autoMode)
            }
        }

    val context = LocalContext.current
    val painter: Painter by produceState<Painter>(
        initialValue = ColorPainter(Color.Transparent),
        key1 = iconRes,
        key2 = context,
    ) {
        val icon: Icon.Loaded? = imageLoader(iconRes, context)
        if (icon != null) {
            val bitmap = icon.drawable.toBitmap()?.asImageBitmap()
            if (bitmap != null) {
                this@produceState.value = BitmapPainter(bitmap)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(if (isDragging) 1000f else 1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showEditBorder) Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(percent = 50),
                    ) else Modifier
                )
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(percent = 50),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Dimensions.SliderHeight),
                contentAlignment = Alignment.Center
            ) {
                GlassSliderCore(
                    value = animatedValue,
                    valueRange = floatValueRange,
                    onValueChange = {
                        if (enabled && !overriddenByAppState) {
                            hapticsViewModel?.onValueChange(it)
                            value = it.toInt()
                            onDrag(value)
                        }
                    },
                    onValueChangeFinished = {
                        if (enabled && !overriddenByAppState) {
                            hapticsViewModel?.onValueChangeEnded()
                            onStop(value)
                        }
                    },
                    enabled = enabled,
                    interactionSource = interactionSource,
                    painter = painter
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(Dimensions.IconContainerSize)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { coroutineScope.launch { onIconClick() } }
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                M3Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimensions.IconSize)
                )
            }
        }
    }

    val currentShowToast by rememberUpdatedState(showToast)
    LaunchedEffect(interactionSource, overriddenByAppState) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start && overriddenByAppState) {
                currentShowToast()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassSliderCore(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    painter: Painter
) {
    val fraction = ((value - valueRange.start) /
            (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .aspectRatio(1f)
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            M3Icon(
                painter = painter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            interactionSource = interactionSource,
            thumb = {
                Box(modifier = Modifier.size(0.dp))
            },
            track = { 
                Box(modifier = Modifier.fillMaxWidth())
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
fun BrightnessSliderContainer(
    viewModel: BrightnessSliderViewModel,
    modifier: Modifier = Modifier,
    containerColors: ContainerColors,
    showEditBorder: Boolean = false,
) {
    val gamma = viewModel.currentBrightness.value
    if (gamma == BrightnessSliderViewModel.initialValue.value) return

    val autoMode = viewModel.autoMode
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val restriction by viewModel.policyRestriction.collectAsStateWithLifecycle(
        initialValue = PolicyRestriction.NoRestriction
    )
    val overriddenByAppState by viewModel.brightnessOverriddenByWindow.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { viewModel.setIsDragging(false) } }

    var dragging by remember { mutableStateOf(false) }
    
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.95f) }
    
    LaunchedEffect(Unit) {
        launch { entranceAlpha.animateTo(1f, tween(300)) }
        launch { entranceScale.animateTo(1f, spring(dampingRatio = 0.8f)) }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(Dimensions.ContainerHeight)
            .sysuiResTag("brightness_slider")
            .graphicsLayer {
                alpha = entranceAlpha.value
                scaleX = entranceScale.value
                scaleY = entranceScale.value
                if (dragging) alpha = 1f
            }
            .zIndex(if (dragging) 1000f else 1f)
    ) {
        BrightnessSlider(
            gammaValue = gamma,
            valueRange = viewModel.minBrightness.value..viewModel.maxBrightness.value,
            autoMode = autoMode,
            iconResProvider = BrightnessSliderViewModel::getIconForPercentage,
            imageLoader = viewModel::loadImage,
            restriction = restriction,
            onRestrictedClick = viewModel::showPolicyRestrictionDialog,
            onDrag = {
                viewModel.setIsDragging(true)
                dragging = true
                coroutineScope.launch { viewModel.onDrag(Drag.Dragging(GammaBrightness(it))) }
            },
            onStop = {
                viewModel.setIsDragging(false)
                dragging = false
                coroutineScope.launch { viewModel.onDrag(Drag.Stopped(GammaBrightness(it))) }
            },
            onIconClick = { viewModel.onIconClick() },
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter {
                     if (it.actionMasked == MotionEvent.ACTION_UP || 
                         it.actionMasked == MotionEvent.ACTION_CANCEL) {
                        viewModel.emitBrightnessTouchForFalsing()
                    }
                    false
                },
            hapticsViewModelFactory = viewModel.hapticsViewModelFactory,
            overriddenByAppState = overriddenByAppState,
            showToast = {
                viewModel.showToast(context, R.string.quick_settings_brightness_unable_adjust_msg)
            },
            isDragging = dragging,
            showEditBorder = showEditBorder,
        )
    }
}

data class ContainerColors(val idleColor: Color, val mirrorColor: Color) {
    companion object {
        fun singleColor(color: Color) = ContainerColors(color, color)

        val defaultContainerColor: Color
            @Composable @ReadOnlyComposable get() = colorResource(R.color.shade_panel_fallback)
    }
}

private object Dimensions {
    val IconSize = 28.dp
    val IconContainerSize = 44.dp
    val SliderHeight = 44.dp
    val ContainerHeight = 80.dp
}

@Composable
fun MutableInteractionSource.collectIsPressedAsState(): androidx.compose.runtime.State<Boolean> {
    val isPressed = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed.value = true
                is PressInteraction.Release -> isPressed.value = false
                is PressInteraction.Cancel -> isPressed.value = false
            }
        }
    }
    return isPressed
}

@Composable
fun MutableInteractionSource.collectIsDraggedAsState(): androidx.compose.runtime.State<Boolean> {
    val isDragged = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> isDragged.value = true
                is DragInteraction.Stop -> isDragged.value = false
                is DragInteraction.Cancel -> isDragged.value = false
            }
        }
    }
    return isDragged
}
