package com.toolbox.ui.life.water

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class WaterRecord(
    val time: Long = System.currentTimeMillis(),
    val amount: Int = 250
)

data class DailyWaterData(
    val date: String,
    val records: List<WaterRecord>,
    val totalAmount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterReminderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val today = remember { dateFormat.format(Date()) }

    var records by remember { mutableStateOf(loadRecords(prefs, gson, today)) }
    var dailyGoal by remember { mutableIntStateOf(prefs.getInt("daily_goal", 2000)) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInput by remember { mutableStateOf(dailyGoal.toString()) }
    var showHistory by remember { mutableStateOf(false) }
    var historyData by remember { mutableStateOf(loadHistory(prefs, gson)) }

    val totalAmount = records.sumOf { it.amount }
    val progress = (totalAmount.toFloat() / dailyGoal).coerceIn(0f, 1f)

    fun addWater(amount: Int) {
        records = records + WaterRecord(amount = amount)
        saveRecords(prefs, gson, today, records)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("喝水提醒") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        historyData = loadHistory(prefs, gson)
                        showHistory = !showHistory
                    }) {
                        Icon(Icons.Default.History, contentDescription = "历史")
                    }
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = { addWater(500) }) {
                    Text("500", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                SmallFloatingActionButton(onClick = { addWater(300) }) {
                    Text("300", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(onClick = { addWater(200) }) {
                    Icon(Icons.Default.WaterDrop, contentDescription = "喝水")
                }
            }
        }
    ) { paddingValues ->
        if (showHistory) {
            HistoryContent(
                historyData = historyData,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("今日饮水", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${totalAmount}ml / ${dailyGoal}ml",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (records.isNotEmpty()) {
                    item {
                        Text(
                            text = "今日记录",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(records.reversed()) { record ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${record.amount}ml",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = timeFormat.format(Date(record.time)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("设置每日目标") },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("目标 (ml)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dailyGoal = goalInput.toIntOrNull() ?: 2000
                    prefs.edit().putInt("daily_goal", dailyGoal).apply()
                    showGoalDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun HistoryContent(
    historyData: List<DailyWaterData>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (historyData.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(historyData) { data ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.date,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${data.records.size}次饮水",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${data.totalAmount}ml",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun loadRecords(prefs: SharedPreferences, gson: Gson, date: String): List<WaterRecord> {
    val json = prefs.getString("records_$date", null) ?: return emptyList()
    val type = object : TypeToken<List<WaterRecord>>() {}.type
    return try {
        gson.fromJson(json, type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveRecords(prefs: SharedPreferences, gson: Gson, date: String, records: List<WaterRecord>) {
    prefs.edit()
        .putString("records_$date", gson.toJson(records))
        .apply()
}

private fun loadHistory(prefs: SharedPreferences, gson: Gson): List<DailyWaterData> {
    val history = mutableListOf<DailyWaterData>()
    val allPrefs = prefs.all

    for ((key, value) in allPrefs) {
        if (key.startsWith("records_") && value is String) {
            val date = key.removePrefix("records_")
            val type = object : TypeToken<List<WaterRecord>>() {}.type
            try {
                val records: List<WaterRecord> = gson.fromJson(value, type) ?: emptyList()
                if (records.isNotEmpty()) {
                    history.add(
                        DailyWaterData(
                            date = date,
                            records = records,
                            totalAmount = records.sumOf { it.amount }
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    return history.sortedByDescending { it.date }
}
