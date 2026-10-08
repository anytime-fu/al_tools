package com.toolbox.data.local

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseMigratorTest {

    private class FakeSchema(
        override val version: Long
    ) : SqlSchema<QueryResult.Value<Unit>> {
        var created = false
        var migratedFrom: Long? = null
        var migratedTo: Long? = null

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            created = true
            driver.execute(null, "CREATE TABLE IF NOT EXISTS t (id INTEGER PRIMARY KEY)", 0)
            return QueryResult.Unit
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion
        ): QueryResult.Value<Unit> {
            migratedFrom = oldVersion
            migratedTo = newVersion
            driver.execute(null, "CREATE TABLE IF NOT EXISTS migrated (id INTEGER PRIMARY KEY)", 0)
            return QueryResult.Unit
        }
    }

    @Test
    fun freshDatabaseIsCreatedAndStamped() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val schema = FakeSchema(version = 3)
        DatabaseMigrator.migrate(driver, schema)
        assertTrue(schema.created)
        assertEquals(3L, DatabaseMigrator.readUserVersion(driver))
    }

    @Test
    fun legacyUnversionedDatabaseWithTablesIsNotRecreated() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE legacy (id INTEGER PRIMARY KEY)", 0)
        val schema = FakeSchema(version = 1)
        DatabaseMigrator.migrate(driver, schema)
        assertFalse(schema.created)
        assertEquals(1L, DatabaseMigrator.readUserVersion(driver))
    }

    @Test
    fun legacyUnversionedDatabaseGetsMigrationsFromBaseline() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE legacy (id INTEGER PRIMARY KEY)", 0)
        val schema = FakeSchema(version = 3)
        DatabaseMigrator.migrate(driver, schema)
        assertEquals(1L, schema.migratedFrom)
        assertEquals(3L, schema.migratedTo)
        assertEquals(3L, DatabaseMigrator.readUserVersion(driver))
    }

    @Test
    fun outdatedDatabaseIsMigrated() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val schemaV1 = FakeSchema(version = 1)
        DatabaseMigrator.migrate(driver, schemaV1)
        val schemaV2 = FakeSchema(version = 2)
        DatabaseMigrator.migrate(driver, schemaV2)
        assertEquals(1L, schemaV2.migratedFrom)
        assertEquals(2L, schemaV2.migratedTo)
        assertEquals(2L, DatabaseMigrator.readUserVersion(driver))
    }

    @Test
    fun upToDateDatabaseIsUntouched() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val schema = FakeSchema(version = 1)
        DatabaseMigrator.migrate(driver, schema)
        schema.created = false
        DatabaseMigrator.migrate(driver, schema)
        assertFalse(schema.created)
        assertNull(schema.migratedFrom)
    }

    @Test
    fun downgradeIsRejected() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val schemaV5 = FakeSchema(version = 5)
        DatabaseMigrator.migrate(driver, schemaV5)
        val schemaV3 = FakeSchema(version = 3)
        assertFailsWith<IllegalStateException> {
            DatabaseMigrator.migrate(driver, schemaV3)
        }
    }
}