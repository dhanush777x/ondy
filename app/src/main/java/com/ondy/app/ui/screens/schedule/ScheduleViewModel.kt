package com.ondy.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ondy.app.domain.model.ScheduleTime
import com.ondy.app.domain.repository.ScheduleRepository
import com.ondy.app.util.ScheduleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val scheduleTimes: List<ScheduleTime> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleManager: ScheduleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadScheduleTimes()
    }

    private fun loadScheduleTimes() {
        viewModelScope.launch {
            scheduleRepository.getAllScheduleTimes().collect { times ->
                _uiState.value = _uiState.value.copy(
                    scheduleTimes = times,
                    isLoading = false
                )
                
                // Schedule alarms for all times
                times.forEach { time ->
                    scheduleManager.scheduleNotification(time)
                }
            }
        }
    }

    fun addScheduleTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val scheduleTime = ScheduleTime(hour = hour, minute = minute)
            val id = scheduleRepository.addScheduleTime(scheduleTime)
            val savedTime = scheduleTime.copy(id = id)
            
            // Immediately schedule the alarm
            scheduleManager.scheduleNotification(savedTime)
            
            // Update UI immediately
            val currentTimes = _uiState.value.scheduleTimes.toMutableList()
            currentTimes.add(savedTime)
            _uiState.value = _uiState.value.copy(scheduleTimes = currentTimes)
        }
    }

    fun deleteScheduleTime(id: Long) {
        viewModelScope.launch {
            scheduleManager.cancelNotification(id)
            scheduleRepository.deleteScheduleTime(id)
            val currentTimes = _uiState.value.scheduleTimes.filter { it.id != id }
            _uiState.value = _uiState.value.copy(scheduleTimes = currentTimes)
        }
    }
}