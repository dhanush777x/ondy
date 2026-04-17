package com.ondy.app.ui.screens.apps

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ondy.app.domain.model.AppInfo
import com.ondy.app.domain.repository.SelectedAppRepository
import com.ondy.app.service.NotificationInterceptorService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    private var allApps: List<AppInfo> = emptyList()
    val packageManager: PackageManager = application.packageManager

    fun loadApps(hasAccess: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val selectedPackages = if (hasAccess) {
                selectedAppRepository.getSelectedAppsList().toSet()
            } else {
                emptySet()
            }

            val pm = packageManager
            val apps = withContext(Dispatchers.IO) {
                try {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .mapNotNull { appInfo ->
                            try {
                                val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                                AppInfo(
                                    packageName = appInfo.packageName,
                                    appName = pm.getApplicationLabel(appInfo).toString(),
                                    isSelected = appInfo.packageName in selectedPackages,
                                    isUserApp = isUserApp
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        .sortedBy { it.appName.lowercase() }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            allApps = apps
            applyFilters()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setNotificationAccess(hasAccess: Boolean) {
        if (hasAccess) {
            refreshSelectedApps()
        }
    }

    fun setFilter(filter: AppFilter) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
        applyFilters()
    }

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            selectedAppRepository.toggleAppSelection(packageName)
            val updatedApps = allApps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isSelected = !app.isSelected)
                } else app
            }
            allApps = updatedApps
            applyFilters()
            sendRefreshBroadcast()
        }
    }

    private fun sendRefreshBroadcast() {
        val intent = Intent(NotificationInterceptorService.ACTION_REFRESH_APPS)
        intent.setPackage(getApplication<Application>().packageName)
        getApplication<Application>().sendBroadcast(intent)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery
        val filter = _uiState.value.currentFilter

        var filtered = when (filter) {
            AppFilter.ALL -> allApps
            AppFilter.USER -> allApps.filter { it.isUserApp }
            AppFilter.SYSTEM -> allApps.filter { !it.isUserApp }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter { it.appName.contains(query, ignoreCase = true) }
        }

        _uiState.value = _uiState.value.copy(filteredApps = filtered)
    }

    private fun refreshSelectedApps() {
        viewModelScope.launch {
            val selectedPackages = selectedAppRepository.getSelectedAppsList().toSet()
            val updatedApps = allApps.map { app ->
                app.copy(isSelected = app.packageName in selectedPackages)
            }
            allApps = updatedApps
            applyFilters()
        }
    }
}