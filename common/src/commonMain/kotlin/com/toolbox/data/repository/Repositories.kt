package com.toolbox.data.repository

import com.toolbox.data.local.entity.*
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNotesByFolder(folderId: Long): Flow<List<Note>>
    fun getNotesByTag(tagId: Long): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun deleteNoteById(id: Long)
}

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun getFolderById(id: Long): Folder?
    suspend fun insertFolder(folder: Folder): Long
    suspend fun updateFolder(folder: Folder)
    suspend fun deleteFolder(folder: Folder)
}

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun getTagById(id: Long): Tag?
    suspend fun insertTag(tag: Tag): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
}

interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    suspend fun getHabitById(id: Long): Habit?
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun toggleHabitForDate(habitId: Long, date: Long): Boolean
    suspend fun isHabitCompletedForDate(habitId: Long, date: Long): Boolean
    fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecord>>
}

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByType(type: String): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    fun getCategoriesByType(type: String): Flow<List<Category>>
    fun getAllCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun deleteCategory(category: Category)
}

interface ChatRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getSessionById(id: Long): ChatSession?
    suspend fun insertSession(session: ChatSession): Long
    suspend fun updateSession(session: ChatSession)
    suspend fun deleteSession(session: ChatSession)
    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessageEntity>>
    suspend fun getMessagesBySessionIdSync(sessionId: Long): List<ChatMessageEntity>
    suspend fun insertMessage(message: ChatMessageEntity): Long
    suspend fun deleteMessagesBySessionId(sessionId: Long)
}

interface ScheduleRepository {
    fun getAllSchedules(): Flow<List<Schedule>>
    suspend fun getScheduleById(id: Long): Schedule?
    fun getSchedulesByDate(date: Long): Flow<List<Schedule>>
    suspend fun insertSchedule(schedule: Schedule): Long
    suspend fun updateSchedule(schedule: Schedule)
    suspend fun deleteSchedule(schedule: Schedule)
}

interface PasswordRepository {
    fun getAllPasswordEntries(): Flow<List<PasswordEntry>>
    suspend fun getPasswordEntryById(id: Long): PasswordEntry?
    fun searchPasswordEntries(query: String): Flow<List<PasswordEntry>>
    suspend fun insertPasswordEntry(entry: PasswordEntry): Long
    suspend fun updatePasswordEntry(entry: PasswordEntry)
    suspend fun deletePasswordEntry(entry: PasswordEntry)
}

interface OcrHistoryRepository {
    fun getAllOcrHistory(): Flow<List<OcrHistory>>
    suspend fun getOcrHistoryById(id: Long): OcrHistory?
    suspend fun insertOcrHistory(history: OcrHistory): Long
    suspend fun deleteOcrHistory(history: OcrHistory)
}

interface MeasurementRepository {
    fun getAllMeasurements(): Flow<List<Measurement>>
    suspend fun getMeasurementById(id: Long): Measurement?
    fun getMeasurementsByType(type: String): Flow<List<Measurement>>
    suspend fun insertMeasurement(measurement: Measurement): Long
    suspend fun deleteMeasurement(measurement: Measurement)
}

interface SettingsRepository {
    fun getAllSettings(): Flow<List<Setting>>
    suspend fun getSettingByKey(key: String): Setting?
    suspend fun getSettingValue(key: String, defaultValue: String = ""): String
    suspend fun saveSetting(key: String, value: String)
    suspend fun deleteSetting(key: String)
}
