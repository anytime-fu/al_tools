package com.toolbox.ui.productivity.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.data.repository.SettingsRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject

private const val SETTINGS_KEY = "pomodoro_stats"

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class PomodoroDay(
    val date: String,
    val count: Int,
    val minutes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val scope = rememberCoroutineScope()
    var workMin by remember { mutableStateOf(25) }
    var breakMin by remember { mutableStateOf(5) }
    var isWork by remember { mutableStateOf(true) }
    var totalSeconds by remember { mutableStateOf(25 * 60) }
    var remaining by remember { mutableStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<List<PomodoroDay>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

    fun saveStats() {
        val snapshot = stats
        scope.launch {
            settingsRepository.saveSetting(SETTINGS_KEY, json.encodeToString(snapshot))
        }
    }

    fun switchMode(work: Boolean) {
        isWork = work
        totalSeconds = (if (work) workMin else breakMin) * 60
        remaining = totalSeconds
    }

    fun completePhase() {
        running = false
        if (isWork) {
            val updated = stats.toMutableList()
            val index = updated.indexOfFirst { it.date == today }
            if (index >= 0) {
                val day = updated[index]
                updated[index] = day.copy(count = day.count + 1, minutes = day.minutes + workMin)
            } else {
                updated.add(PomodoroDay(today, 1, workMin))
            }
            stats = updated
            saveStats()
            message = "完成一个番茄！休息 $breakMin 分钟"
            switchMode(work = false)
        } else {
            message = "休息结束，开始下一轮专注"
            switchMode(work = true)
        }
    }

    LaunchedEffect(Unit) {
        val saved = settingsRepository.getSettingValue(SETTINGS_KEY, "")
        if (saved.isNotBlank()) {
            stats = runCatching { json.decodeFromString<List<PomodoroDay>>(saved) }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1000)
            remaining--
        }
        if (running && remaining <= 0) {
            completePhase()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("番茄钟") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isWork) "专注中" else "休息中",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "%02d:%02d".format(remaining / 60, remaining % 60),
                    fontSize = 64.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = if (totalSeconds == 0) 0f else 1f - remaining.toFloat() / totalSeconds,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { running = !running }) {
                        Icon(
                            if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (running) "暂停" else "开始")
                    }
                    OutlinedButton(onClick = {
                        running = false
                        switchMode(isWork)
                        message = ""
                    }) {
                        Text("重置")
                    }
                    OutlinedButton(onClick = {
                        running = false
                        completePhase()
                    }) {
                        Text("跳过")
                    }
                }

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to 3, 25 to 5, 45 to 10).forEach { (work, rest) ->
                        FilterChip(
                            selected = workMin == work && breakMin == rest,
                            onClick = {
                                workMin = work
                                breakMin = rest
                                running = false
                                switchMode(isWork)
                            },
                            label = { Text("$work/$rest 分钟") }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("统计报表", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    val todayStat = stats.firstOrNull { it.date == today }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(
                                text = "${todayStat?.count ?: 0}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("今日番茄数", style = MaterialTheme.typography.labelSmall)
                        }
                        Column {
                            Text(
                                text = "${todayStat?.minutes ?: 0}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("今日专注分钟", style = MaterialTheme.typography.labelSmall)
                        }
                        Column {
                            val totalCount = stats.sumOf { it.count }
                            Text(
                                text = "$totalCount",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("累计番茄数", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("最近 7 天", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val days = (6 downTo 0).map {
                        LocalDate.now().minusDays(it.toLong()).format(DateTimeFormatter.ISO_DATE)
                    }
                    val maxCount = days.maxOf { d -> stats.firstOrNull { it.date == d }?.count ?: 0 }.coerceAtLeast(1)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEach { date ->
                            val count = stats.firstOrNull { it.date == date }?.count ?: 0
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$count", style = MaterialTheme.typography.labelSmall)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((count * 100 / maxCount).coerceAtLeast(2).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                                )
                                Text(
                                    text = date.substring(5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
