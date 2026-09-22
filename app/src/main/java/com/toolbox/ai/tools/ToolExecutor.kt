package com.toolbox.ai.tools

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.toolbox.data.local.entity.Habit
import com.toolbox.data.local.entity.Note
import com.toolbox.data.local.entity.Transaction
import com.toolbox.data.repository.HabitRepository
import com.toolbox.data.repository.NoteRepository
import com.toolbox.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutor @Inject constructor(
    private val noteRepository: NoteRepository,
    private val transactionRepository: TransactionRepository,
    private val habitRepository: HabitRepository,
    private val gson: Gson
) {
    // 内部结果类
    private data class ExecutionResult(
        val result: String,
        val isError: Boolean = false
    )
    
    // 执行工具调用
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
    
    private fun parseArguments(argumentsJson: String): Map<String, Any> {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(argumentsJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun getArgString(args: Map<String, Any>, key: String, default: String = ""): String {
        return args[key]?.toString() ?: default
    }
    
    private fun getArgDouble(args: Map<String, Any>, key: String, default: Double = 0.0): Double {
        return args[key]?.toString()?.toDoubleOrNull() ?: default
    }
    
    private fun getArgInt(args: Map<String, Any>, key: String, default: Int = 0): Int {
        return args[key]?.toString()?.toIntOrNull() ?: default
    }
    
    private fun getArgBoolean(args: Map<String, Any>, key: String, default: Boolean = false): Boolean {
        return args[key]?.toString()?.toBooleanStrictOrNull() ?: default
    }
    
    // Note tools
    private suspend fun createNote(args: Map<String, Any>): ExecutionResult {
        val title = getArgString(args, "title")
        val content = getArgString(args, "content")
        
        if (title.isBlank()) {
            return ExecutionResult(result = "Title cannot be empty", isError = true)
        }
        
        val note = Note(title = title, content = content)
        val noteId = noteRepository.insertNote(note)
        
        return ExecutionResult(result = "Note created successfully, ID: $noteId, Title: $title")
    }
    
    private suspend fun searchNotes(args: Map<String, Any>): ExecutionResult {
        val query = getArgString(args, "query")
        
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
    
    private suspend fun getRecentNotes(args: Map<String, Any>): ExecutionResult {
        val limit = getArgInt(args, "limit", 5)
        
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
    
    // Transaction tools
    private suspend fun addTransaction(args: Map<String, Any>): ExecutionResult {
        val amount = getArgDouble(args, "amount")
        val type = getArgString(args, "type")
        val category = getArgString(args, "category")
        val note = getArgString(args, "note", "")
        
        if (amount <= 0) {
            return ExecutionResult(result = "Amount must be greater than 0", isError = true)
        }
        
        if (type != "income" && type != "expense") {
            return ExecutionResult(result = "Type must be income or expense", isError = true)
        }
        
        val categories = transactionRepository.getCategoriesByType(type).first()
        val existingCategory = categories.find { it.name == category }
        val categoryId = existingCategory?.id ?: run {
            transactionRepository.insertCategory(
                com.toolbox.data.local.entity.Category(
                    name = category,
                    type = type,
                    icon = if (type == "income") "💰" else "💸"
                )
            )
        }
        
        val transaction = Transaction(
            amount = amount,
            type = type,
            categoryId = categoryId,
            note = note
        )
        transactionRepository.insertTransaction(transaction)
        
        val typeStr = if (type == "income") "Income" else "Expense"
        return ExecutionResult(result = "Transaction recorded: $typeStr ¥${String.format("%.2f", amount)} ($category)")
    }
    
    private suspend fun queryTransactions(args: Map<String, Any>): ExecutionResult {
        val type = getArgString(args, "type", "all")
        val period = getArgString(args, "period", "month")
        
        val (startDate, endDate) = getDateRange(period)
        
        val transactions = when (type) {
            "income" -> transactionRepository.getTransactionsByType("income").first()
            "expense" -> transactionRepository.getTransactionsByType("expense").first()
            else -> transactionRepository.getAllTransactions().first()
        }.filter { it.date in startDate..endDate }
        
        return if (transactions.isEmpty()) {
            ExecutionResult(result = "No ${getPeriodName(period)} transaction records")
        } else {
            val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
            val recentList = transactions.sortedByDescending { it.date }.take(5)
                .joinToString("\n") { t ->
                    val typeStr = if (t.type == "income") "Income" else "Expense"
                    "- $typeStr ¥${String.format("%.2f", t.amount)} ${t.note}"
                }
            
            ExecutionResult(
                result = """${getPeriodName(period)} Transaction Summary:
Total Income: ¥${String.format("%.2f", totalIncome)}
Total Expense: ¥${String.format("%.2f", totalExpense)}
Balance: ¥${String.format("%.2f", totalIncome - totalExpense)}

Recent Records:
$recentList"""
            )
        }
    }
    
    private suspend fun getExpenseSummary(args: Map<String, Any>): ExecutionResult {
        val period = getArgString(args, "period", "month")
        val (startDate, endDate) = getDateRange(period)
        
        val categoryTotals = transactionRepository.getCategoryTotals("expense", startDate, endDate).first()
        
        return if (categoryTotals.isEmpty()) {
            ExecutionResult(result = "No ${getPeriodName(period)} expense records")
        } else {
            val total = categoryTotals.sumOf { it.total }
            val categories = transactionRepository.getAllCategories().first()
            val summary = categoryTotals.joinToString("\n") { ct ->
                val categoryName = categories.find { it.id == ct.categoryId }?.name ?: "Uncategorized"
                "- $categoryName: ¥${String.format("%.2f", ct.total)} (${String.format("%.1f", ct.total / total * 100)}%)"
            }
            
            ExecutionResult(
                result = """${getPeriodName(period)} Expense Summary:
Total: ¥${String.format("%.2f", total)}

Category Breakdown:
$summary"""
            )
        }
    }
    
    // Habit tools
    private suspend fun createHabit(args: Map<String, Any>): ExecutionResult {
        val name = getArgString(args, "name")
        val icon = getArgString(args, "icon", "⭐")
        
        if (name.isBlank()) {
            return ExecutionResult(result = "Habit name cannot be empty", isError = true)
        }
        
        val habit = Habit(name = name, icon = icon)
        habitRepository.insertHabit(habit)
        
        return ExecutionResult(result = "Habit created: $icon $name")
    }
    
    private suspend fun checkHabit(args: Map<String, Any>): ExecutionResult {
        val habitName = getArgString(args, "habit_name")
        
        if (habitName.isBlank()) {
            return ExecutionResult(result = "Habit name cannot be empty", isError = true)
        }
        
        val habits = habitRepository.getAllHabits().first()
        val habit = habits.find { it.name.contains(habitName, ignoreCase = true) }
        
        if (habit == null) {
            return ExecutionResult(result = "Habit not found: $habitName", isError = true)
        }
        
        val completed = habitRepository.toggleHabitForDate(habit.id, LocalDate.now())
        val status = if (completed) "Checked in" else "Unchecked"
        
        return ExecutionResult(result = "${habit.icon} ${habit.name} $status")
    }
    
    private suspend fun getHabits(args: Map<String, Any>): ExecutionResult {
        val habits = habitRepository.getAllHabits().first()
        
        return if (habits.isEmpty()) {
            ExecutionResult(result = "No habits yet. Create one first.")
        } else {
            val today = LocalDate.now()
            val habitList = mutableListOf<String>()
            
            for (habit in habits) {
                val isCompleted = habitRepository.isHabitCompletedForDate(habit.id, today)
                val status = if (isCompleted) "✅" else "⬜"
                habitList.add("$status ${habit.icon} ${habit.name}")
            }
            
            ExecutionResult(result = "Today's Habits:\n${habitList.joinToString("\n")}")
        }
    }
    
    // Todo tools
    private suspend fun addTodo(args: Map<String, Any>): ExecutionResult {
        val title = getArgString(args, "title")
        
        if (title.isBlank()) {
            return ExecutionResult(result = "Todo title cannot be empty", isError = true)
        }
        
        return ExecutionResult(result = "Todo added: $title")
    }
    
    private suspend fun getTodos(args: Map<String, Any>): ExecutionResult {
        return ExecutionResult(result = "Todo list feature coming soon")
    }
    
    // Password tool
    private suspend fun generatePassword(args: Map<String, Any>): ExecutionResult {
        val length = getArgInt(args, "length", 16)
        val includeSymbols = getArgBoolean(args, "include_symbols", true)
        
        val chars = mutableListOf<Char>().apply {
            addAll('a'..'z')
            addAll('A'..'Z')
            addAll('0'..'9')
            if (includeSymbols) {
                addAll("!@#$%^&*()_+-=[]{}|;:,.<>?".toList())
            }
        }
        
        val password = (1..length)
            .map { chars.random() }
            .joinToString("")
        
        return ExecutionResult(result = "Generated ${length}-character password:\n$password\n\nKeep it safe and don't share it.")
    }
    
    // Calculator tools
    private suspend fun calculate(args: Map<String, Any>): ExecutionResult {
        val expression = getArgString(args, "expression")
        
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
    
    private suspend fun unitConvert(args: Map<String, Any>): ExecutionResult {
        val value = getArgDouble(args, "value")
        val fromUnit = getArgString(args, "from_unit")
        val toUnit = getArgString(args, "to_unit")
        
        val result = when {
            fromUnit == "km" && toUnit == "miles" -> value * 0.621371
            fromUnit == "miles" && toUnit == "km" -> value * 1.60934
            fromUnit == "kg" && toUnit == "lbs" -> value * 2.20462
            fromUnit == "lbs" && toUnit == "kg" -> value * 0.453592
            fromUnit == "celsius" && toUnit == "fahrenheit" -> value * 9/5 + 32
            fromUnit == "fahrenheit" && toUnit == "celsius" -> (value - 32) * 5/9
            else -> return ExecutionResult(
                result = "Unsupported unit conversion: $fromUnit -> $toUnit",
                isError = true
            )
        }
        
        return ExecutionResult(result = "$value $fromUnit = ${String.format("%.2f", result)} $toUnit")
    }
    
    // Helper methods
    private fun getDateRange(period: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        when (period) {
            "today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            "week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            "month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            "year" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }
        
        return calendar.timeInMillis to endDate
    }
    
    private fun getPeriodName(period: String): String {
        return when (period) {
            "today" -> "Today"
            "week" -> "This Week"
            "month" -> "This Month"
            "year" -> "This Year"
            else -> ""
        }
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
