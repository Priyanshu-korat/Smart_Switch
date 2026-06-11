package com.smarthome.app.ui.schedules

import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.error.DomainResult
import com.smarthome.core.network.ApiService
import com.smarthome.core.network.dto.CreateScheduleRequest
import com.smarthome.core.network.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val apiService: ApiService
) : BaseViewModel<SchedulesUiState>(SchedulesUiState()) {

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            setState { it.copy(isLoading = true) }
            when (val result = safeApiCall { apiService.getSchedules() }) {
                is DomainResult.Success -> setState {
                    it.copy(isLoading = false, schedules = result.data.items)
                }
                is DomainResult.Error -> {
                    setState { it.copy(isLoading = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
                }
            }
        }
    }

    fun onShowCreateDialog() = setState { it.copy(showCreateDialog = true) }
    fun onDismissDialog() = setState { it.copy(showCreateDialog = false) }

    fun onLabelChanged(label: String) = setState { it.copy(label = label) }
    fun onHourChanged(hour: Int) = setState { it.copy(selectedHour = hour) }
    fun onMinuteChanged(minute: Int) = setState { it.copy(selectedMinute = minute) }
    fun onTargetStateChanged(state: Boolean) = setState { it.copy(targetState = state) }
    fun onDayToggled(day: Int) = setState {
        val days = it.selectedDays.toMutableSet()
        if (days.contains(day)) days.remove(day) else days.add(day)
        it.copy(selectedDays = days)
    }

    fun onCreateSchedule() {
        val state = uiState.value
        if (state.selectedDeviceId.isBlank() || state.selectedDays.isEmpty()) {
            sendEvent(UiEvent.ShowSnackbar("Please fill all fields and select at least one day"))
            return
        }

        val cron = buildCronExpression(
            state.selectedMinute,
            state.selectedHour,
            state.selectedDays
        )

        setState { it.copy(isCreating = true) }
        viewModelScope.launch {
            val request = CreateScheduleRequest(
                deviceId = state.selectedDeviceId,
                switchIndex = state.selectedSwitchIndex,
                targetState = state.targetState,
                cronExpression = cron,
                label = state.label.ifBlank { "Schedule" }
            )
            when (val result = safeApiCall { apiService.createSchedule(request) }) {
                is DomainResult.Success -> {
                    setState {
                        it.copy(
                            isCreating = false,
                            showCreateDialog = false,
                            schedules = it.schedules + result.data
                        )
                    }
                    sendEvent(UiEvent.ShowSnackbar("Schedule created!"))
                }
                is DomainResult.Error -> {
                    setState { it.copy(isCreating = false) }
                    sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
                }
            }
        }
    }

    fun onDeleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            when (val result = safeApiCall { apiService.deleteSchedule(scheduleId) }) {
                is DomainResult.Success -> {
                    setState { it.copy(schedules = it.schedules.filter { s -> s.scheduleId != scheduleId }) }
                }
                is DomainResult.Error ->
                    sendEvent(UiEvent.ShowSnackbar(result.error.userMessage))
            }
        }
    }

    /**
     * Builds an AWS EventBridge / cron expression.
     * days: 1=Mon, 2=Tue, ... 7=Sun (ISO 8601)
     */
    private fun buildCronExpression(minute: Int, hour: Int, days: Set<Int>): String {
        val dayNames = mapOf(1 to "MON", 2 to "TUE", 3 to "WED", 4 to "THU", 5 to "FRI", 6 to "SAT", 7 to "SUN")
        val dayString = days.sorted().mapNotNull { dayNames[it] }.joinToString(",")
        return "cron($minute $hour ? * $dayString *)"
    }
}
