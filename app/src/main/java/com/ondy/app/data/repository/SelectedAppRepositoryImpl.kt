package com.ondy.app.data.repository

import com.ondy.app.data.local.dao.SelectedAppDao
import com.ondy.app.data.local.entity.SelectedAppEntity
import com.ondy.app.domain.repository.SelectedAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedAppRepositoryImpl @Inject constructor(
    private val selectedAppDao: SelectedAppDao
) : SelectedAppRepository {

    override fun getSelectedApps(): Flow<List<String>> {
        return selectedAppDao.getAllSelectedApps().map { list ->
            list.map { it.packageName }
        }
    }

    override suspend fun getSelectedAppsList(): List<String> {
        return selectedAppDao.getAllSelectedAppsList().map { it.packageName }
    }

    override suspend fun toggleAppSelection(packageName: String) {
        val isSelected = selectedAppDao.isAppSelected(packageName)
        if (isSelected) {
            selectedAppDao.deleteByPackageName(packageName)
        } else {
            selectedAppDao.insert(SelectedAppEntity(packageName))
        }
    }

    override suspend fun isAppSelected(packageName: String): Boolean {
        return selectedAppDao.isAppSelected(packageName)
    }
}