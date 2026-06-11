package com.smarthome.app.ui.schedules

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.core.designsystem.*
import com.smarthome.core.network.dto.ScheduleDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    state: SchedulesUiState,
    onShowCreate: () -> Unit,
    onDismissCreate: () -> Unit,
    onDayToggled: (Int) -> Unit,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onLabelChanged: (String) -> Unit,
    onTargetStateChanged: (Boolean) -> Unit,
    onCreateSchedule: () -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Neutral900, Neutral850)))
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Schedules", style = MaterialTheme.typography.titleLarge, color = Neutral100) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Neutral300)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent500)
                }
            } else if (state.schedules.isEmpty()) {
                EmptySchedulesState(onAddClick = onShowCreate)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.schedules, key = { it.scheduleId }) { schedule ->
                        ScheduleItem(
                            schedule = schedule,
                            onDelete = { onDeleteSchedule(schedule.scheduleId) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onShowCreate,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Accent500,
            contentColor = Brand900
        ) {
            Icon(Icons.Default.Add, "Add Schedule")
        }
    }

    // ── Create Schedule Dialog ──
    if (state.showCreateDialog) {
        CreateScheduleDialog(
            state = state,
            onDismiss = onDismissCreate,
            onDayToggled = onDayToggled,
            onHourChanged = onHourChanged,
            onMinuteChanged = onMinuteChanged,
            onLabelChanged = onLabelChanged,
            onTargetStateChanged = onTargetStateChanged,
            onCreate = onCreateSchedule
        )
    }
}

@Composable
private fun ScheduleItem(schedule: ScheduleDto, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (schedule.isEnabled) Brand600 else Neutral700,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Timer,
                    null,
                    tint = if (schedule.isEnabled) Accent500 else Neutral500,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    schedule.label.ifBlank { "Schedule" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral100,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${parseCronToReadable(schedule.cronExpression)} · Switch ${schedule.switchIndex + 1} → ${if (schedule.targetState) "ON" else "OFF"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral400
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (schedule.isEnabled) "Active" else "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (schedule.isEnabled) On500 else Neutral500,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Delete", tint = Danger500, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateScheduleDialog(
    state: SchedulesUiState,
    onDismiss: () -> Unit,
    onDayToggled: (Int) -> Unit,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onLabelChanged: (String) -> Unit,
    onTargetStateChanged: (Boolean) -> Unit,
    onCreate: () -> Unit
) {
    val days = listOf("Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6, "Sun" to 7)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neutral800,
        title = { Text("New Schedule", color = Neutral100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.label,
                    onValueChange = onLabelChanged,
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Time picker (simplified sliders)
                Text("Time", style = MaterialTheme.typography.labelMedium, color = Neutral300)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hour: ${state.selectedHour}", style = MaterialTheme.typography.labelSmall, color = Neutral400)
                        Slider(value = state.selectedHour.toFloat(), onValueChange = { onHourChanged(it.toInt()) }, valueRange = 0f..23f, steps = 22, colors = SliderDefaults.colors(thumbColor = Accent500, activeTrackColor = Accent500))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Minute: ${state.selectedMinute}", style = MaterialTheme.typography.labelSmall, color = Neutral400)
                        Slider(value = state.selectedMinute.toFloat(), onValueChange = { onMinuteChanged(it.toInt()) }, valueRange = 0f..59f, steps = 58, colors = SliderDefaults.colors(thumbColor = Accent500, activeTrackColor = Accent500))
                    }
                }

                // Day selector
                Text("Days", style = MaterialTheme.typography.labelMedium, color = Neutral300)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.forEach { (name, index) ->
                        val selected = state.selectedDays.contains(index)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selected) Accent500 else Neutral700)
                                .clickable { onDayToggled(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name.take(1),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Brand900 else Neutral400,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Target state toggle
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Turn switch:", color = Neutral300)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (state.targetState) "ON" else "OFF", color = if (state.targetState) On500 else Neutral400, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = state.targetState,
                            onCheckedChange = onTargetStateChanged,
                            colors = SwitchDefaults.colors(checkedThumbColor = Neutral900, checkedTrackColor = On500)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = !state.isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = Accent500, contentColor = Brand900)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Brand900, strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Neutral400)
            }
        }
    )
}

@Composable
private fun EmptySchedulesState(onAddClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LottieEmptyState(
                title = "No Schedules",
                message = "Automate your switches on a daily or weekly schedule."
            )
            Button(onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = Accent500, contentColor = Brand900)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Schedule")
            }
        }
    }
}

private fun parseCronToReadable(cron: String): String {
    return try {
        // "cron(min hour ? * DAYS *)" → "08:00 · MON,WED"
        val inner = cron.removePrefix("cron(").removeSuffix(")")
        val parts = inner.split(" ")
        val hour = parts[1].toIntOrNull() ?: return cron
        val minute = parts[0].toIntOrNull() ?: return cron
        val days = parts[4]
        "%02d:%02d · %s".format(hour, minute, days)
    } catch (e: Exception) {
        cron
    }
}
