package com.toolbox.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.toolbox.data.local.CryptoManager
import com.toolbox.data.local.ToolboxDatabase
import com.toolbox.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightNoteRepository(
    private val database: ToolboxDatabase
) : NoteRepository {
    
    override fun getAllNotes(): Flow<List<Note>> {
        return database.toolboxQueries.getAllNotes()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notesList ->
                notesList.map { row ->
                    Note(
                        id = row.id,
                        title = row.title,
                        content = row.content,
                        folderId = row.folder_id,
                        isPinned = row.is_pinned == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override fun searchNotes(query: String): Flow<List<Note>> {
        return database.toolboxQueries.searchNotes(query, query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notesList ->
                notesList.map { row ->
                    Note(
                        id = row.id,
                        title = row.title,
                        content = row.content,
                        folderId = row.folder_id,
                        isPinned = row.is_pinned == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override fun getNotesByFolder(folderId: Long): Flow<List<Note>> {
        return database.toolboxQueries.getNotesByFolder(folderId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notesList ->
                notesList.map { row ->
                    Note(
                        id = row.id,
                        title = row.title,
                        content = row.content,
                        folderId = row.folder_id,
                        isPinned = row.is_pinned == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override fun getNotesByTag(tagId: Long): Flow<List<Note>> {
        return database.toolboxQueries.getNotesByTagId(tagId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notesList ->
                notesList.map { row ->
                    Note(
                        id = row.id,
                        title = row.title,
                        content = row.content,
                        folderId = row.folder_id,
                        isPinned = row.is_pinned == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override suspend fun getNoteById(id: Long): Note? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getNoteById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    Note(
                        id = row.id,
                        title = row.title,
                        content = row.content,
                        folderId = row.folder_id,
                        isPinned = row.is_pinned == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
        }
    }
    
    override suspend fun insertNote(note: Note): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertNote(
                title = note.title,
                content = note.content,
                folder_id = note.folderId,
                is_pinned = if (note.isPinned) 1L else 0L,
                created_at = note.createdAt,
                updated_at = note.updatedAt
            )
            // Get the last inserted ID using a separate query
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateNote(note: Note) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updateNote(
                title = note.title,
                content = note.content,
                folder_id = note.folderId,
                is_pinned = if (note.isPinned) 1L else 0L,
                updated_at = note.updatedAt,
                id = note.id
            )
        }
    }
    
    override suspend fun deleteNote(note: Note) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteNote(note.id)
        }
    }
    
    override suspend fun deleteNoteById(id: Long) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteNoteById(id)
        }
    }
}

class SqlDelightFolderRepository(
    private val database: ToolboxDatabase
) : FolderRepository {
    
    override fun getAllFolders(): Flow<List<Folder>> {
        return database.toolboxQueries.getAllFolders()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { foldersList ->
                foldersList.map { row ->
                    Folder(
                        id = row.id,
                        name = row.name,
                        parentId = row.parent_id,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override suspend fun getFolderById(id: Long): Folder? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getFolderById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    Folder(
                        id = row.id,
                        name = row.name,
                        parentId = row.parent_id,
                        createdAt = row.created_at
                    )
                }
        }
    }
    
    override suspend fun insertFolder(folder: Folder): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertFolder(
                name = folder.name,
                parent_id = folder.parentId,
                created_at = folder.createdAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateFolder(folder: Folder) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updateFolder(
                name = folder.name,
                parent_id = folder.parentId,
                id = folder.id
            )
        }
    }
    
    override suspend fun deleteFolder(folder: Folder) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteFolder(folder.id)
        }
    }
}

class SqlDelightTagRepository(
    private val database: ToolboxDatabase
) : TagRepository {
    
    override fun getAllTags(): Flow<List<Tag>> {
        return database.toolboxQueries.getAllTags()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { tagsList ->
                tagsList.map { row ->
                    Tag(
                        id = row.id,
                        name = row.name,
                        color = row.color
                    )
                }
            }
    }
    
    override suspend fun getTagById(id: Long): Tag? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getTagById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    Tag(
                        id = row.id,
                        name = row.name,
                        color = row.color
                    )
                }
        }
    }
    
    override suspend fun insertTag(tag: Tag): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertTag(
                name = tag.name,
                color = tag.color
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateTag(tag: Tag) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updateTag(
                name = tag.name,
                color = tag.color,
                id = tag.id
            )
        }
    }
    
    override suspend fun deleteTag(tag: Tag) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteTag(tag.id)
        }
    }
}

