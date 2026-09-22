package com.toolbox.data.local.dao

import androidx.room.*
import com.toolbox.data.local.entity.Schedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE date = :date ORDER BY time ASC")
    fun getSchedulesByDate(date: Long): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    fun getSchedulesBetween(startDate: Long, endDate: Long): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules ORDER BY date DESC, time DESC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
