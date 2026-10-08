package com.toolbox.ui.network.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private val wellKnownPorts = mapOf(
    21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP", 53 to "DNS",
    80 to "HTTP", 110 to "POP3", 143 to "IMAP", 443 to "HTTPS", 445 to "SMB",
    993 to "IMAPS", 995 to "POP3S", 1433 to "MSSQL", 1521 to "Oracle",
    3306 to "MySQL", 3389 to "RDP", 5432 to "PostgreSQL", 5900 to "VNC",
    6379 to "Redis", 8080 to "HTTP-Proxy", 8443 to "HTTPS-Alt", 27017 to "MongoDB",
    9200 to "Elasticsearch"
)

private data class OpenPort(val port: Int, val service: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScanScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("127.0.0.1") }
    var portFrom by remember { mutableStateOf("1") }
    var portTo by remember { mutableStateOf("1024") }
    var openPorts by remember { mutableStateOf<List<OpenPort>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableStateOf(0) }
    var totalPorts by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("端口扫描") },
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
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.weight(2f),
                    label = { Text("目标主机") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = portFrom,
                    onValueChange = { portFrom = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("起始端口") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = portTo,
                    onValueChange = { portTo = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("结束端口") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (scanning) {
                            scanJob?.cancel()
                            return@Button
                        }
                        val from = portFrom.toIntOrNull() ?: 1
                        val to = portTo.toIntOrNull() ?: 1024
                        if (from < 1 || to > 65535 || from > to) {
                            message = "端口范围无效（1-65535）"
                            return@Button
                        }
                        openPorts = emptyList()
                        scannedCount = 0
                        totalPorts = to - from + 1
                        message = ""
                        scanning = true
                        scanJob = scope.launch {
                            var cancelled = false
                            try {
                                val found = withContext(Dispatchers.IO) {
                                    scanPorts(host.trim(), from, to) { scannedCount = it }
                                }
                                openPorts = found
                                message = "扫描完成：发现 ${found.size} 个开放端口"
                            } catch (e: CancellationException) {
                                cancelled = true
                            } finally {
                                scanning = false
                                if (cancelled) message = "已取消扫描"
                            }
                        }
                    },
                    colors = if (scanning) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (scanning) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (scanning) "停止" else "扫描")
                }
            }

            if (scanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (totalPorts > 0) scannedCount.toFloat() / totalPorts else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "已扫描 $scannedCount / $totalPorts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(openPorts) { item ->
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
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = item.port.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.service,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "开放",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun scanPorts(
    host: String,
    from: Int,
    to: Int,
    onProgress: (Int) -> Unit
): List<OpenPort> {
    val results = java.util.Collections.synchronizedList(mutableListOf<OpenPort>())
    val semaphore = Semaphore(128)
    val scanned = java.util.concurrent.atomic.AtomicInteger(0)

    kotlinx.coroutines.coroutineScope {
        for (port in from..to) {
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    if (!isActive) return@withPermit
                    try {
                        Socket().use { s ->
                            s.connect(InetSocketAddress(host, port), 800)
                        }
                        results.add(OpenPort(port, wellKnownPorts[port] ?: "未知服务"))
                    } catch (_: Exception) {
                    }
                    onProgress(scanned.incrementAndGet())
                }
            }
        }
    }
    return results.sortedBy { it.port }
}