class SqlDelightHabitRepository(
    private val database: ToolboxDatabase
) : HabitRepository {
    
    override fun getAllHabits(): Flow<List<Habit>> {
        return database.toolboxQueries.getAllHabits()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { habitsList ->
                habitsList.map { row ->
                    Habit(
                        id = row.id,
                        name = row.name,
                        icon = row.icon,
                        color = row.color,
                        frequency = row.frequency,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override suspend fun getHabitById(id: Long): Habit? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getHabitById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    Habit(
                        id = row.id,
                        name = row.name,
                        icon = row.icon,
                        color = row.color,
                        frequency = row.frequency,
                        createdAt = row.created_at
                    )
                }
        }
    }
    
    override suspend fun insertHabit(habit: Habit): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertHabit(
                name = habit.name,
                icon = habit.icon,
                color = habit.color,
                frequency = habit.frequency,
                created_at = habit.createdAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateHabit(habit: Habit) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updateHabit(
                name = habit.name,
                icon = habit.icon,
                color = habit.color,
                frequency = habit.frequency,
                id = habit.id
            )
        }
    }
    
    override suspend fun deleteHabit(habit: Habit) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteHabit(habit.id)
        }
    }
    
    override suspend fun toggleHabitForDate(habitId: Long, date: Long): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = database.toolboxQueries.getHabitRecordByDate(habitId, date).executeAsOneOrNull()
            if (existing != null) {
                database.toolboxQueries.deleteHabitRecord(existing.id)
                false
            } else {
                database.toolboxQueries.insertHabitRecord(
                    habit_id = habitId,
                    date = date,
                    is_completed = 1L
                )
                true
            }
        }
    }
    
    override suspend fun isHabitCompletedForDate(habitId: Long, date: Long): Boolean {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getHabitRecordByDate(habitId, date).executeAsOneOrNull() != null
        }
    }
    
    override fun getRecordsForHabit(habitId: Long): Flow<List<HabitRecord>> {
        return database.toolboxQueries.getHabitRecords(habitId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { recordsList ->
                recordsList.map { row ->
                    HabitRecord(
                        id = row.id,
                        habitId = row.habit_id,
                        date = row.date,
                        isCompleted = row.is_completed == 1L
                    )
                }
            }
    }
}

