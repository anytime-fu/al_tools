package com.toolbox.ui.system.clipboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryScreen(onBack: () -> Unit) {
    val history = remember { mutableStateListOf<String>() }
    var watching by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(watching) {
        var last: String? = null
        while (watching) {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val contents = clipboard.getContents(null)
                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    val text = contents.getTransferData(DataFlavor.stringFlavor) as? String
                    if (!text.isNullOrBlank() && text != last) {
                        last = text
                        val trimmed = text.trim()
                        history.remove(trimmed)
                        history.add(0, trimmed)
                        if (history.size > 50) {
                            history.removeAt(history.size - 1)
                        }
                    }
                }
            } catch (_: Exception) {
            }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("剪贴板历史") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = watching, onCheckedChange = { watching = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (watching) "监听中" else "已暂停", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { history.clear(); message = "已清空历史" }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("清空历史")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (message.isNotEmpty()) message else "记录最近 ${history.size} 条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(history) { text ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = text.replace('\n', ' '),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    Toolkit.getDefaultToolkit().systemClipboard
                                        .setContents(StringSelection(text), null)
                                    message = "已复制到剪贴板"
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                            }

                            IconButton(
                                onClick = { history.remove(text) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
