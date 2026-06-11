package com.smarthome.app.ui.device

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.core.designsystem.*
import com.smarthome.core.model.SwitchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlScreen(
    state: DeviceControlUiState,
    onToggleSwitch: (index: Int, currentState: Boolean) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Neutral900, Neutral850, Brand800.copy(alpha = 0.3f)))
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── Top Bar ──
            TopAppBar(
                title = {
                    Text(
                        state.device?.name ?: "Device",
                        style = MaterialTheme.typography.titleLarge,
                        color = Neutral100
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Neutral300)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(state.error)
                state.device != null -> {
                    val device = state.device
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        // ── Device Status Card ──
                        DeviceStatusCard(
                            isOnline = device.isOnline,
                            lastSeenAt = device.lastSeenAt
                        )

                        Spacer(Modifier.height(24.dp))

                        Text(
                            "Switches",
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral300,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // ── Switch Grid (2-column) ──
                        device.switches.chunked(2).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { sw ->
                                    LargeSwitchCard(
                                        switch = sw,
                                        enabled = device.isOnline,
                                        onToggle = { onToggleSwitch(sw.index, sw.state) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusCard(isOnline: Boolean, lastSeenAt: Long) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val dotColor by animateColorAsState(
                if (isOnline) On500 else Danger500,
                animationSpec = tween(500),
                label = "status_color"
            )
            Box(Modifier.size(10.dp).background(dotColor, androidx.compose.foundation.shape.CircleShape))
            Column {
                Text(
                    if (isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral100
                )
                Text(
                    if (isOnline) "Receiving live updates" else "Last seen: ${formatTime(lastSeenAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral400
                )
            }
        }
    }
}

@Composable
private fun LargeSwitchCard(
    switch: SwitchState,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val isOn = switch.state
    val accentColor by animateColorAsState(
        if (isOn) Accent500 else Neutral500,
        animationSpec = tween(350),
        label = "switch_accent"
    )
    val bgGradient = if (isOn) {
        Brush.verticalGradient(listOf(Accent500.copy(alpha = 0.20f), Brand600.copy(alpha = 0.10f)))
    } else {
        Brush.verticalGradient(listOf(Neutral750, Neutral800))
    }

    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgGradient)
            .border(
                1.dp,
                if (isOn) Accent500.copy(alpha = 0.5f) else GlassBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: icon + state dot
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(
                    if (isOn) Icons.Filled.Lightbulb else Icons.Filled.LightbulbCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, androidx.compose.foundation.shape.CircleShape)
                )
            }

            // Bottom: name + on/off
            Column {
                Text(
                    switch.name.ifBlank { "Switch ${switch.index + 1}" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral100,
                    maxLines = 1
                )
                Text(
                    if (isOn) "ON" else "OFF",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent500)
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Danger500)
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60}h ago"
    }
}
