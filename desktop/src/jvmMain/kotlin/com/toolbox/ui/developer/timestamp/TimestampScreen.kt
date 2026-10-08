package com.toolbox.ui.developer

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import com.toolbox.util.TimeUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimestampScreen(onBack: () -> Unit) {
    var timestamp by remember { mutableStateOf(System.currentTimeMillis().toString()) }
    var dateTime by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(0) } // 0 = 时间戳转日期, 1 = 日期转时间戳
    var dateFormat by remember { mutableStateOf("yyyy-MM-dd HH:mm:ss") }
    var selectedZone by remember { mutableStateOf("Asia/Shanghai") }

    val zones = listOf("Asia/Shanghai", "UTC", "America/New_York", "Europe/London", "Asia/Tokyo")

    // 初始化
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        timestamp = now.toString()
        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.of(selectedZone))
        dateTime = ldt.format(formatter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时间戳工具") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 模式切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    selected = inputMode == 0,
                    onClick = { inputMode = 0 },
                    label = { Text("时间戳 → 日期") },
                    leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null) }
                )
                AppFilterChip(
                    selected = inputMode == 1,
                    onClick = { inputMode = 1 },
                    label = { Text("日期 → 时间戳") },
                    leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // 时区选择
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedZone,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("时区") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().width(200.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        zones.forEach { zone ->
                            DropdownMenuItem(
                                text = { Text(zone) },
                                onClick = {
                                    selectedZone = zone
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 当前时间
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "当前时间",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "时间戳: ${System.currentTimeMillis()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "日期: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat))}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 转换区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 输入区域
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (inputMode == 0) "时间戳（毫秒）" else "日期时间",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = if (inputMode == 0) timestamp else dateTime,
                        onValueChange = { 
                            if (inputMode == 0) timestamp = it else dateTime = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (inputMode == 0) "输入时间戳..." else "输入日期时间...") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 日期格式（日期转时间戳模式）
                    if (inputMode == 1) {
                        OutlinedTextField(
                            value = dateFormat,
                            onValueChange = { dateFormat = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("日期格式") },
                            placeholder = { Text("yyyy-MM-dd HH:mm:ss") },
                            singleLine = true
                        )
                    }
                }

                // 转换按钮
                Column(
                    modifier = Modifier.padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                if (inputMode == 0) {
                                    dateTime = TimeUtils.formatTimestamp(timestamp.toLong(), dateFormat, selectedZone)
                                } else {
                                    timestamp = TimeUtils.parseToTimestamp(dateTime, dateFormat, selectedZone).toString()
                                }
                            } catch (e: Exception) {
                                // 错误处理
                            }
                        }
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("转换")
                    }

                    Button(
                        onClick = {
                            timestamp = TimeUtils.currentTimestamp().toString()
                            dateTime = TimeUtils.formatNow(dateFormat)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("当前时间")
                    }
                }

                // 输出区域
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (inputMode == 0) "日期时间" else "时间戳（毫秒）",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = if (inputMode == 0) dateTime else timestamp,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 复制按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppOutlinedButton(
                            onClick = {
                                val text = if (inputMode == 0) dateTime else timestamp
                                val selection = StringSelection(text)
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 常用格式
            Text(
                text = "常用日期格式",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyyMMddHHmmss",
                    "yyyy-MM-dd'T'HH:mm:ss"
                ).forEach { format ->
                    AssistChip(
                        onClick = { dateFormat = format },
                        label = { Text(format, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }
    }
}