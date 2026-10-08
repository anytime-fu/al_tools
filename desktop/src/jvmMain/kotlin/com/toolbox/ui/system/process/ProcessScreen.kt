package com.toolbox.ui.system.process

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ProcInfo(
    val pid: Long,
    val name: String,
    val command: String,
    val user: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessScreen(onBack: () -> Unit) {
    var processes by remember { mutableStateOf<List<ProcInfo>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadProcesses() {
        loading = true
        message = ""
        scope.launch {
            processes = withContext(Dispatchers.IO) {
                ProcessHandle.allProcesses()
                    .filter { it.isAlive }
                    .map { p ->
                        val info = p.info()
                        ProcInfo(
                            pid = p.pid(),
                            name = info.command().map { cmd -> cmd.substringAfterLast('\\').substringAfterLast('/') }.orElse(""),
                            command = info.commandLine().orElse(""),
                            user = info.user().orElse("")
                        )
                    }
                    .toList()
                    .sortedBy { it.name.lowercase() }
            }
            loading = false
            message = "共 ${processes.size} 个进程"
        }
    }

    LaunchedEffect(Unit) { loadProcesses() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("进程管理") },
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
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("按名称或 PID 过滤...") },
                    singleLine = true
                )
                OutlinedButton(onClick = { loadProcesses() }, enabled = !loading) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新")
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (loading) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(8.dp))

            val filtered = remember(processes, searchText) {
                if (searchText.isBlank()) processes
                else processes.filter {
                    it.name.contains(searchText, ignoreCase = true) || it.pid.toString() == searchText.trim()
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered) { proc ->
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = proc.name.ifBlank { "(未知)" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "PID ${proc.pid}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = proc.command.ifBlank { proc.user },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) {
                                            ProcessHandle.of(proc.pid).map { it.destroy() }.orElse(false)
                                        }
                                        message = if (ok) {
                                            "已发送结束信号: PID ${proc.pid}"
                                        } else {
                                            "无法结束 PID ${proc.pid}（权限不足或已退出）"
                                        }
                                        loadProcesses()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "结束进程",
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
