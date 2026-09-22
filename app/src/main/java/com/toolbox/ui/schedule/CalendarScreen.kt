package com.toolbox.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val newTitle by viewModel.newTitle.collectAsState()
    val newDescription by viewModel.newDescription.collectAsState()
    val newTime by viewModel.newTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程安排") },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "添加日程")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 日历视图
            CalendarView(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 选中日期
            Text(
                text = "选中日期: ${formatDate(selectedDate)}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 日程列表
            if (schedules.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无日程，点击+添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(schedules) { schedule ->
                        ScheduleItem(
                            schedule = schedule,
                            onToggleComplete = { viewModel.toggleComplete(schedule) },
                            onDelete = { viewModel.deleteSchedule(schedule) }
                        )
                    }
                }
            }
        }
    }

    // 添加日程对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddDialog() },
            title = { Text("添加日程") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { viewModel.onTitleChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        label = { Text("标题") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 时间选择下拉框
                    var timeExpanded by remember { mutableStateOf(false) }
                    val timeOptions = listOf("08:00", "09:00", "10:00", "11:00", "12:00", 
                        "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", 
                        "19:00", "20:00", "21:00", "22:00")
                    
                    ExposedDropdownMenuBox(
                        expanded = timeExpanded,
                        onExpandedChange = { timeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = newTime.ifEmpty { "选择时间" },
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = MaterialTheme.shapes.large,
                            label = { Text("时间") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = timeExpanded,
                            onDismissRequest = { timeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("无具体时间") },
                                onClick = {
                                    viewModel.onTimeChange("")
                                    timeExpanded = false
                                }
                            )
                            timeOptions.forEach { time ->
                                DropdownMenuItem(
                                    text = { Text(time) },
                                    onClick = {
                                        viewModel.onTimeChange(time)
                                        timeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { viewModel.onDescriptionChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        label = { Text("描述") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addSchedule() }) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CalendarView(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = calendar.apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 月份标题
            Text(
                text = "${year}年${month + 1}月",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 星期标题
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 日期网格
            var dayCount = 1
            for (week in 0..5) {
                if (dayCount > daysInMonth) break
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayOfWeek in 1..7) {
                        if (week == 0 && dayOfWeek < firstDayOfMonth || dayCount > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val day = dayCount
                            val dayCalendar = Calendar.getInstance().apply {
                                set(year, month, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val dayTimestamp = dayCalendar.timeInMillis
                            val isSelected = isSameDay(selectedDate, dayTimestamp)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { onDateSelected(dayTimestamp) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            dayCount++
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(
    schedule: com.toolbox.data.local.entity.Schedule,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = schedule.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else null
                )
                if (schedule.time.isNotEmpty()) {
                    Text(
                        text = schedule.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (schedule.description.isNotEmpty()) {
                    Text(
                        text = schedule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
