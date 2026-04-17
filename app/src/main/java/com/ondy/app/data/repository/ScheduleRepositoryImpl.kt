package com.ondy.app.data.repository

import com.ondy.app.data.local.dao.ScheduleTimeDao
import com.ondy.app.data.local.entity.ScheduleTimeEntity
import com.ondy.app.domain.model.ScheduleTime
import com.ondy.app.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleTimeDao: ScheduleTimeDao
) : ScheduleRepository {

    override fun getAllScheduleTimes(): Flow<List<ScheduleTime>> {
        return scheduleTimeDao.getAllScheduleTimes().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllScheduleTimesList(): List<ScheduleTime> {
        return scheduleTimeDao.getAllScheduleTimesList().map { it.toDomain() }
    }

    override suspend fun addScheduleTime(scheduleTime: ScheduleTime): Long {
        return scheduleTimeDao.insert(scheduleTime.toEntity())
    }

    override suspend fun deleteScheduleTime(id: Long) {
        scheduleTimeDao.deleteById(id)
    }

    private fun ScheduleTimeEntity.toDomain(): ScheduleTime {
        return ScheduleTime(
            id = id,
            hour = hour,
            minute = minute
        )
    }

    private fun ScheduleTime.toEntity(): ScheduleTimeEntity {
        return ScheduleTimeEntity(
            id = id,
            hour = hour,
            minute = minute
        )
    }
}