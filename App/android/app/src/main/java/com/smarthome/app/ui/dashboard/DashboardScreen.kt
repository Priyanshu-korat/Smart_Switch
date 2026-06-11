package com.smarthome.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.core.designsystem.*
import com.smarthome.core.model.Device
import com.smarthome.core.model.SwitchState
import com.smarthome.core.mqtt.MqttConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onToggleSwitch: (deviceId: String, switchIndex: Int, currentState: Boolean) -> Unit,
    onAddDevice: () -> Unit,
    onDeviceCardClick: (deviceId: String) -> Unit,
    onScenesClick: () -> Unit
) {
    timber.log.Timber.d("DashboardScreen: Composing with state: $state")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Neutral900, Neutral850, Brand800.copy(alpha = 0.4f))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Header ──
            item {
                DashboardHeader(
                    greeting = state.greeting,
                    activeCount = state.activeCount,
                    mqttState = state.mqttState,
                    onNotificationsClick = {}
                )
            }

            // ── Summary Cards ──
            item {
                SummaryRow(
                    deviceCount = state.devices.size,
                    activeCount = state.activeCount,
                    onScenesClick = onScenesClick
                )
            }

            // ── Devices Section ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Devices",
                        style = MaterialTheme.typography.titleLarge,
                        color = Neutral100
                    )
                    IconButton(onClick = onAddDevice) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Device", tint = Accent500)
                    }
                }
            }

            if (state.isLoading) {
                items(3) { DeviceCardSkeleton() }
            } else if (state.devices.isEmpty()) {
                item { EmptyDeviceState(onAddDevice) }
            } else {
                items(state.devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onToggle = { index, current -> onToggleSwitch(device.id, index, current) },
                        onClick = { onDeviceCardClick(device.id) }
                    )
                }
            }
        }

        // ── FAB ──
        FloatingActionButton(
            onClick = onAddDevice,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Accent500,
            contentColor = Brand900
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Device")
        }
    }
}

@Composable
private fun DashboardHeader(
    greeting: String,
    activeCount: Int,
    mqttState: MqttConnectionState,
    onNotificationsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral300
                )
                Text(
                    "Smart Home",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Neutral100,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // MQTT connection pill
                MqttStatusPill(mqttState)
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Neutral300)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "$activeCount switches active",
            style = MaterialTheme.typography.bodyMedium,
            color = if (activeCount > 0) On500 else Neutral400
        )
    }
}

@Composable
private fun MqttStatusPill(state: MqttConnectionState) {
    val (color, label) = when (state) {
        MqttConnectionState.Connected    -> On500 to "Live"
        MqttConnectionState.Connecting   -> Warn500 to "Connecting"
        MqttConnectionState.Reconnecting -> Warn400 to "Reconnecting"
        MqttConnectionState.Disconnected -> Danger500 to "Offline"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.clip(CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .scale(if (state == MqttConnectionState.Reconnecting) scale else 1f)
                    .background(color, CircleShape)
            )
            Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryRow(
    deviceCount: Int,
    activeCount: Int,
    onScenesClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryChip(
                label = "$deviceCount Devices",
                icon = Icons.Filled.Devices,
                color = Brand400,
                onClick = {}
            )
        }
        item {
            SummaryChip(
                label = "$activeCount Active",
                icon = Icons.Filled.Bolt,
                color = On500,
                onClick = {}
            )
        }
        item {
            SummaryChip(
                label = "Scenes",
                icon = Icons.Filled.AutoAwesome,
                color = Accent500,
                onClick = onScenesClick
            )
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(label, color = Neutral100, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    onToggle: (switchIndex: Int, currentState: Boolean) -> Unit,
    onClick: () -> Unit
) {
    val anyOn = device.switches.any { it.state }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(
                if (anyOn) Modifier.activeGlow(Brand400) else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (device.isOnline) Brand600 else Neutral700,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Router,
                            contentDescription = null,
                            tint = if (device.isOnline) Accent500 else Neutral500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(device.name, style = MaterialTheme.typography.titleMedium, color = Neutral100)
                        Text(
                            if (device.isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.isOnline) On500 else Neutral500
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Neutral600)
            }

            if (device.switches.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                // Switches grid — max 4 per row
                device.switches.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { sw ->
                            SwitchTile(
                                switch = sw,
                                enabled = device.isOnline,
                                onToggle = { onToggle(sw.index, sw.state) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining columns if odd count
                        repeat(2 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SwitchTile(
    switch: SwitchState,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (switch.state) Accent500 else Neutral600,
        animationSpec = tween(300),
        label = "switch_color"
    )
    val bgColor by animateColorAsState(
        targetValue = if (switch.state) Accent500.copy(alpha = 0.12f) else Neutral750,
        animationSpec = tween(300),
        label = "switch_bg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (switch.state) Accent500.copy(alpha = 0.4f) else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(12.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (switch.state) Icons.Filled.Lightbulb else Icons.Outlined.LightbulbCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                switch.name.ifBlank { "Switch ${switch.index + 1}" },
                style = MaterialTheme.typography.labelMedium,
                color = if (switch.state) Neutral100 else Neutral400,
                maxLines = 1
            )
            Text(
                if (switch.state) "On" else "Off",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DeviceCardSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Neutral750.copy(alpha = shimmer))
    )
}

@Composable
private fun EmptyDeviceState(onAddDevice: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieEmptyState(
            title = "No Devices Yet",
            message = "Tap the + button below to add your first SmartHome device.",
            lottieAsset = "empty_state.json"
        )
    }
}
