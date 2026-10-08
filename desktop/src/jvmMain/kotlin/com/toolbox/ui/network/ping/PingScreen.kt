package com.toolbox.ui.network.ping

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private fun decodeLine(raw: String): String {
    val bytes = raw.toByteArray(Charsets.ISO_8859_1)
    return try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: Exception) {
        String(bytes, Charset.forName("GBK"))
    }
}

private enum class NetTool(val label: String) {
    PING("Ping"),
    TRACEROUTE("路由追踪")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("www.baidu.com") }
    var tool by remember { mutableStateOf(NetTool.PING) }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var runJob by remember { mutableStateOf<Job?>(null) }
    var process by remember { mutableStateOf<Process?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ping 工具") },
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
                NetTool.values().forEach { t ->
                    FilterChip(
                        selected = tool == t,
                        onClick = { tool = t },
                        label = { Text(t.label) }
                    )
                }

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("目标主机") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (running) {
                            runJob?.cancel()
                            process?.destroy()
                            return@Button
                        }
                        val target = host.trim()
                        if (target.isEmpty()) return@Button
                        output = ""
                        running = true
                        runJob = scope.launch {
                            var cancelled = false
                            try {
                                withContext(Dispatchers.IO) {
                                    val command = buildCommand(tool, target)
                                    val proc = ProcessBuilder(command)
                                        .redirectErrorStream(true)
                                        .start()
                                    process = proc
                                    proc.inputStream.bufferedReader(Charsets.ISO_8859_1).use { reader ->
                                        var line = reader.readLine()
                                        while (line != null) {
                                            val l = decodeLine(line)
                                            withContext(Dispatchers.Main) {
                                                output += l + "\n"
                                            }
                                            line = reader.readLine()
                                        }
                                    }
                                    proc.waitFor()
                                }
                            } catch (e: CancellationException) {
                                cancelled = true
                            } catch (e: Exception) {
                                output += "执行失败: ${e.message}\n"
                            } finally {
                                running = false
                                process = null
                                if (cancelled) output += "\n[已中止]"
                            }
                        }
                    },
                    colors = if (running) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (running) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (running) "停止" else "执行")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = output,
                onValueChange = {},
                modifier = Modifier.fillMaxSize(),
                readOnly = true,
                placeholder = { Text("执行结果...") },
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
        }
    }
}

private fun buildCommand(tool: NetTool, host: String): List<String> {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    return when (tool) {
        NetTool.PING -> if (isWindows) {
            listOf("ping", "-n", "4", host)
        } else {
            listOf("ping", "-c", "4", host)
        }
        NetTool.TRACEROUTE -> if (isWindows) {
            listOf("tracert", "-d", host)
        } else {
            listOf("traceroute", "-n", host)
        }
    }
}
