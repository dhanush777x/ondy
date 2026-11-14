package com.ondy.app.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ondy.app.domain.model.AppInfo
import com.ondy.app.domain.repository.SelectedAppRepository
import com.ondy.app.util.AppCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilter {
    ALL, USER, SYSTEM
}

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val currentFilter: AppFilter = AppFilter.USER,
    val isLoading: Boolean = true
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val selectedAppRepository: SelectedAppRepository,
    private val appCache: AppCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    private var allApps: List<AppInfo> = emptyList()

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val selectedPackages = selectedAppRepository.getSelectedAppsList().toSet()
                appCache.ensureLoaded()
                appCache.getAllApps().map { app ->
                    app.copy(isSelected = app.packageName in selectedPackages)
                }
            }.onSuccess { apps ->
                allApps = apps
                _uiState.update { it.copy(apps = apps) }
                applyFilters()
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setFilter(filter: AppFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
        applyFilters()
    }

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            runCatching { selectedAppRepository.toggleAppSelection(packageName) }
                .onSuccess {
                    allApps = allApps.map { app ->
                        if (app.packageName == packageName) {
                            app.copy(isSelected = !app.isSelected)
                        } else {
                            app
                        }
                    }
                    _uiState.update { it.copy(apps = allApps) }
                    applyFilters()
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = when (state.currentFilter) {
            AppFilter.ALL -> allApps
            AppFilter.USER -> allApps.filter { it.isUserApp }
            AppFilter.SYSTEM -> allApps.filter { !it.isUserApp }
        }.let { apps ->
            if (state.searchQuery.isBlank()) {
                apps
            } else {
                apps.filter { it.appName.contains(state.searchQuery, ignoreCase = true) }
            }
        }

        _uiState.update { it.copy(filteredApps = filtered) }
    }
}
