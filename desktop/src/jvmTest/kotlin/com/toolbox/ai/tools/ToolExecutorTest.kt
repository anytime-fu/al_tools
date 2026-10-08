package com.toolbox.ai.tools

import com.toolbox.data.local.ToolboxDatabase
import com.toolbox.data.local.createInMemoryDatabase
import com.toolbox.data.repository.SqlDelightHabitRepository
import com.toolbox.data.repository.SqlDelightNoteRepository
import com.toolbox.data.repository.SqlDelightPasswordRepository
import com.toolbox.data.repository.SqlDelightScheduleRepository
import com.toolbox.data.repository.SqlDelightSettingsRepository
import com.toolbox.data.repository.SqlDelightTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolExecutorTest {

    private data class Fixture(val executor: ToolExecutor, val db: ToolboxDatabase)

    private fun buildFixture(): Fixture {
        val db = createInMemoryDatabase()
        val executor = ToolExecutor(
            noteRepository = SqlDelightNoteRepository(db),
            transactionRepository = SqlDelightTransactionRepository(db),
            habitRepository = SqlDelightHabitRepository(db),
            settingsRepository = SqlDelightSettingsRepository(db),
            passwordRepository = SqlDelightPasswordRepository(db),
            scheduleRepository = SqlDelightScheduleRepository(db)
        )
        return Fixture(executor, db)
    }

    private fun call(name: String, args: String): ToolCall {
        return ToolCall(id = "t1", function = ToolCallFunction(name = name, arguments = args))
    }

    @Test
    fun createNoteWritesToRepository() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(
            call("create_note", """{"title":"标题","content":"正文"}""")
        )
        assertFalse(result.isError)
        val notes = SqlDelightNoteRepository(fixture.db).getAllNotes().first()
        assertEquals(1, notes.size)
        assertEquals("标题", notes.first().title)
    }

    @Test
    fun createNoteRejectsEmptyTitle() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(
            call("create_note", """{"title":"","content":"x"}""")
        )
        assertTrue(result.isError)
    }

    @Test
    fun addTransactionThenQuery() = runBlocking {
        val fixture = buildFixture()
        val add = fixture.executor.executeTool(
            call("add_transaction", """{"amount":12.5,"type":"expense","category":"餐饮"}""")
        )
        assertFalse(add.isError)
        val query = fixture.executor.executeTool(
            call("query_transactions", """{"type":"expense","period":"month"}""")
        )
        assertFalse(query.isError)
        assertTrue(query.result.contains("12.50"))
        val summary = fixture.executor.executeTool(
            call("get_expense_summary", """{"period":"month"}""")
        )
        assertFalse(summary.isError)
        assertTrue(summary.result.contains("餐饮"))
    }

    @Test
    fun addTodoAppearsInTodoList() = runBlocking {
        val fixture = buildFixture()
        val add = fixture.executor.executeTool(call("add_todo", """{"title":"写周报"}"""))
        assertFalse(add.isError)
        val list = fixture.executor.executeTool(call("get_todos", "{}"))
        assertTrue(list.result.contains("写周报"))
    }

    @Test
    fun calculateEvaluatesExpression() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(call("calculate", """{"expression":"2+3*4"}"""))
        assertFalse(result.isError)
        assertEquals("2+3*4 = 14.0", result.result)
    }

    @Test
    fun unitConvertKmToMiles() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(
            call("unit_convert", """{"value":1,"from_unit":"km","to_unit":"miles"}""")
        )
        assertFalse(result.isError)
        assertEquals("1.0 km = 0.62 miles", result.result)
    }

    @Test
    fun generatePasswordRespectsLength() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(
            call("generate_password", """{"length":12,"include_symbols":false}""")
        )
        assertFalse(result.isError)
        assertTrue(result.result.contains("12-character"))
    }

    @Test
    fun unknownToolIsError() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(call("nope", "{}"))
        assertTrue(result.isError)
    }

    @Test
    fun savePasswordPersistsToPasswordManager() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(
            call("save_password", """{"app_name":"GitHub","account":"octocat","password":"sup3rSecret!","note":"工作"}""")
        )
        assertFalse(result.isError)
        assertFalse(result.result.contains("sup3rSecret"))
        val entries = SqlDelightPasswordRepository(fixture.db).getAllPasswordEntries().first()
        assertEquals(1, entries.size)
        assertEquals("GitHub", entries.first().appName)
        assertEquals("sup3rSecret!", entries.first().password)
    }

    @Test
    fun savePasswordGeneratesWhenMissing() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(
            call("save_password", """{"app_name":"邮箱","account":"me@example.com"}""")
        )
        assertFalse(result.isError)
        assertTrue(result.result.contains("生成的密码"))
        val entries = SqlDelightPasswordRepository(fixture.db).getAllPasswordEntries().first()
        assertEquals(1, entries.size)
        assertEquals(16, entries.first().password.length)
    }

    @Test
    fun searchPasswordsMasksPassword() = runBlocking {
        val fixture = buildFixture()
        fixture.executor.executeTool(
            call("save_password", """{"app_name":"GitHub","account":"octocat","password":"sup3rSecret!"}""")
        )
        val search = fixture.executor.executeTool(call("search_passwords", """{"query":"GitHub"}"""))
        assertFalse(search.isError)
        assertTrue(search.result.contains("GitHub"))
        assertFalse(search.result.contains("sup3rSecret"))
    }

    @Test
    fun scheduleCreateQueryComplete() = runBlocking {
        val fixture = buildFixture()
        val create = fixture.executor.executeTool(
            call("create_schedule", """{"title":"周会","time":"10:00","description":"例会"}""")
        )
        assertFalse(create.isError)

        val query = fixture.executor.executeTool(call("get_schedules", "{}"))
        assertFalse(query.isError)
        assertTrue(query.result.contains("周会"))

        val done = fixture.executor.executeTool(call("complete_schedule", """{"title":"周会"}"""))
        assertFalse(done.isError)
        val schedules = SqlDelightScheduleRepository(fixture.db).getAllSchedules().first()
        assertEquals(1, schedules.size)
        assertTrue(schedules.first().isCompleted)
    }

    @Test
    fun completeScheduleUnknownIsError() = runBlocking {
        val fixture = buildFixture()
        val result = fixture.executor.executeTool(call("complete_schedule", """{"title":"不存在"}"""))
        assertTrue(result.isError)
    }
}