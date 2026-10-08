package com.toolbox.ui.network.http

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpClientScreen(onBack: () -> Unit) {
    var method by remember { mutableStateOf("GET") }
    var url by remember { mutableStateOf("") }
    var headersText by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var responseBody by remember { mutableStateOf("") }
    var responseInfo by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sendJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HTTP 客户端") },
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
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it },
                    modifier = Modifier.width(120.dp)
                ) {
                    OutlinedTextField(
                        value = method,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded)
                        },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        methods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    method = m
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("https://api.example.com/path") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (sending) {
                            sendJob?.cancel()
                            return@Button
                        }
                        sending = true
                        responseBody = ""
                        responseInfo = "请求中..."
                        sendJob = scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    httpCall(method, url.trim(), headersText, bodyText)
                                }
                                responseInfo = result.info
                                responseBody = result.body
                            } catch (e: Exception) {
                                responseInfo = "请求失败"
                                responseBody = e.message ?: e.toString()
                            } finally {
                                sending = false
                            }
                        }
                    },
                    colors = if (sending) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (sending) Icons.Default.Close else Icons.Default.Send,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (sending) "停止" else "发送")
                }

                IconButton(
                    onClick = {
                        if (responseBody.isNotEmpty()) {
                            Toolkit.getDefaultToolkit().systemClipboard
                                .setContents(StringSelection(responseBody), null)
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制响应")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("请求头（每行 Key: Value）", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = headersText,
                        onValueChange = { headersText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Content-Type: application/json") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("请求体", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = bodyText,
                        onValueChange = { bodyText = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("{\"key\": \"value\"}") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (responseInfo.isEmpty()) "响应" else "响应 · $responseInfo",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = responseBody,
                        onValueChange = {},
                        modifier = Modifier.fillMaxSize(),
                        readOnly = true,
                        placeholder = { Text("响应内容...") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}

private data class HttpResult(val info: String, val body: String)

private suspend fun httpCall(method: String, url: String, headersText: String, bodyText: String): HttpResult {
    val client = HttpClient(CIO) {
        expectSuccess = false
        followRedirects = true
    }
    try {
        val response = client.request(url) {
            this.method = HttpMethod.parse(method)
            headersText.lines()
                .filter { it.isNotBlank() && it.contains(":") }
                .forEach { line ->
                    val idx = line.indexOf(':')
                    header(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
                }
            if (method != "GET" && method != "HEAD" && bodyText.isNotBlank()) {
                setBody(bodyText)
            }
        }

        val text = response.bodyAsText()
        val headerInfo = response.headers.entries()
            .joinToString("\n") { (k, v) -> "$k: ${v.joinToString(", ")}" }

        return HttpResult(
            info = "${response.status.value} ${response.status.description} · ${text.length} 字符",
            body = if (headerInfo.isEmpty()) text else "$headerInfo\n\n$text"
        )
    } finally {
        client.close()
    }
}
