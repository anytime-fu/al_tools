package com.toolbox.ui.developer

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton
import com.toolbox.util.CodecUtils

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlEncodeScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var isEncode by remember { mutableStateOf(true) }
    var encodeSpaceAs by remember { mutableStateOf(true) } // true = %20, false = +
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URL 编解码") },
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
            // 模式切换和操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    selected = isEncode,
                    onClick = { isEncode = true },
                    label = { Text("编码") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                )
                AppFilterChip(
                    selected = !isEncode,
                    onClick = { isEncode = false },
                    label = { Text("解码") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) }
                )

                if (isEncode) {
                    AppFilterChip(
                        selected = encodeSpaceAs,
                        onClick = { encodeSpaceAs = !encodeSpaceAs },
                        label = { Text(if (encodeSpaceAs) "%20" else "+") }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        try {
                            errorMessage = null
                            if (isEncode) {
                                output = CodecUtils.urlEncode(input, encodeSpaceAs)
                            } else {
                                output = CodecUtils.urlDecode(input)
                            }
                        } catch (e: Exception) {
                            errorMessage = if (isEncode) "编码失败" else "解码失败: ${e.message}"
                        }
                    }
                ) {
                    Icon(Icons.Default.Transform, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEncode) "编码" else "解码")
                }

                AppOutlinedButton(
                    onClick = {
                        if (output.isNotEmpty()) {
                            val selection = StringSelection(output)
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制结果")
                }

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
                        text = if (isEncode) "原始文本" else "URL 编码文本",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text(if (isEncode) "输入要编码的文本或URL..." else "输入要解码的URL...") },
                        isError = errorMessage != null
                    )
                }

                // 输出区域
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEncode) "URL 编码结果" else "解码结果",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = output,
                        onValueChange = {},
                        modifier = Modifier.fillMaxSize(),
                        readOnly = true,
                        placeholder = { Text("结果...") }
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