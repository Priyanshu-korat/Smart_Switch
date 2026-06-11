package com.smarthome.app.ui.schedules

import com.smarthome.core.network.dto.ScheduleDto

data class SchedulesUiState(
    val isLoading: Boolean = true,
    val schedules: List<ScheduleDto> = emptyList(),
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    // Dialog form state
    val selectedDeviceId: String = "",
    val selectedSwitchIndex: Int = 0,
    val targetState: Boolean = true,
    val label: String = "",
    val selectedHour: Int = 8,
    val selectedMinute: Int = 0,
    val selectedDays: Set<Int> = emptySet() // 1=Mon .. 7=Sun
)
