package com.toolbox.data.local.dao

import androidx.room.*
import com.toolbox.data.local.entity.Habit
import com.toolbox.data.local.entity.HabitRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun getRecordForDate(habitId: Long, date: String): HabitRecord?

    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date DESC")
    fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HabitRecord): Long

    @Delete
    suspend fun deleteRecord(record: HabitRecord)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun deleteRecordForDate(habitId: Long, date: String)
}
