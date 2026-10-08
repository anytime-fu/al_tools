package com.toolbox.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

fun createInMemoryDatabase(): ToolboxDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ToolboxDatabase.Schema.create(driver)
    return ToolboxDatabase(driver)
}