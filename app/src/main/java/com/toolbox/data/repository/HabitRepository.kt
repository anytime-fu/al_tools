package com.toolbox.data.repository

import com.toolbox.data.local.dao.HabitDao
import com.toolbox.data.local.entity.Habit
import com.toolbox.data.local.entity.HabitRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao
) {
    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun getHabitById(id: Long): Habit? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun toggleHabitForDate(habitId: Long, date: LocalDate): Boolean {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = habitDao.getRecordForDate(habitId, dateStr)

        return if (existing != null) {
            habitDao.deleteRecord(existing)
            false
        } else {
            habitDao.insertRecord(
                HabitRecord(
                    habitId = habitId,
                    date = dateStr,
                    completed = true
                )
            )
            true
        }
    }

    suspend fun isHabitCompletedForDate(habitId: Long, date: LocalDate): Boolean {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return habitDao.getRecordForDate(habitId, dateStr) != null
    }

    fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecord>> =
        habitDao.getRecordsForHabit(habitId)
}
