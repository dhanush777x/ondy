package com.ondy.app.ui.screens.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ondy.app.data.backup.BackupCodec
import com.ondy.app.data.preferences.PreferencesManager
import com.ondy.app.domain.model.BackupData
import com.ondy.app.domain.model.ThemeMode
import com.ondy.app.domain.repository.BackupRepository
import com.ondy.app.domain.repository.ScheduleRepository
import com.ondy.app.domain.repository.SelectedAppRepository
import com.ondy.app.util.ScheduleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.MATERIAL,
    val selectedAppsCount: Int = 0,
    val scheduleTimesCount: Int = 0,
    val appVersion: String = "",
    val isBusy: Boolean = false,
    val pendingImport: BackupData? = null
)

sealed interface SettingsEvent {
    data object ExportSucceeded : SettingsEvent
    data object ExportFailed : SettingsEvent
    data object ImportSucceeded : SettingsEvent
    data object ImportFailed : SettingsEvent
    data object InvalidBackup : SettingsEvent
}

/**
 * Owns theme selection and the backup/restore flow.
 *
 * All file and database work happens on [Dispatchers.IO]; the UI only ever
 * observes small state objects. Restoring is a two-step process so the screen
 * can show a confirmation dialog before replacing the user's data.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val preferencesManager: PreferencesManager,
    private val backupRepository: BackupRepository,
    private val scheduleManager: ScheduleManager,
    private val selectedAppRepository: SelectedAppRepository,
    private val scheduleRepository: ScheduleRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(appVersion = readVersionName(application))
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            preferencesManager.themeMode
                .catch { Log.e(TAG, "Failed to observe theme mode", it) }
                .collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            combine(
                selectedAppRepository.getSelectedApps(),
                scheduleRepository.getAllScheduleTimes()
            ) { apps, times -> apps.size to times.size }
                .catch { Log.e(TAG, "Failed to observe backup counts", it) }
                .collect { (apps, times) ->
                    _uiState.update {
                        it.copy(selectedAppsCount = apps, scheduleTimesCount = times)
                    }
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (mode == _uiState.value.themeMode) return
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    /** Writes the current selection and schedule to [uri] as JSON. */
    fun exportTo(uri: Uri) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val json = BackupCodec.encode(backupRepository.createBackup())
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                    } ?: throw IOException("Unable to open $uri for writing")
                }
            }

            _uiState.update { it.copy(isBusy = false) }
            if (result.isFailure) Log.e(TAG, "Backup export failed", result.exceptionOrNull())
            _events.tryEmit(
                if (result.isSuccess) SettingsEvent.ExportSucceeded else SettingsEvent.ExportFailed
            )
        }
    }

    /** Reads and validates a backup, then waits for [confirmImport]. */
    fun importFrom(uri: Uri) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val json = resolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } ?: throw IOException("Unable to read $uri")
                    BackupCodec.decode(json)
                }
            }

            val data = result.getOrNull()
            if (data != null) {
                _uiState.update { it.copy(isBusy = false, pendingImport = data) }
            } else {
                _uiState.update { it.copy(isBusy = false) }
                val error = result.exceptionOrNull()
                Log.w(TAG, "Backup import rejected", error)
                _events.tryEmit(
                    if (error is IllegalArgumentException) {
                        SettingsEvent.InvalidBackup
                    } else {
                        SettingsEvent.ImportFailed
                    }
                )
            }
        }
    }

    /** Applies the parsed backup, replacing all selected apps and schedule times. */
    fun confirmImport() {
        val data = _uiState.value.pendingImport ?: return
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    // Cancel alarms for the old schedule first so times that are
                    // not part of the backup cannot keep firing.
                    scheduleManager.cancelAllAlarms()
                    backupRepository.restoreBackup(data)
                    scheduleManager.rescheduleAllAlarms()
                }
            }

            _uiState.update { it.copy(isBusy = false, pendingImport = null) }
            if (result.isFailure) Log.e(TAG, "Backup restore failed", result.exceptionOrNull())
            _events.tryEmit(
                if (result.isSuccess) SettingsEvent.ImportSucceeded else SettingsEvent.ImportFailed
            )
        }
    }

    fun cancelImport() {
        _uiState.update { it.copy(pendingImport = null) }
    }

    companion object {
        private const val TAG = "SettingsViewModel"

        /** File name suggested in the system "save as" dialog. */
        fun suggestedFileName(now: Long = System.currentTimeMillis()): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now))
            return "ondy-backup-$stamp.json"
        }

        private fun readVersionName(application: Application): String {
            return runCatching {
                @Suppress("DEPRECATION")
                application.packageManager
                    .getPackageInfo(application.packageName, 0)
                    .versionName
            }.getOrNull().orEmpty()
        }
    }
}
