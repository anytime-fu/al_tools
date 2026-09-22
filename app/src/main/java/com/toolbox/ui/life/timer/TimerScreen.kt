package com.toolbox.ui.life.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class TimerMode(val label: String, val minutes: Int) {
    POMODORO("番茄钟", 25),
    SHORT_BREAK("短休息", 5),
    LONG_BREAK("长休息", 15),
    CUSTOM("自定义", 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(TimerMode.POMODORO) }
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("25") }
    var sessionCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning, remainingSeconds) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        if (remainingSeconds == 0 && isRunning) {
            isRunning = false
            sessionCount++
        }
    }

    fun resetTimer() {
        isRunning = false
        remainingSeconds = totalSeconds
    }

    fun setMode(newMode: TimerMode) {
        mode = newMode
        totalSeconds = if (newMode == TimerMode.CUSTOM) {
            (customMinutes.toIntOrNull() ?: 25) * 60
        } else {
            newMode.minutes * 60
        }
        resetTimer()
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
                actions = {
                    Text("完成: $sessionCount", modifier = Modifier.padding(end = 16.dp))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mode selection
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimerMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick = { setMode(m) },
                        label = { Text(m.label, fontSize = 12.sp) }
                    )
                }
            }

            if (mode == TimerMode.CUSTOM) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = {
                        customMinutes = it
                        totalSeconds = (it.toIntOrNull() ?: 25) * 60
                        if (!isRunning) remainingSeconds = totalSeconds
                    },
                    modifier = Modifier.width(120.dp),
                    label = { Text("分钟") },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Timer display
            val hours = remainingSeconds / 3600
            val minutes = (remainingSeconds % 3600) / 60
            val seconds = remainingSeconds % 60

            Text(
                text = if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
                else String.format("%02d:%02d", minutes, seconds),
                fontSize = 64.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = { resetTimer() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置")
                }

                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.width(120.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRunning) "暂停" else "开始")
                }
            }
        }
    }
}
