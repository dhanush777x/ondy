package com.ondy.app.ui.screens.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ondy.app.domain.model.ScheduleTime
import com.ondy.app.domain.repository.ScheduleRepository
import com.ondy.app.util.ScheduleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        viewModelScope.launch {
            scheduleRepository.getAllScheduleTimes().collect { times ->
                _uiState.update { it.copy(scheduleTimes = times, isLoading = false) }
                // Re-registering is idempotent: ScheduleManager replaces the
                // existing PendingIntent for the same hour/minute.
                times.forEach(scheduleManager::scheduleNotification)
            }
        }
    }

    fun addScheduleTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            runCatching {
                val scheduleTime = ScheduleTime(hour = hour, minute = minute)
                val id = scheduleRepository.addScheduleTime(scheduleTime)
                scheduleManager.scheduleNotification(scheduleTime.copy(id = id))
            }.onFailure { Log.e(TAG, "Failed to add schedule time", it) }
        }
    }

    fun deleteScheduleTime(id: Long) {
        viewModelScope.launch {
            runCatching {
                scheduleManager.cancelNotification(id)
                scheduleRepository.deleteScheduleTime(id)
            }.onFailure { Log.e(TAG, "Failed to delete schedule time", it) }
        }
    }

    companion object {
        private const val TAG = "ScheduleViewModel"
    }
}
