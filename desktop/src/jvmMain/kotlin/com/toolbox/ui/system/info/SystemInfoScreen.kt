package com.toolbox.ui.system.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import oshi.SystemInfo
import java.net.InetAddress
import java.net.NetworkInterface

private data class SysInfo(
    val os: String,
    val cpu: String,
    val memory: String,
    val disks: String,
    val network: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfoScreen(onBack: () -> Unit) {
    var info by remember { mutableStateOf<SysInfo?>(null) }
    var loading by remember { mutableStateOf(false) }

    suspend fun loadInfo(): SysInfo = withContext(Dispatchers.IO) {
        val si = SystemInfo()
        val os = si.operatingSystem
        val hw = si.hardware

        val osText = buildString {
            appendLine("系统: ${os.family} ${os.versionInfo}")
            appendLine("架构: ${System.getProperty("os.arch")}")
            appendLine("运行时间: ${formatUptime(os.systemUptime)}")
            appendLine("Java: ${System.getProperty("java.version")}")
            appendLine("主机名: ${InetAddress.getLocalHost().hostName}")
        }

        val cpuText = buildString {
            val proc = hw.processor
            appendLine("型号: ${proc.processorIdentifier.name.trim()}")
            appendLine("物理核心: ${proc.physicalProcessorCount} · 线程: ${proc.logicalProcessorCount}")
            appendLine("系统 CPU 负载: ${"%.1f%%".format(proc.getSystemCpuLoad(200) * 100)}")
        }

        val mem = hw.memory
        val totalMb = mem.total / 1024 / 1024
        val availMb = mem.available / 1024 / 1024
        val memText = buildString {
            appendLine("总内存: $totalMb MB")
            appendLine("可用: $availMb MB · 已用: ${totalMb - availMb} MB")
            appendLine("使用率: ${"%.1f%%".format((totalMb - availMb) * 100.0 / totalMb)}")
        }

        val disksText = os.fileSystem.fileStores.joinToString("\n") { fs ->
            val totalGb = fs.totalSpace / 1024.0 / 1024 / 1024
            val freeGb = fs.usableSpace / 1024.0 / 1024 / 1024
            "${fs.name} (${fs.mount}) ${"%.1f".format(totalGb)}GB · 可用 ${"%.1f".format(freeGb)}GB"
        }

        val netText = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .joinToString("\n") { ni ->
                val ips = ni.inetAddresses.toList()
                    .filter { !it.isLoopbackAddress }
                    .joinToString(", ") { it.hostAddress }
                val mac = ni.hardwareAddress?.joinToString(":") { b -> "%02X".format(b) } ?: ""
                "${ni.displayName}: $ips · $mac"
            }

        SysInfo(osText.trim(), cpuText.trim(), memText.trim(), disksText.ifBlank { "无" }, netText.ifBlank { "无" })
    }

    LaunchedEffect(Unit) {
        loading = true
        info = runCatching { loadInfo() }.getOrElse { SysInfo("获取失败: ${it.message}", "", "", "", "") }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统信息") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    loading = true
                    info = null
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("刷新")
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            info?.let { data ->
                InfoCard("操作系统", data.os)
                InfoCard("CPU", data.cpu)
                InfoCard("内存", data.memory)
                InfoCard("磁盘", data.disks)
                InfoCard("网络", data.network)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    return buildString {
        if (d > 0) append("${d}天")
        if (h > 0) append("${h}小时")
        append("${m}分")
    }
}
