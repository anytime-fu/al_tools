package com.toolbox.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

object DatabaseFactory {
    private var database: ToolboxDatabase? = null
    
    fun createDatabase(): ToolboxDatabase {
        if (database != null) return database!!
        
        val dbFile = File(System.getProperty("user.home"), ".ai-toolbox/toolbox.db")
        dbFile.parentFile.mkdirs()
        
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        DatabaseMigrator.migrate(driver, ToolboxDatabase.Schema, dbFile)
        
        database = ToolboxDatabase(driver)
        return database!!
    }
    
    fun closeDatabase() {
        database?.let { (it as? java.io.Closeable)?.close() }
        database = null
    }
}

object DatabaseMigrator {
    
    fun migrate(
        driver: SqlDriver,
        schema: SqlSchema<QueryResult.Value<Unit>>,
        dbFile: File? = null
    ) {
        val currentVersion = readUserVersion(driver)
        when {
            currentVersion > schema.version -> throw IllegalStateException(
                "数据库版本 ($currentVersion) 高于当前应用支持的版本 (${schema.version})，请升级应用后再打开"
            )
            currentVersion == schema.version -> Unit
            currentVersion == 0L && !hasAnyTable(driver) -> {
                schema.create(driver)
                writeUserVersion(driver, schema.version)
            }
            currentVersion == 0L -> {
                if (schema.version > 1) {
                    dbFile?.let { backupBeforeMigrate(it) }
                    schema.migrate(driver, 1, schema.version)
                }
                writeUserVersion(driver, schema.version)
            }
            else -> {
                dbFile?.let { backupBeforeMigrate(it) }
                schema.migrate(driver, currentVersion, schema.version)
                writeUserVersion(driver, schema.version)
            }
        }
    }
    
    fun readUserVersion(driver: SqlDriver): Long {
        return driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 1
        ).value
    }
    
    fun writeUserVersion(driver: SqlDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version;", 0)
    }
    
    private fun hasAnyTable(driver: SqlDriver): Boolean {
        return driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%';",
            mapper = { cursor -> QueryResult.Value((cursor.getLong(0) ?: 0L) > 0L) },
            parameters = 1
        ).value
    }
    
    private fun backupBeforeMigrate(dbFile: File) {
        if (!dbFile.exists()) return
        runCatching {
            val backupFile = File(dbFile.parentFile, "${dbFile.name}.backup-${System.currentTimeMillis()}")
            dbFile.copyTo(backupFile, overwrite = true)
        }
    }
}
