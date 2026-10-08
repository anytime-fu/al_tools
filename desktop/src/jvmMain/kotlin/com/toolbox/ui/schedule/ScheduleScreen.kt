package com.toolbox.ui.schedule

import com.toolbox.ui.components.appClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.data.local.entity.Schedule
import com.toolbox.data.repository.ScheduleRepository
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(onBack: (() -> Unit)? = null) {
    val scheduleRepository: ScheduleRepository by inject(ScheduleRepository::class.java)
    val coroutineScope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<Schedule?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Get schedules from repository
    val schedules by scheduleRepository.getAllSchedules().collectAsState(initial = emptyList())
    
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = { Text("日程管理") },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "添加",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
        
        // Date selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedDate -= 86400000 }) {
                Icon(
                    Icons.Default.ChevronLeft, 
                    contentDescription = "前一天",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Text(
                text = dateFormat.format(Date(selectedDate)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            IconButton(onClick = { selectedDate += 86400000 }) {
                Icon(
                    Icons.Default.ChevronRight, 
                    contentDescription = "后一天",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            TextButton(onClick = { selectedDate = System.currentTimeMillis() }) {
                Text("今天")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Schedule list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val daySchedules = schedules.filter {
                val calendar1 = Calendar.getInstance().apply { timeInMillis = it.date }
                val calendar2 = Calendar.getInstance().apply { timeInMillis = selectedDate }
                calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
            }
            
            if (daySchedules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无日程",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            items(daySchedules.sortedBy { it.time }) { schedule ->
                ScheduleListItem(
                    schedule = schedule,
                    onClick = { selectedSchedule = schedule },
                    onToggleComplete = {
                        coroutineScope.launch {
                            scheduleRepository.updateSchedule(
                                schedule.copy(
                                    isCompleted = !schedule.isCompleted,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    },
                    onDelete = {
                        coroutineScope.launch {
                            scheduleRepository.deleteSchedule(schedule)
                        }
                    }
                )
            }
        }
    }
    
    // Add dialog
    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, description, time ->
                coroutineScope.launch {
                    val newSchedule = Schedule(
                        title = title,
                        description = description,
                        date = selectedDate,
                        time = time,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    scheduleRepository.insertSchedule(newSchedule)
                }
                showAddDialog = false
            }
        )
    }
    
    // Detail dialog
    selectedSchedule?.let { schedule ->
        ScheduleDetailDialog(
            schedule = schedule,
            onDismiss = { selectedSchedule = null },
            onDelete = {
                coroutineScope.launch {
                    scheduleRepository.deleteSchedule(schedule)
                }
                selectedSchedule = null
            }
        )
    }
}

@Composable
fun ScheduleListItem(
    schedule: Schedule,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .appClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = schedule.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (schedule.description.isNotBlank()) {
                    Text(
                        text = schedule.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = schedule.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
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

@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加日程") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题") },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("描述") },
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("时间 (HH:mm)") },
                    placeholder = { Text("10:00") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, description, time) },
                enabled = title.isNotBlank() && time.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ScheduleDetailDialog(
    schedule: Schedule,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(schedule.title) },
        text = {
            Column {
                if (schedule.description.isNotBlank()) {
                    Text("描述: ${schedule.description}")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text("时间: ${schedule.time}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("状态: ${if (schedule.isCompleted) "已完成" else "未完成"}")
            }
        },
        confirmButton = {
            Button(onClick = onDelete) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
