package com.toolbox.ui.developer

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import com.toolbox.util.CodecUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var md5 by remember { mutableStateOf("") }
    var sha1 by remember { mutableStateOf("") }
    var sha256 by remember { mutableStateOf("") }
    var sha512 by remember { mutableStateOf("") }
    var isFileMode by remember { mutableStateOf(false) }
    var filePath by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun calculateHash(text: String) {
        try {
            errorMessage = null
            val bytes = text.toByteArray(Charsets.UTF_8)
            md5 = CodecUtils.hashBytes(bytes, "MD5")
            sha1 = CodecUtils.hashBytes(bytes, "SHA-1")
            sha256 = CodecUtils.hashBytes(bytes, "SHA-256")
            sha512 = CodecUtils.hashBytes(bytes, "SHA-512")
        } catch (e: Exception) {
            errorMessage = "计算失败: ${e.message}"
        }
    }

    fun calculateFileHash(path: String) {
        try {
            errorMessage = null
            val file = File(path)
            if (!file.exists()) {
                errorMessage = "文件不存在"
                return
            }
            val bytes = file.readBytes()
            md5 = CodecUtils.hashBytes(bytes, "MD5")
            sha1 = CodecUtils.hashBytes(bytes, "SHA-1")
            sha256 = CodecUtils.hashBytes(bytes, "SHA-256")
            sha512 = CodecUtils.hashBytes(bytes, "SHA-512")
        } catch (e: Exception) {
            errorMessage = "计算失败: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("哈希计算") },
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
            // 模式切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    selected = !isFileMode,
                    onClick = { isFileMode = false },
                    label = { Text("文本模式") },
                    leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) }
                )
                AppFilterChip(
                    selected = isFileMode,
                    onClick = { isFileMode = true },
                    label = { Text("文件模式") },
                    leadingIcon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) }
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (isFileMode) {
                            calculateFileHash(filePath)
                        } else {
                            calculateHash(input)
                        }
                    }
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("计算")
                }

                AppOutlinedButton(
                    onClick = {
                        input = ""
                        filePath = ""
                        md5 = ""
                        sha1 = ""
                        sha256 = ""
                        sha512 = ""
                        errorMessage = null
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 输入区域
            if (isFileMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = filePath,
                        onValueChange = { filePath = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("文件路径") },
                        placeholder = { Text("输入文件路径...") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            // 文件选择对话框
                            val fileChooser = javax.swing.JFileChooser()
                            val result = fileChooser.showOpenDialog(null)
                            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                filePath = fileChooser.selectedFile.absolutePath
                                calculateFileHash(filePath)
                            }
                        }
                    ) {
                        Text("选择文件")
                    }
                }
            } else {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("输入文本") },
                    placeholder = { Text("输入要计算哈希的文本...") },
                    minLines = 3
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 结果显示
            Text(
                text = "计算结果",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            HashResultCard("MD5", md5)
            Spacer(modifier = Modifier.height(8.dp))
            HashResultCard("SHA-1", sha1)
            Spacer(modifier = Modifier.height(8.dp))
            HashResultCard("SHA-256", sha256)
            Spacer(modifier = Modifier.height(8.dp))
            HashResultCard("SHA-512", sha512)

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

@Composable
private fun HashResultCard(algorithm: String, hash: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = algorithm,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = hash.ifEmpty { "-" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hash.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val selection = StringSelection(hash)
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                }
            }
        }
    }
}

