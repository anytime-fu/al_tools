package com.toolbox

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setupExceptionHandler()
    }

    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isDbError = isDatabaseError(throwable)

            if (isDbError) {
                Log.e("App", "Database error detected, resetting database...", throwable)
                resetDatabase()
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun isDatabaseError(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            val message = current.message?.lowercase() ?: ""
            val className = current.javaClass.name.lowercase()

            if (message.contains("room") ||
                message.contains("database") ||
                message.contains("sqlite") ||
                message.contains("sql") ||
                message.contains("corrupt") ||
                message.contains("malformed") ||
                className.contains("room") ||
                className.contains("sqlite")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun resetDatabase() {
        try {
            val dbFile = getDatabasePath("toolbox_database")
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            val journalFile = File(dbFile.path + "-journal")

            listOf(dbFile, walFile, shmFile, journalFile).forEach { file ->
                if (file.exists()) {
                    file.delete()
                    Log.i("App", "Deleted: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("App", "Failed to reset database", e)
        }
    }
}
