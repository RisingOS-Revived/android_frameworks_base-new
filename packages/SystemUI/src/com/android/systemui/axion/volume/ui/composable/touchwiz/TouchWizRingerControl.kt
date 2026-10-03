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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.systemui.axion.volume.domain.model.AxionRingerMode
import com.android.systemui.axion.volume.ui.viewmodel.AxionVolumeDialogViewModel

@Composable
fun TouchWizRingerControl(
    viewModel: AxionVolumeDialogViewModel,
    ringerMode: AxionRingerMode,
    supportedModes: List<AxionRingerMode>,
    modifier: Modifier = Modifier
) {
    val options = buildList {
        if (AxionRingerMode.NORMAL in supportedModes) {
            add(AxionRingerMode.NORMAL to Icons.Filled.Notifications)
        }
        if (AxionRingerMode.VIBRATE in supportedModes) {
            add(AxionRingerMode.VIBRATE to Icons.Filled.Vibration)
        }
        if (AxionRingerMode.SILENT in supportedModes) {
            add(AxionRingerMode.SILENT to Icons.Filled.NotificationsOff)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TouchWizRingerModeSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (mode, icon) ->
            TouchWizRingerModeButton(
                icon = icon,
                label = mode.name,
                isSelected = ringerMode == mode,
                onClick = {
                    viewModel.rescheduleTimeout()
                    viewModel.setRingerMode(mode)
                }
            )
        }
    }
}

@Composable
private fun TouchWizRingerModeButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = TouchWizSpringDamping,
            stiffness = TouchWizSpringStiffness
        ),
        label = "touchwiz_ringer_btn_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(TouchWizAnimDuration),
        label = "touchwiz_ringer_btn_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(TouchWizAnimDuration),
        label = "touchwiz_ringer_btn_content"
    )

    Box(
        modifier = Modifier
            .size(TouchWizRingerModeButtonSize)
            .scale(scale)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(TouchWizSmallIconSize)
        )
    }
}
