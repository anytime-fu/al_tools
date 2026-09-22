package com.toolbox.ui.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.local.entity.Category
import com.toolbox.data.local.entity.Habit
import com.toolbox.data.local.entity.HabitRecord
import com.toolbox.data.local.entity.Transaction
import com.toolbox.data.repository.HabitRepository
import com.toolbox.data.repository.TransactionRepository
import com.toolbox.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonalViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = habitRepository.getAllHabits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _habitCompletionState = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val habitCompletionState: StateFlow<Map<Long, Boolean>> = _habitCompletionState

    private val _habitStats = MutableStateFlow<Map<Long, HabitStats>>(emptyMap())
    val habitStats: StateFlow<Map<Long, HabitStats>> = _habitStats

    data class HabitStats(
        val streak: Int,
        val completionRate: Float,
        val totalDays: Int,
        val lastSevenDays: List<Boolean>
    )

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        initDefaultCategories()
        loadHabitCompletionAndStats()
    }

    private fun initDefaultCategories() {
        viewModelScope.launch {
            categories.first().let { existingCategories ->
                if (existingCategories.isEmpty()) {
                    val defaultCategories = listOf(
                        Category(name = "餐饮", type = "expense", icon = "🍽️", color = 0xFFFF5722),
                        Category(name = "交通", type = "expense", icon = "🚗", color = 0xFF2196F3),
                        Category(name = "购物", type = "expense", icon = "🛒", color = 0xFFE91E63),
                        Category(name = "娱乐", type = "expense", icon = "🎮", color = 0xFF9C27B0),
                        Category(name = "居住", type = "expense", icon = "🏠", color = 0xFF795548),
                        Category(name = "医疗", type = "expense", icon = "💊", color = 0xFFF44336),
                        Category(name = "教育", type = "expense", icon = "📚", color = 0xFF3F51B5),
                        Category(name = "其他支出", type = "expense", icon = "📦", color = 0xFF607D8B),
                        Category(name = "工资", type = "income", icon = "💰", color = 0xFF4CAF50),
                        Category(name = "奖金", type = "income", icon = "🎁", color = 0xFFFFC107),
                        Category(name = "投资", type = "income", icon = "📈", color = 0xFF00BCD4),
                        Category(name = "其他收入", type = "income", icon = "💎", color = 0xFF8BC34A)
                    )
                    defaultCategories.forEach { category ->
                        categoryRepository.insertCategory(category)
                    }
                }
            }
        }
    }

    private fun loadHabitCompletionAndStats() {
        viewModelScope.launch {
            habits.collect { habitList ->
                val state = mutableMapOf<Long, Boolean>()
                val stats = mutableMapOf<Long, HabitStats>()
                habitList.forEach { habit ->
                    state[habit.id] = habitRepository.isHabitCompletedForDate(
                        habit.id,
                        _selectedDate.value
                    )
                    val records = habitRepository.getRecordsForHabit(habit.id)
                    records.first().let { recordList ->
                        val streak = calculateStreak(recordList)
                        val completionRate = calculateCompletionRate(recordList)
                        val totalDays = recordList.size
                        val lastSevenDays = calculateLastSevenDays(recordList)
                        stats[habit.id] = HabitStats(
                            streak = streak,
                            completionRate = completionRate,
                            totalDays = totalDays,
                            lastSevenDays = lastSevenDays
                        )
                    }
                }
                _habitCompletionState.value = state
                _habitStats.value = stats
            }
        }
    }

    fun onDateChange(date: LocalDate) {
        _selectedDate.value = date
        loadHabitCompletionAndStats()
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            habitRepository.toggleHabitForDate(habitId, _selectedDate.value)
            loadHabitCompletionAndStats()
        }
    }

    fun addHabit(name: String, icon: String) {
        viewModelScope.launch {
            habitRepository.insertHabit(
                Habit(
                    name = name,
                    icon = icon
                )
            )
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    private fun calculateStreak(records: List<HabitRecord>): Int {
        if (records.isEmpty()) return 0

        val sortedDates = records.map { LocalDate.parse(it.date) }.sortedDescending()
        var streak = 1
        var currentDate = sortedDates.first()

        for (i in 1 until sortedDates.size) {
            val previousDate = sortedDates[i]
            if (currentDate.minusDays(1) == previousDate) {
                streak++
                currentDate = previousDate
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateCompletionRate(records: List<HabitRecord>): Float {
        if (records.isEmpty()) return 0f

        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val recentRecords = records.filter { record ->
            val recordDate = LocalDate.parse(record.date)
            recordDate.isAfter(thirtyDaysAgo) || recordDate.isEqual(thirtyDaysAgo)
        }

        return recentRecords.size.toFloat() / 30f
    }

    private fun calculateLastSevenDays(records: List<HabitRecord>): List<Boolean> {
        val today = LocalDate.now()
        val completedDates = records.map { LocalDate.parse(it.date) }.toSet()

        return (0..6).map { daysAgo ->
            completedDates.contains(today.minusDays(daysAgo.toLong()))
        }.reversed()
    }

    // 记账相关
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private fun getMonthStartTime(month: YearMonth): Long {
        return month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun getMonthEndTime(month: YearMonth): Long {
        return month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val monthIncome: StateFlow<Double> = _currentMonth.flatMapLatest { month ->
        transactionRepository.getTotalByTypeAndDateRange(
            "income",
            getMonthStartTime(month),
            getMonthEndTime(month)
        )
    }.map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val monthExpense: StateFlow<Double> = _currentMonth.flatMapLatest { month ->
        transactionRepository.getTotalByTypeAndDateRange(
            "expense",
            getMonthStartTime(month),
            getMonthEndTime(month)
        )
    }.map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val monthTransactions: StateFlow<List<Transaction>> = _currentMonth.flatMapLatest { month ->
        transactionRepository.getTransactionsByDateRange(
            getMonthStartTime(month),
            getMonthEndTime(month)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun addTransaction(amount: Double, type: String, categoryId: Long, note: String) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(
                Transaction(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    note = note
                )
            )
        }
    }

    fun addCategory(name: String, type: String, icon: String, color: Long) {
        viewModelScope.launch {
            categoryRepository.insertCategory(
                Category(
                    name = name,
                    type = type,
                    icon = icon,
                    color = color
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun exportTransactionsToCsv(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val transactions = monthTransactions.value
                val csvContent = StringBuilder()
                csvContent.appendLine("日期,类型,分类,金额,备注")
                
                transactions.forEach { transaction ->
                    val category = categories.value.find { it.id == transaction.categoryId }
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date(transaction.date))
                    val type = if (transaction.type == "income") "收入" else "支出"
                    val categoryName = category?.name ?: "未知"
                    val note = transaction.note.replace(",", "，")
                    csvContent.appendLine("$date,$type,$categoryName,${transaction.amount},$note")
                }
                
                val fileName = "记账记录_${currentMonth.value.format(DateTimeFormatter.ofPattern("yyyy年MM月"))}.csv"
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(csvContent.toString())
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_TITLE, fileName)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(android.content.Intent.createChooser(intent, "导出CSV"))
            } catch (e: Exception) {
                // 导出失败
            }
        }
    }
}
