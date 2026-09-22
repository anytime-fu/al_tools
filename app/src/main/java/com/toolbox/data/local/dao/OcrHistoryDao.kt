package com.toolbox.data.local.dao

import androidx.room.*
import com.toolbox.data.local.entity.OcrHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrHistoryDao {
    @Query("SELECT * FROM ocr_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<OcrHistory>>

    @Query("SELECT * FROM ocr_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): OcrHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: OcrHistory): Long

    @Delete
    suspend fun deleteHistory(history: OcrHistory)

    @Query("DELETE FROM ocr_history")
    suspend fun clearHistory()
}