class SqlDelightTransactionRepository(
    private val database: ToolboxDatabase
) : TransactionRepository {
    
    override fun getAllTransactions(): Flow<List<Transaction>> {
        return database.toolboxQueries.getAllTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { transactionsList ->
                transactionsList.map { row ->
                    Transaction(
                        id = row.id,
                        amount = row.amount,
                        type = row.type,
                        categoryId = row.category_id,
                        note = row.note,
                        date = row.date,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override fun getTransactionsByType(type: String): Flow<List<Transaction>> {
        return database.toolboxQueries.getTransactionsByType(type)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { transactionsList ->
                transactionsList.map { row ->
                    Transaction(
                        id = row.id,
                        amount = row.amount,
                        type = row.type,
                        categoryId = row.category_id,
                        note = row.note,
                        date = row.date,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return database.toolboxQueries.getTransactionsByDateRange(startDate, endDate)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { transactionsList ->
                transactionsList.map { row ->
                    Transaction(
                        id = row.id,
                        amount = row.amount,
                        type = row.type,
                        categoryId = row.category_id,
                        note = row.note,
                        date = row.date,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override suspend fun getTransactionById(id: Long): Transaction? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getTransactionById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    Transaction(
                        id = row.id,
                        amount = row.amount,
                        type = row.type,
                        categoryId = row.category_id,
                        note = row.note,
                        date = row.date,
                        createdAt = row.created_at
                    )
                }
        }
    }
    
    override suspend fun insertTransaction(transaction: Transaction): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertTransaction(
                amount = transaction.amount,
                type = transaction.type,
                category_id = transaction.categoryId,
                note = transaction.note,
                date = transaction.date,
                created_at = transaction.createdAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updateTransaction(
                amount = transaction.amount,
                type = transaction.type,
                category_id = transaction.categoryId,
                note = transaction.note,
                date = transaction.date,
                id = transaction.id
            )
        }
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteTransaction(transaction.id)
        }
    }
    
    override fun getCategoriesByType(type: String): Flow<List<Category>> {
        return database.toolboxQueries.getCategoriesByType(type)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { categoriesList ->
                categoriesList.map { row ->
                    Category(
                        id = row.id,
                        name = row.name,
                        type = row.type,
                        icon = row.icon,
                        color = row.color,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override fun getAllCategories(): Flow<List<Category>> {
        return database.toolboxQueries.getAllCategories()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { categoriesList ->
                categoriesList.map { row ->
                    Category(
                        id = row.id,
                        name = row.name,
                        type = row.type,
                        icon = row.icon,
                        color = row.color,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override suspend fun insertCategory(category: Category): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertCategory(
                name = category.name,
                type = category.type,
                icon = category.icon,
                color = category.color,
                created_at = category.createdAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun deleteCategory(category: Category) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteCategory(category.id)
        }
    }
}

class SqlDelightScheduleRepository(
    private val database: ToolboxDatabase
) : ScheduleRepository {
    
    override fun getAllSchedules(): Flow<List<Schedule>> {
        return database.toolboxQueries.getAllSchedules()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { schedulesList ->
                schedulesList.map { row ->
                    Schedule(
                        id = row.id,
                        title = row.title,
                        description = row.description,
                        date = row.date,
                        time = row.time,
                        isCompleted = row.is_completed == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override suspend fun getScheduleById(id: Long): Schedule? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getScheduleById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    Schedule(
                        id = row.id,
                        title = row.title,
                        description = row.description,
                        date = row.date,
                        time = row.time,
                        isCompleted = row.is_completed == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
        }
    }
    
    override fun getSchedulesByDate(date: Long): Flow<List<Schedule>> {
        return database.toolboxQueries.getSchedulesByDate(date)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { schedulesList ->
                schedulesList.map { row ->
                    Schedule(
                        id = row.id,
                        title = row.title,
                        description = row.description,
                        date = row.date,
                        time = row.time,
                        isCompleted = row.is_completed == 1L,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override suspend fun insertSchedule(schedule: Schedule): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertSchedule(
                title = schedule.title,
                description = schedule.description,
                date = schedule.date,
                time = schedule.time,
                is_completed = if (schedule.isCompleted) 1L else 0L,
                created_at = schedule.createdAt,
                updated_at = schedule.updatedAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateSchedule(schedule: Schedule) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updateSchedule(
                title = schedule.title,
                description = schedule.description,
                date = schedule.date,
                time = schedule.time,
                is_completed = if (schedule.isCompleted) 1L else 0L,
                updated_at = schedule.updatedAt,
                id = schedule.id
            )
        }
    }
    
    override suspend fun deleteSchedule(schedule: Schedule) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteSchedule(schedule.id)
        }
    }
}

class SqlDelightPasswordRepository(
    private val database: ToolboxDatabase
) : PasswordRepository {
    
    override fun getAllPasswordEntries(): Flow<List<PasswordEntry>> {
        return database.toolboxQueries.getAllPasswordEntries()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entriesList ->
                entriesList.map { row ->
                    PasswordEntry(
                        id = row.id,
                        appName = row.app_name,
                        account = row.account,
                        password = CryptoManager.decrypt(row.password),
                        note = row.note,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override suspend fun getPasswordEntryById(id: Long): PasswordEntry? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getPasswordEntryById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    PasswordEntry(
                        id = row.id,
                        appName = row.app_name,
                        account = row.account,
                        password = CryptoManager.decrypt(row.password),
                        note = row.note,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
        }
    }
    
    override fun searchPasswordEntries(query: String): Flow<List<PasswordEntry>> {
        return database.toolboxQueries.searchPasswordEntries(query, query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entriesList ->
                entriesList.map { row ->
                    PasswordEntry(
                        id = row.id,
                        appName = row.app_name,
                        account = row.account,
                        password = CryptoManager.decrypt(row.password),
                        note = row.note,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override suspend fun insertPasswordEntry(entry: PasswordEntry): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertPasswordEntry(
                app_name = entry.appName,
                account = entry.account,
                password = CryptoManager.encrypt(entry.password),
                note = entry.note,
                created_at = entry.createdAt,
                updated_at = entry.updatedAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updatePasswordEntry(entry: PasswordEntry) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updatePasswordEntry(
                app_name = entry.appName,
                account = entry.account,
                password = CryptoManager.encrypt(entry.password),
                note = entry.note,
                updated_at = entry.updatedAt,
                id = entry.id
            )
        }
    }
    
    override suspend fun deletePasswordEntry(entry: PasswordEntry) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deletePasswordEntry(entry.id)
        }
    }
}

class SqlDelightChatRepository(
    private val database: ToolboxDatabase
) : ChatRepository {
    
    override fun getAllSessions(): Flow<List<ChatSession>> {
        return database.toolboxQueries.getAllChatSessions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sessionsList ->
                sessionsList.map { row ->
                    ChatSession(
                        id = row.id,
                        title = row.title,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override suspend fun getSessionById(id: Long): ChatSession? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getChatSessionById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    ChatSession(
                        id = row.id,
                        title = row.title,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at
                    )
                }
        }
    }
    
    override suspend fun insertSession(session: ChatSession): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertChatSession(
                title = session.title,
                created_at = session.createdAt,
                updated_at = session.updatedAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun updateSession(session: ChatSession) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.updateChatSession(
                title = session.title,
                updated_at = session.updatedAt,
                id = session.id
            )
        }
    }
    
    override suspend fun deleteSession(session: ChatSession) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteChatSession(session.id)
        }
    }
    
    override fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessageEntity>> {
        return database.toolboxQueries.getMessagesBySessionId(sessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { messagesList ->
                messagesList.map { row ->
                    ChatMessageEntity(
                        id = row.id,
                        sessionId = row.session_id,
                        role = row.role,
                        content = row.content,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override suspend fun getMessagesBySessionIdSync(sessionId: Long): List<ChatMessageEntity> {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getMessagesBySessionId(sessionId)
                .executeAsList()
                .map { row ->
                    ChatMessageEntity(
                        id = row.id,
                        sessionId = row.session_id,
                        role = row.role,
                        content = row.content,
                        createdAt = row.created_at
                    )
                }
        }
    }
    
    override suspend fun insertMessage(message: ChatMessageEntity): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertChatMessage(
                session_id = message.sessionId,
                role = message.role,
                content = message.content,
                created_at = message.createdAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }
    
    override suspend fun deleteMessagesBySessionId(sessionId: Long) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteMessagesBySessionId(sessionId)
        }
    }
}

class SqlDelightSettingsRepository(
    private val database: ToolboxDatabase
) : SettingsRepository {
    
    override fun getAllSettings(): Flow<List<Setting>> {
        return database.toolboxQueries.getAllSettings()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { settingsList ->
                settingsList.map { row ->
                    Setting(
                        key = row.key,
                        value = row.value_,
                        updatedAt = row.updated_at
                    )
                }
            }
    }
    
    override suspend fun getSettingByKey(key: String): Setting? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getSettingByKey(key)
                .executeAsOneOrNull()
                ?.let { row ->
                    Setting(
                        key = row.key,
                        value = row.value_,
                        updatedAt = row.updated_at
                    )
                }
        }
    }
    
    override suspend fun getSettingValue(key: String, defaultValue: String): String {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getSettingByKey(key)
                .executeAsOneOrNull()
                ?.value_ ?: defaultValue
        }
    }
    
    override suspend fun saveSetting(key: String, value: String) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.insertOrReplaceSetting(
                key = key,
                value_ = value,
                updated_at = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun deleteSetting(key: String) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteSetting(key)
        }
    }
}

class SqlDelightOcrHistoryRepository(
    private val database: ToolboxDatabase
) : OcrHistoryRepository {

    override fun getAllOcrHistory(): Flow<List<OcrHistory>> {
        return database.toolboxQueries.getAllOcrHistory()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { historyList ->
                historyList.map { row ->
                    OcrHistory(
                        id = row.id,
                        imagePath = row.image_path,
                        recognizedText = row.recognized_text,
                        language = row.language,
                        createdAt = row.created_at
                    )
                }
            }
    }

    override suspend fun getOcrHistoryById(id: Long): OcrHistory? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getOcrHistoryById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    OcrHistory(
                        id = row.id,
                        imagePath = row.image_path,
                        recognizedText = row.recognized_text,
                        language = row.language,
                        createdAt = row.created_at
                    )
                }
        }
    }

    override suspend fun insertOcrHistory(history: OcrHistory): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertOcrHistory(
                image_path = history.imagePath,
                recognized_text = history.recognizedText,
                language = history.language,
                created_at = history.createdAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun deleteOcrHistory(history: OcrHistory) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteOcrHistory(history.id)
        }
    }
}

class SqlDelightMeasurementRepository(
    private val database: ToolboxDatabase
) : MeasurementRepository {

    override fun getAllMeasurements(): Flow<List<Measurement>> {
        return database.toolboxQueries.getAllMeasurements()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { measurementsList ->
                measurementsList.map { row ->
                    Measurement(
                        id = row.id,
                        name = row.name,
                        value = row.value_,
                        unit = row.unit,
                        type = row.type,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override suspend fun getMeasurementById(id: Long): Measurement? {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.getMeasurementById(id)
                .executeAsOneOrNull()
                ?.let { row ->
                    Measurement(
                        id = row.id,
                        name = row.name,
                        value = row.value_,
                        unit = row.unit,
                        type = row.type,
                        createdAt = row.created_at
                    )
                }
        }
    }
    
    override fun getMeasurementsByType(type: String): Flow<List<Measurement>> {
        return database.toolboxQueries.getMeasurementsByType(type)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { measurementsList ->
                measurementsList.map { row ->
                    Measurement(
                        id = row.id,
                        name = row.name,
                        value = row.value_,
                        unit = row.unit,
                        type = row.type,
                        createdAt = row.created_at
                    )
                }
            }
    }
    
    override suspend fun insertMeasurement(measurement: Measurement): Long {
        return withContext(Dispatchers.IO) {
            database.toolboxQueries.insertMeasurement(
                name = measurement.name,
                value_ = measurement.value,
                unit = measurement.unit,
                type = measurement.type,
                created_at = measurement.createdAt
            )
            database.toolboxQueries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun deleteMeasurement(measurement: Measurement) {
        withContext(Dispatchers.IO) {
            database.toolboxQueries.deleteMeasurement(measurement.id)
        }
    }
}
