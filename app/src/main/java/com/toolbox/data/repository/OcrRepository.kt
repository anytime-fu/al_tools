package com.toolbox.data.repository

import com.toolbox.data.local.dao.OcrHistoryDao
import com.toolbox.data.local.entity.OcrHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepository @Inject constructor(
    private val ocrHistoryDao: OcrHistoryDao
) {
    fun getAllHistory(): Flow<List<OcrHistory>> = ocrHistoryDao.getAllHistory()

    suspend fun getHistoryById(id: Long): OcrHistory? = ocrHistoryDao.getHistoryById(id)

    suspend fun insertHistory(history: OcrHistory): Long = ocrHistoryDao.insertHistory(history)

    suspend fun deleteHistory(history: OcrHistory) = ocrHistoryDao.deleteHistory(history)

    suspend fun clearHistory() = ocrHistoryDao.clearHistory()
}
