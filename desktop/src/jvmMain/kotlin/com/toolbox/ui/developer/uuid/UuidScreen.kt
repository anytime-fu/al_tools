package com.toolbox.ui.developer

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UuidScreen(onBack: () -> Unit) {
    var uuids by remember { mutableStateOf<List<String>>(emptyList()) }
    var count by remember { mutableStateOf("1") }
    var format by remember { mutableStateOf(0) } // 0=标准, 1=无连字符, 2=大写
    var includeBraces by remember { mutableStateOf(false) }

    fun generateUuids() {
        val n = count.toIntOrNull() ?: 1
        if (n in 1..100) {
            uuids = (1..n).map {
                val uuid = UUID.randomUUID()
                val formatted = when (format) {
                    0 -> uuid.toString()
                    1 -> uuid.toString().replace("-", "")
                    2 -> uuid.toString().uppercase()
                    else -> uuid.toString()
                }
                if (includeBraces) "{$formatted}" else formatted
            }
        }
    }

    // 初始化生成一个
    LaunchedEffect(Unit) {
        generateUuids()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UUID 生成") },
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
            // 配置区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "生成配置",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 数量
                        OutlinedTextField(
                            value = count,
                            onValueChange = { count = it },
                            modifier = Modifier.width(100.dp),
                            label = { Text("数量") },
                            singleLine = true
                        )

                        // 格式
                        Column {
                            Text(
                                text = "格式",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AppFilterChip(
                                    selected = format == 0,
                                    onClick = { format = 0 },
                                    label = { Text("标准") }
                                )
                                AppFilterChip(
                                    selected = format == 1,
                                    onClick = { format = 1 },
                                    label = { Text("无连字符") }
                                )
                                AppFilterChip(
                                    selected = format == 2,
                                    onClick = { format = 2 },
                                    label = { Text("大写") }
                                )
                            }
                        }

                        // 大括号
                        AppFilterChip(
                            selected = includeBraces,
                            onClick = { includeBraces = !includeBraces },
                            label = { Text("大括号") }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // 生成按钮
                        Button(
                            onClick = { generateUuids() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("生成")
                        }

                        // 复制全部
                        AppOutlinedButton(
                            onClick = {
                                val allUuids = uuids.joinToString("\n")
                                val selection = StringSelection(allUuids)
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制全部")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 结果列表
            Text(
                text = "生成结果 (${uuids.size} 个)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                uuids.forEachIndexed { index, uuid ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(30.dp)
                            )
                            Text(
                                text = uuid,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val selection = StringSelection(uuid)
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}