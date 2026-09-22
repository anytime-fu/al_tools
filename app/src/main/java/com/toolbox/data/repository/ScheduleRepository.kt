package com.toolbox.data.repository

import com.toolbox.data.local.dao.ScheduleDao
import com.toolbox.data.local.entity.Schedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao
) {
    fun getSchedulesByDate(date: Long): Flow<List<Schedule>> {
        return scheduleDao.getSchedulesByDate(date)
    }

    fun getSchedulesBetween(startDate: Long, endDate: Long): Flow<List<Schedule>> {
        return scheduleDao.getSchedulesBetween(startDate, endDate)
    }

    fun getAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedules()
    }

    suspend fun insert(schedule: Schedule): Long {
        return scheduleDao.insert(schedule)
    }

    suspend fun update(schedule: Schedule) {
        scheduleDao.update(schedule)
    }

    suspend fun delete(schedule: Schedule) {
        scheduleDao.delete(schedule)
    }

    suspend fun deleteById(id: Long) {
        scheduleDao.deleteById(id)
    }
}
