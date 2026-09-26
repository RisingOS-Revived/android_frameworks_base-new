package com.android.systemui.axdynamicbar.ui.compose

import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.*
import com.android.systemui.res.R

@Composable
internal fun ChargingExpanded(event: IslandEvent.Charging) {
    ExpandedCardLayout(
        accentColor = IslandTone.POSITIVE.tint,
        icon = {
            Icon(Icons.Filled.BatteryChargingFull, null, tint = IslandTone.POSITIVE.tint, modifier = Modifier.size(16.dp))
        },
        title = {
            Text(
                if (event.isWireless) stringResource(R.string.ax_dynamic_bar_wireless_charging)
                else stringResource(R.string.ax_dynamic_bar_charging),
                color = OnCardText,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceMd),
            ) {
                StatusChip("${event.level}%", IslandTone.POSITIVE.tint)
                if (event.isPowerSave) {
                    StatusChip(stringResource(R.string.ax_dynamic_bar_battery_saver), IslandTone.ATTENTION.tint)
                }
                // The time-to-full estimate used to sit in a trailing column of its own; on the
                // caption line it costs no height.
                if (!event.timeRemaining.isNullOrEmpty()) {
                    Text(
                        "${event.timeRemaining} ${stringResource(R.string.ax_dynamic_bar_until_full)}",
                        color = SubtleGray,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        },
    )
}

@Composable
internal fun BluetoothExpanded(event: IslandEvent.Bluetooth, interactor: IslandActions) {
    val artwork = event.deviceImage
    val earbuds =
        listOfNotNull(
            event.leftBatteryLevel?.let {
                BudBattery(R.string.ax_dynamic_bar_left_earbud, it, mirror = false)
            },
            event.rightBatteryLevel?.let {
                BudBattery(R.string.ax_dynamic_bar_right_earbud, it, mirror = true)
            },
        )
    val showSingleBattery = earbuds.isEmpty() && event.caseBatteryLevel == null
    ExpandedCardLayout(
        accentColor = IslandTone.ACCENT.tint,
        icon = {
            val image = artwork ?: event.deviceIcon
            if (image != null) {
                Image(
                    bitmap = image.toScaledBitmap(20.dp),
                    contentDescription =
                        event.deviceTypeLabel.ifEmpty {
                            stringResource(R.string.ax_dynamic_bar_bluetooth_device)
                        },
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(Icons.Filled.Bluetooth, null, tint = IslandTone.ACCENT.tint, modifier = Modifier.size(16.dp))
            }
        },
        title = {
            Text(
                event.deviceName,
                color = OnCardText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceMd),
            ) {
                StatusChip(
                    event.deviceTypeLabel.ifEmpty { stringResource(R.string.ax_dynamic_bar_connected) },
                    IslandTone.ACCENT.tint,
                )
                // The battery joins the caption line as text rather than a second chip.
                if (showSingleBattery && event.batteryLevel >= 0) {
                    Text(
                        "${event.batteryLevel}%",
                        color = if (event.batteryLevel > 20) IslandTone.POSITIVE.tint else IslandTone.ALERT.tint,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            event.caseBatteryLevel?.let { level ->
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dynamic_island_earbuds_case),
                        contentDescription = null,
                        tint = SubtleGray,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(R.string.ax_dynamic_bar_earbuds_case),
                        color = SubtleGray,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Text(
                        "$level%",
                        color = if (level > 20) IslandTone.POSITIVE.tint else IslandTone.ALERT.tint,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                PulsingDot(color = IslandTone.ACCENT.tint, size = SpaceMd)
                ActionIconButton(
                    icon = Icons.Filled.BluetoothDisabled,
                    tint = OnDestructiveText,
                    bg = DestructiveBg,
                    contentDescription = stringResource(R.string.ax_dynamic_bar_disconnect),
                    onClick = { interactor.disconnectBluetooth(event.address) },
                )
            }
        },
        actions = {
            if (earbuds.isNotEmpty()) BluetoothEarbudBatteries(earbuds)
        },
    )
}

private data class BudBattery(val labelRes: Int, val level: Int, val mirror: Boolean)

@Composable
private fun BluetoothEarbudBatteries(batteries: List<BudBattery>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        batteries.forEach { battery ->
            Row(
                modifier =
                    Modifier.weight(1f)
                        .heightIn(min = 56.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IslandTone.ACCENT.tint.copy(alpha = 0.08f))
                        .semantics(mergeDescendants = true) {}
                        .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.dynamic_island_earbud),
                    contentDescription = null,
                    tint = OnCardText.copy(alpha = 0.85f),
                    modifier =
                        Modifier.size(24.dp).graphicsLayer { scaleX = if (battery.mirror) -1f else 1f },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(battery.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = SubtleGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${battery.level}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (battery.level > 20) IslandTone.POSITIVE.tint else IslandTone.ALERT.tint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun HotspotExpanded(event: IslandEvent.Hotspot) {
    ExpandedCardLayout(
        accentColor = IslandTone.ACCENT.tint,
        icon = { Icon(Icons.Filled.Wifi, null, tint = IslandTone.ACCENT.tint, modifier = Modifier.size(16.dp)) },
        title = {
            Text(stringResource(R.string.ax_dynamic_bar_hotspot_active), color = OnCardText, style = MaterialTheme.typography.titleMedium)
            StatusChip(
                when (event.numDevices) {
                    0 -> stringResource(R.string.ax_dynamic_bar_no_devices)
                    1 -> stringResource(R.string.ax_dynamic_bar_one_device_connected)
                    else -> stringResource(R.string.ax_dynamic_bar_devices_connected, event.numDevices)
                },
                IslandTone.ACCENT.tint,
            )
        },
        trailing = { PulsingDot(color = IslandTone.ACCENT.tint, size = SpaceMd) },
    )
}

@Composable
internal fun RingerModeExpanded(event: IslandEvent.RingerMode, interactor: IslandActions) {
    val style = eventStyleFor(event)
    ExpandedCardLayout(
        accentColor = style.accent,
        icon = { style.icon?.let { Icon(it, null, tint = style.accent, modifier = Modifier.size(16.dp)) } },
        title = {
            Text(stringResource(R.string.ax_dynamic_bar_sound_mode), color = OnCardText, style = MaterialTheme.typography.titleMedium)
            StatusChip(stringResource(style.labelRes), style.accent)
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceLg),
            ) {
                RingerCard(
                    isSelected = event.mode == AudioManager.RINGER_MODE_NORMAL,
                    icon = Icons.Filled.VolumeUp,
                    label = stringResource(R.string.ax_dynamic_bar_ring),
                    accent = IslandTone.ACCENT.tint,
                    onClick = { interactor.setRingerMode(AudioManager.RINGER_MODE_NORMAL) },
                    modifier = Modifier.weight(1f),
                )
                RingerCard(
                    isSelected = event.mode == AudioManager.RINGER_MODE_VIBRATE,
                    icon = Icons.Filled.Vibration,
                    label = stringResource(R.string.ax_dynamic_bar_vibrate),
                    accent = IslandTone.ATTENTION.tint,
                    onClick = { interactor.setRingerMode(AudioManager.RINGER_MODE_VIBRATE) },
                    modifier = Modifier.weight(1f),
                )
                RingerCard(
                    isSelected = event.mode == AudioManager.RINGER_MODE_SILENT,
                    icon = Icons.Filled.VolumeOff,
                    label = stringResource(R.string.ax_dynamic_bar_silent),
                    accent = IslandTone.ATTENTION.tint,
                    onClick = { interactor.setRingerMode(AudioManager.RINGER_MODE_SILENT) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RingerCard(
    isSelected: Boolean,
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by
        animateColorAsState(
            targetValue = if (isSelected) accent.copy(alpha = AlphaIconBg) else OnCardText.copy(alpha = AlphaFaint),
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "ringer_bg",
        )
    val tint by
        animateColorAsState(
            targetValue = if (isSelected) accent else SubtleGray,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "ringer_tint",
        )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ShapeLg,
        color = bg,
    ) {
        Column(
            // These three cards sit inside a tile that is itself already inset, so the generous
            // vertical padding they used to carry just made the tile tall.
            modifier = Modifier.padding(vertical = SpaceMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpaceXs),
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun VpnExpanded(event: IslandEvent.Vpn) {
    val style = eventStyleFor(event)
    ExpandedCardLayout(
        accentColor = style.accent,
        icon = { style.icon?.let { Icon(it, null, tint = style.accent, modifier = Modifier.size(16.dp)) } },
        title = {
            Text(
                stringResource(style.labelRes),
                color = OnCardText,
                style = MaterialTheme.typography.titleMedium,
            )
            StatusChip(
                if (event.isValidated) stringResource(R.string.ax_dynamic_bar_secured)
                else stringResource(R.string.ax_dynamic_bar_connecting),
                if (event.isValidated) IslandTone.POSITIVE.tint else IslandTone.ATTENTION.tint,
            )
        },
        trailing = {
            PulsingDot(color = if (event.isValidated) IslandTone.POSITIVE.tint else IslandTone.ATTENTION.tint, size = SpaceMd)
        },
    )
}
