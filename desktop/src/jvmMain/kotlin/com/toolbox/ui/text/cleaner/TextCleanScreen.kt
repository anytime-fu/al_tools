package com.toolbox.ui.text.cleaner

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import com.toolbox.util.CleanOptions
import com.toolbox.util.TextCleaner

private data class CleanOption(
    val id: String,
    val label: String,
    val checked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextCleanScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var trimLines by remember { mutableStateOf(true) }
    var removeEmptyLines by remember { mutableStateOf(true) }
    var mergeSpaces by remember { mutableStateOf(false) }
    var removeSpaces by remember { mutableStateOf(false) }
    var dedupeLines by remember { mutableStateOf(false) }
    var sortLines by remember { mutableStateOf(false) }
    var removeInvisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文本清理") },
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "原始文本",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("输入要清理的文本...") }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "清理结果",
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CleanCheck("去除行首尾空格", trimLines) { trimLines = it }
                CleanCheck("去除空行", removeEmptyLines) { removeEmptyLines = it }
                CleanCheck("合并连续空格", mergeSpaces) { mergeSpaces = it }
                CleanCheck("去除所有空格", removeSpaces) { removeSpaces = it }
                CleanCheck("去除重复行", dedupeLines) { dedupeLines = it }
                CleanCheck("行排序", sortLines) { sortLines = it }
                CleanCheck("去除不可见字符", removeInvisible) { removeInvisible = it }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        output = TextCleaner.clean(
                            input,
                            CleanOptions(
                                trimLines = trimLines,
                                removeEmptyLines = removeEmptyLines,
                                mergeSpaces = mergeSpaces,
                                removeSpaces = removeSpaces,
                                dedupeLines = dedupeLines,
                                sortLines = sortLines,
                                removeInvisible = removeInvisible
                            )
                        )
                        message = "清理完成：${input.length} 字符 → ${output.length} 字符"
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清理")
                }

                AppOutlinedButton(
                    onClick = {
                        if (output.isNotEmpty()) {
                            Toolkit.getDefaultToolkit().systemClipboard
                                .setContents(StringSelection(output), null)
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
                        message = ""
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
