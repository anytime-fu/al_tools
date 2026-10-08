package com.toolbox.ui.developer

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSerializationApi::class)
@Composable
fun JsonFormatScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var indentSize by remember { mutableStateOf(2) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JSON 格式化") },
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
            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val jsonElement = Json.parseToJsonElement(input)
                            output = Json.encodeToString(JsonElement.serializer(), jsonElement)
                            errorMessage = null
                        } catch (e: Exception) {
                            errorMessage = "压缩失败: ${e.message}"
                        }
                    }
                ) {
                    Icon(Icons.Default.Compress, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("压缩")
                }

                Button(
                    onClick = {
                        try {
                            val jsonElement = Json.parseToJsonElement(input)
                            output = Json { prettyPrint = true; prettyPrintIndent = " ".repeat(indentSize) }
                                .encodeToString(JsonElement.serializer(), jsonElement)
                            errorMessage = null
                        } catch (e: Exception) {
                            errorMessage = "格式化失败: ${e.message}"
                        }
                    }
                ) {
                    Icon(Icons.Default.FormatAlignLeft, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("格式化")
                }

                Button(
                    onClick = {
                        try {
                            Json.parseToJsonElement(input)
                            errorMessage = null
                            output = "✓ JSON 格式有效"
                        } catch (e: Exception) {
                            errorMessage = "验证失败: ${e.message}"
                            output = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("验证")
                }

                Spacer(modifier = Modifier.weight(1f))

                // 缩进设置
                OutlinedTextField(
                    value = indentSize.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { size ->
                            if (size in 1..8) indentSize = size
                        }
                    },
                    label = { Text("缩进") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )

                AppOutlinedButton(
                    onClick = {
                        input = ""
                        output = ""
                        errorMessage = null
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 输入输出区域
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 输入区域
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "输入",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("粘贴 JSON 文本...") },
                        isError = errorMessage != null
                    )
                }

                // 输出区域
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "输出",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                if (output.isNotEmpty()) {
                                    val selection = StringSelection(output)
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = output,
                        onValueChange = {},
                        modifier = Modifier.fillMaxSize(),
                        readOnly = true,
                        placeholder = { Text("格式化结果...") }
                    )
                }
            }

            // 错误提示
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}