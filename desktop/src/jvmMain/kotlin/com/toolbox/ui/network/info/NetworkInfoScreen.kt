package com.toolbox.ui.network.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInfoScreen(onBack: () -> Unit) {
    var localInfo by remember { mutableStateOf("") }
    var queryHost by remember { mutableStateOf("") }
    var dnsResult by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        localInfo = withContext(Dispatchers.IO) { buildLocalInfo() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网络信息") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text("本机地址", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = localInfo,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                readOnly = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("DNS 查询", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryHost,
                    onValueChange = { queryHost = it },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    placeholder = { Text("域名或 IP") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        val host = queryHost.trim()
                        if (host.isEmpty()) return@Button
                        loading = true
                        dnsResult = "查询中..."
                        scope.launch {
                            dnsResult = withContext(Dispatchers.IO) { queryDns(host) }
                            loading = false
                        }
                    },
                    enabled = !loading
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("查询")
                }
            }

            if (dnsResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dnsResult,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    readOnly = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )
            }
        }
    }
}

private fun buildLocalInfo(): String {
    val sb = StringBuilder()
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
        for (ni in interfaces) {
            if (!ni.isUp || ni.isLoopback) continue
            sb.appendLine("[${ni.displayName}]")
            ni.interfaceAddresses.forEach { addr ->
                val ip = addr.address
                val label = if (ip is Inet4Address) "IPv4" else if (ip is Inet6Address) "IPv6" else "地址"
                sb.appendLine("  $label: ${ip.hostAddress}")
            }
            val mac = ni.hardwareAddress
            if (mac != null) {
                sb.appendLine("  MAC: " + mac.joinToString(":") { "%02X".format(it) })
            }
            sb.appendLine()
        }
        InetAddress.getLocalHost().let {
            sb.appendLine("主机名: ${it.hostName}")
            sb.appendLine("本机地址: ${it.hostAddress}")
        }
    } catch (e: Exception) {
        sb.appendLine("获取失败: ${e.message}")
    }
    return sb.toString().trim()
}

private fun queryDns(host: String): String {
    val sb = StringBuilder()
    try {
        val addresses = InetAddress.getAllByName(host)
        sb.appendLine("主机名: ${addresses.firstOrNull()?.canonicalHostName ?: host}")
        addresses.forEach { addr ->
            val label = if (addr is Inet4Address) "IPv4" else if (addr is Inet6Address) "IPv6" else "地址"
            sb.appendLine("$label: ${addr.hostAddress}")
        }
    } catch (e: Exception) {
        sb.appendLine("解析失败: ${e.message}")
    }
    return sb.toString().trim()
}
