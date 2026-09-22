package com.toolbox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.toolbox.data.local.dao.*
import com.toolbox.data.local.entity.*

@Database(
    entities = [
        Note::class,
        Folder::class,
        Tag::class,
        NoteTagCrossRef::class,
        Habit::class,
        HabitRecord::class,
        Category::class,
        Transaction::class,
        OcrHistory::class,
        ChatSession::class,
        ChatMessageEntity::class,
        Schedule::class,
        PasswordEntry::class,
        Measurement::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun habitDao(): HabitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun ocrHistoryDao(): OcrHistoryDao
    abstract fun chatDao(): ChatDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun passwordDao(): PasswordDao
    abstract fun measurementDao(): MeasurementDao
}
