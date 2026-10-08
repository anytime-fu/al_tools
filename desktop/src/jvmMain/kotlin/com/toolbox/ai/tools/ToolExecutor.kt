package com.toolbox.ai.tools

import com.toolbox.data.local.entity.Category
import com.toolbox.data.local.entity.Habit
import com.toolbox.data.local.entity.Note
import com.toolbox.data.local.entity.PasswordEntry
import com.toolbox.data.local.entity.Schedule
import com.toolbox.data.local.entity.Transaction
import com.toolbox.data.repository.HabitRepository
import com.toolbox.data.repository.NoteRepository
import com.toolbox.data.repository.PasswordRepository
import com.toolbox.data.repository.ScheduleRepository
import com.toolbox.data.repository.SettingsRepository
import com.toolbox.data.repository.TransactionRepository
import com.toolbox.ui.productivity.kanban.KanbanTaskStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ToolExecutor(
    private val noteRepository: NoteRepository,
    private val transactionRepository: TransactionRepository,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val passwordRepository: PasswordRepository,
    private val scheduleRepository: ScheduleRepository
) {
    private data class ExecutionResult(
        val result: String,
        val isError: Boolean = false
    )
    
    suspend fun executeTool(toolCall: ToolCall): ToolResult {
        val functionName = toolCall.function.name
        val args = parseArguments(toolCall.function.arguments)
        
        val executionResult = try {
            when (functionName) {
                "create_note" -> createNote(args)
                "search_notes" -> searchNotes(args)
                "get_recent_notes" -> getRecentNotes(args)
                "add_transaction" -> addTransaction(args)
                "query_transactions" -> queryTransactions(args)
                "get_expense_summary" -> getExpenseSummary(args)
                "create_habit" -> createHabit(args)
                "check_habit" -> checkHabit(args)
                "get_habits" -> getHabits(args)
                "add_todo" -> addTodo(args)
                "get_todos" -> getTodos(args)
                "save_password" -> savePassword(args)
                "search_passwords" -> searchPasswords(args)
                "create_schedule" -> createSchedule(args)
                "get_schedules" -> getSchedules(args)
                "complete_schedule" -> completeSchedule(args)
                "generate_password" -> generatePassword(args)
                "calculate" -> calculate(args)
                "unit_convert" -> unitConvert(args)
                else -> ExecutionResult(result = "Unknown tool: $functionName", isError = true)
            }
        } catch (e: Exception) {
            ExecutionResult(result = "Execution failed: ${e.message}", isError = true)
        }
        
        return ToolResult(
            toolCallId = toolCall.id,
            functionName = functionName,
            result = executionResult.result,
            isError = executionResult.isError
        )
    }
    
    private fun parseArguments(argumentsJson: String): Map<String, JsonElement> {
        return try {
            Json.parseToJsonElement(argumentsJson).jsonObject
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun argString(args: Map<String, JsonElement>, key: String, default: String = ""): String {
        val element = args[key] as? JsonPrimitive ?: return default
        return element.contentOrNull ?: default
    }
    
    private fun argDouble(args: Map<String, JsonElement>, key: String, default: Double = 0.0): Double {
        return argString(args, key).toDoubleOrNull() ?: default
    }
    
    private fun argInt(args: Map<String, JsonElement>, key: String, default: Int = 0): Int {
        return argString(args, key).toIntOrNull() ?: default
    }
    
    private fun argBoolean(args: Map<String, JsonElement>, key: String, default: Boolean = false): Boolean {
        val element = args[key] as? JsonPrimitive ?: return default
        return element.booleanOrNull ?: element.contentOrNull?.toBooleanStrictOrNull() ?: default
    }
    
    private suspend fun createNote(args: Map<String, JsonElement>): ExecutionResult {
        val title = argString(args, "title")
        val content = argString(args, "content")
        
        if (title.isBlank()) {
            return ExecutionResult(result = "Title cannot be empty", isError = true)
        }
        
        val noteId = noteRepository.insertNote(Note(title = title, content = content))
        return ExecutionResult(result = "Note created successfully, ID: $noteId, Title: $title")
    }
    
    private suspend fun searchNotes(args: Map<String, JsonElement>): ExecutionResult {
        val query = argString(args, "query")
        
        if (query.isBlank()) {
            return ExecutionResult(result = "Search keyword cannot be empty", isError = true)
        }
        
        val notes = noteRepository.searchNotes(query).first()
        
        return if (notes.isEmpty()) {
            ExecutionResult(result = "No notes found containing \"$query\"")
        } else {
            val summary = notes.take(5).joinToString("\n") { note ->
                "- ${note.title} (${formatDate(note.updatedAt)})"
            }
            ExecutionResult(result = "Found ${notes.size} notes:\n$summary")
        }
    }
    
    private suspend fun getRecentNotes(args: Map<String, JsonElement>): ExecutionResult {
        val limit = argInt(args, "limit", 5)
        
        val notes = noteRepository.getAllNotes().first()
            .sortedByDescending { it.updatedAt }
            .take(limit)
        
        return if (notes.isEmpty()) {
            ExecutionResult(result = "No notes yet")
        } else {
            val summary = notes.joinToString("\n") { note ->
                "- ${note.title} (${formatDate(note.updatedAt)})"
            }
            ExecutionResult(result = "Recent ${notes.size} notes:\n$summary")
        }
    }
    
    private suspend fun addTransaction(args: Map<String, JsonElement>): ExecutionResult {
        val amount = argDouble(args, "amount")
        val type = argString(args, "type")
        val category = argString(args, "category")
        val note = argString(args, "note", "")
        
        if (amount <= 0) {
            return ExecutionResult(result = "Amount must be greater than 0", isError = true)
        }
        
        if (type != "income" && type != "expense") {
            return ExecutionResult(result = "Type must be income or expense", isError = true)
        }
        
        val categories = transactionRepository.getCategoriesByType(type).first()
        val existingCategory = categories.find { it.name == category }
        val categoryId = existingCategory?.id ?: transactionRepository.insertCategory(
            Category(
                name = category,
                type = type,
                icon = if (type == "income") "\uD83D\uDCB0" else "\uD83D\uDCB8"
            )
        )
        
        transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = type,
                categoryId = categoryId,
                note = note
            )
        )
        
        val typeStr = if (type == "income") "Income" else "Expense"
        return ExecutionResult(result = "Transaction recorded: $typeStr ¥${"%.2f".format(amount)} ($category)")
    }
    
    private suspend fun queryTransactions(args: Map<String, JsonElement>): ExecutionResult {
        val type = argString(args, "type", "all")
        val period = argString(args, "period", "month")
        val (startDate, endDate) = dateRange(period)
        
        val transactions = when (type) {
            "income" -> transactionRepository.getTransactionsByType("income").first()
            "expense" -> transactionRepository.getTransactionsByType("expense").first()
            else -> transactionRepository.getAllTransactions().first()
        }.filter { it.date in startDate..endDate }
        
        return if (transactions.isEmpty()) {
            ExecutionResult(result = "No ${periodName(period)} transaction records")
        } else {
            val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
            val recentList = transactions.sortedByDescending { it.date }.take(5)
                .joinToString("\n") { t ->
                    val typeStr = if (t.type == "income") "Income" else "Expense"
                    "- $typeStr ¥${"%.2f".format(t.amount)} ${t.note}"
                }
            
            ExecutionResult(
                result = "${periodName(period)} Transaction Summary:\n" +
                    "Total Income: ¥${"%.2f".format(totalIncome)}\n" +
                    "Total Expense: ¥${"%.2f".format(totalExpense)}\n" +
                    "Balance: ¥${"%.2f".format(totalIncome - totalExpense)}\n\n" +
                    "Recent Records:\n$recentList"
            )
        }
    }
    
    private suspend fun getExpenseSummary(args: Map<String, JsonElement>): ExecutionResult {
        val period = argString(args, "period", "month")
        val (startDate, endDate) = dateRange(period)
        
        val expenseTransactions = transactionRepository.getTransactionsByType("expense").first()
            .filter { it.date in startDate..endDate }
        
        return if (expenseTransactions.isEmpty()) {
            ExecutionResult(result = "No ${periodName(period)} expense records")
        } else {
            val categories = transactionRepository.getAllCategories().first()
            val totals = expenseTransactions.groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            val total = totals.values.sum()
            val summary = totals.entries.sortedByDescending { it.value }.joinToString("\n") { (categoryId, amount) ->
                val categoryName = categories.find { it.id == categoryId }?.name ?: "Uncategorized"
                "- $categoryName: ¥${"%.2f".format(amount)} (${"%.1f".format(amount / total * 100)}%)"
            }
            
            ExecutionResult(
                result = "${periodName(period)} Expense Summary:\n" +
                    "Total: ¥${"%.2f".format(total)}\n\n" +
                    "Category Breakdown:\n$summary"
            )
        }
    }
    
    private suspend fun createHabit(args: Map<String, JsonElement>): ExecutionResult {
        val name = argString(args, "name")
        val icon = argString(args, "icon", "⭐")
        
        if (name.isBlank()) {
            return ExecutionResult(result = "Habit name cannot be empty", isError = true)
        }
        
        habitRepository.insertHabit(Habit(name = name, icon = icon))
        return ExecutionResult(result = "Habit created: $icon $name")
    }
    
    private suspend fun checkHabit(args: Map<String, JsonElement>): ExecutionResult {
        val habitName = argString(args, "habit_name")
        
        if (habitName.isBlank()) {
            return ExecutionResult(result = "Habit name cannot be empty", isError = true)
        }
        
        val habits = habitRepository.getAllHabits().first()
        val habit = habits.find { it.name.contains(habitName, ignoreCase = true) }
        
        if (habit == null) {
            return ExecutionResult(result = "Habit not found: $habitName", isError = true)
        }
        
        val completed = habitRepository.toggleHabitForDate(habit.id, startOfToday())
        val status = if (completed) "Checked in" else "Unchecked"
        return ExecutionResult(result = "${habit.icon} ${habit.name} $status")
    }
    
    private suspend fun getHabits(args: Map<String, JsonElement>): ExecutionResult {
        val habits = habitRepository.getAllHabits().first()
        
        return if (habits.isEmpty()) {
            ExecutionResult(result = "No habits yet. Create one first.")
        } else {
            val today = startOfToday()
            val habitList = habits.map { habit ->
                val isCompleted = habitRepository.isHabitCompletedForDate(habit.id, today)
                val status = if (isCompleted) "✅" else "⬜"
                "$status ${habit.icon} ${habit.name}"
            }
            ExecutionResult(result = "Today's Habits:\n${habitList.joinToString("\n")}")
        }
    }
    
    private suspend fun addTodo(args: Map<String, JsonElement>): ExecutionResult {
        val title = argString(args, "title")
        
        if (title.isBlank()) {
            return ExecutionResult(result = "Todo title cannot be empty", isError = true)
        }
        
        KanbanTaskStore.add(settingsRepository, title, note = "")
        return ExecutionResult(result = "Todo added: $title")
    }
    
    private suspend fun getTodos(args: Map<String, JsonElement>): ExecutionResult {
        val tasks = KanbanTaskStore.load(settingsRepository)
        
        return if (tasks.isEmpty()) {
            ExecutionResult(result = "No todos yet")
        } else {
            val list = tasks.joinToString("\n") { task ->
                val status = if (task.column == 2) "✅" else "⬜"
                "$status ${task.title}"
            }
            ExecutionResult(result = "Todo list:\n$list")
        }
    }
    
    private suspend fun savePassword(args: Map<String, JsonElement>): ExecutionResult {
        val appName = argString(args, "app_name")
        val account = argString(args, "account")
        
        if (appName.isBlank()) {
            return ExecutionResult(result = "App name cannot be empty", isError = true)
        }
        if (account.isBlank()) {
            return ExecutionResult(result = "Account cannot be empty", isError = true)
        }
        
        val provided = argString(args, "password")
        val generated = provided.isBlank()
        val password = if (generated) randomPassword() else provided
        val note = argString(args, "note", "")
        
        passwordRepository.insertPasswordEntry(
            PasswordEntry(
                appName = appName,
                account = account,
                password = password,
                note = note
            )
        )
        
        val base = "已保存到密码管理: $appName / $account（密码 ${password.length} 位）"
        return if (generated) {
            ExecutionResult(result = "$base\n生成的密码: $password")
        } else {
            ExecutionResult(result = base)
        }
    }
    
    private suspend fun searchPasswords(args: Map<String, JsonElement>): ExecutionResult {
        val query = argString(args, "query")
        
        if (query.isBlank()) {
            return ExecutionResult(result = "Search keyword cannot be empty", isError = true)
        }
        
        val entries = passwordRepository.searchPasswordEntries(query).first()
        
        return if (entries.isEmpty()) {
            ExecutionResult(result = "未找到包含 \"$query\" 的密码条目")
        } else {
            val summary = entries.take(5).joinToString("\n") { entry ->
                "- ${entry.appName} / ${entry.account}（密码已设置，长度 ${entry.password.length}）"
            }
            ExecutionResult(result = "找到 ${entries.size} 条密码记录:\n$summary\n明文密码请在「密码」页查看")
        }
    }
    
    private suspend fun createSchedule(args: Map<String, JsonElement>): ExecutionResult {
        val title = argString(args, "title")
        
        if (title.isBlank()) {
            return ExecutionResult(result = "Schedule title cannot be empty", isError = true)
        }
        
        val date = resolveDate(argString(args, "date"))
        val time = argString(args, "time", "")
        val description = argString(args, "description", "")
        
        scheduleRepository.insertSchedule(
            Schedule(
                title = title,
                description = description,
                date = startOfDay(date),
                time = time
            )
        )
        
        val whenText = date.toString() + if (time.isNotBlank()) " $time" else ""
        return ExecutionResult(result = "日程已创建: $title（$whenText）")
    }
    
    private suspend fun getSchedules(args: Map<String, JsonElement>): ExecutionResult {
        val date = resolveDate(argString(args, "date"))
        
        val daySchedules = scheduleRepository.getAllSchedules().first()
            .filter { isSameDay(it.date, date) }
            .sortedBy { it.time }
        
        return if (daySchedules.isEmpty()) {
            ExecutionResult(result = "$date 暂无日程")
        } else {
            val summary = daySchedules.joinToString("\n") { s ->
                val status = if (s.isCompleted) "✅" else "⬜"
                val timeText = if (s.time.isNotBlank()) "${s.time} " else ""
                "$status $timeText${s.title}"
            }
            ExecutionResult(result = "$date 日程:\n$summary")
        }
    }
    
    private suspend fun completeSchedule(args: Map<String, JsonElement>): ExecutionResult {
        val title = argString(args, "title")
        
        if (title.isBlank()) {
            return ExecutionResult(result = "Schedule title cannot be empty", isError = true)
        }
        
        val matched = scheduleRepository.getAllSchedules().first()
            .filter { it.title.contains(title, ignoreCase = true) }
        val target = matched.firstOrNull { !it.isCompleted } ?: matched.firstOrNull()
        
        if (target == null) {
            return ExecutionResult(result = "未找到日程: $title", isError = true)
        }
        
        scheduleRepository.updateSchedule(
            target.copy(isCompleted = true, updatedAt = System.currentTimeMillis())
        )
        return ExecutionResult(result = "已完成: ${target.title}")
    }
    
    private fun resolveDate(dateText: String): LocalDate {
        return if (dateText.isBlank()) LocalDate.now() else LocalDate.parse(dateText)
    }
    
    private fun startOfDay(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    private fun isSameDay(millis: Long, date: LocalDate): Boolean {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() == date
    }
    
    private fun randomPassword(length: Int = 16): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + "!@#\$%^&*()_+-=[]{}|;:,.<>?".toList()
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    private suspend fun generatePassword(args: Map<String, JsonElement>): ExecutionResult {
        val length = argInt(args, "length", 16).coerceIn(4, 128)
        val includeSymbols = argBoolean(args, "include_symbols", true)
        
        val chars = mutableListOf<Char>().apply {
            addAll('a'..'z')
            addAll('A'..'Z')
            addAll('0'..'9')
            if (includeSymbols) {
                addAll("!@#\$%^&*()_+-=[]{}|;:,.<>?".toList())
            }
        }
        
        val password = (1..length)
            .map { chars.random() }
            .joinToString("")
        
        return ExecutionResult(
            result = "Generated ${length}-character password:\n$password\n\nKeep it safe and don't share it."
        )
    }
    
    private suspend fun calculate(args: Map<String, JsonElement>): ExecutionResult {
        val expression = argString(args, "expression")
        
        if (expression.isBlank()) {
            return ExecutionResult(result = "Expression cannot be empty", isError = true)
        }
        
        return try {
            val result = evaluateExpression(expression)
            ExecutionResult(result = "$expression = $result")
        } catch (e: Exception) {
            ExecutionResult(result = "Calculation failed: ${e.message}", isError = true)
        }
    }
    
    private suspend fun unitConvert(args: Map<String, JsonElement>): ExecutionResult {
        val value = argDouble(args, "value")
        val fromUnit = argString(args, "from_unit")
        val toUnit = argString(args, "to_unit")
        
        val result = when {
            fromUnit == "km" && toUnit == "miles" -> value * 0.621371
            fromUnit == "miles" && toUnit == "km" -> value * 1.60934
            fromUnit == "kg" && toUnit == "lbs" -> value * 2.20462
            fromUnit == "lbs" && toUnit == "kg" -> value * 0.453592
            fromUnit == "celsius" && toUnit == "fahrenheit" -> value * 9 / 5 + 32
            fromUnit == "fahrenheit" && toUnit == "celsius" -> (value - 32) * 5 / 9
            else -> return ExecutionResult(
                result = "Unsupported unit conversion: $fromUnit -> $toUnit",
                isError = true
            )
        }
        
        return ExecutionResult(result = "$value $fromUnit = ${"%.2f".format(result)} $toUnit")
    }
    
    private fun dateRange(period: String): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startDate = when (period) {
            "today" -> today
            "week" -> today.with(DayOfWeek.MONDAY)
            "year" -> today.withDayOfYear(1)
            else -> today.withDayOfMonth(1)
        }
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
    
    private fun periodName(period: String): String {
        return when (period) {
            "today" -> "Today"
            "week" -> "This Week"
            "month" -> "This Month"
            "year" -> "This Year"
            else -> ""
        }
    }
    
    private fun startOfToday(): Long {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
    
    private fun evaluateExpression(expression: String): Double {
        return try {
            val expr = expression.replace(" ", "")
            
            when {
                expr.contains("+") -> {
                    val parts = expr.split("+", limit = 2)
                    evaluateExpression(parts[0]) + evaluateExpression(parts[1])
                }
                expr.contains("-") -> {
                    val parts = expr.split("-", limit = 2)
                    evaluateExpression(parts[0]) - evaluateExpression(parts[1])
                }
                expr.contains("*") -> {
                    val parts = expr.split("*", limit = 2)
                    evaluateExpression(parts[0]) * evaluateExpression(parts[1])
                }
                expr.contains("/") -> {
                    val parts = expr.split("/", limit = 2)
                    val divisor = evaluateExpression(parts[1])
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    evaluateExpression(parts[0]) / divisor
                }
                expr.startsWith("sqrt(") && expr.endsWith(")") -> {
                    val inner = expr.substring(5, expr.length - 1)
                    Math.sqrt(evaluateExpression(inner))
                }
                expr.startsWith("sin(") && expr.endsWith(")") -> {
                    val inner = expr.substring(4, expr.length - 1)
                    Math.sin(Math.toRadians(evaluateExpression(inner)))
                }
                expr.startsWith("cos(") && expr.endsWith(")") -> {
                    val inner = expr.substring(4, expr.length - 1)
                    Math.cos(Math.toRadians(evaluateExpression(inner)))
                }
                expr.startsWith("tan(") && expr.endsWith(")") -> {
                    val inner = expr.substring(4, expr.length - 1)
                    Math.tan(Math.toRadians(evaluateExpression(inner)))
                }
                else -> expr.toDouble()
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot evaluate expression: $expression")
        }
    }
}