package com.toolbox.ui.file.rename

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import javax.swing.JFileChooser

private enum class RenameMode(val label: String) {
    REPLACE("查找替换"),
    REGEX("正则替换"),
    PREFIX_SUFFIX("前后缀"),
    NUMBERING("编号命名"),
    UPPER("转大写"),
    LOWER("转小写"),
    TITLE("首字母大写")
}

private data class RenameItem(
    val file: File,
    val newName: String
) {
    val changed: Boolean get() = newName != file.name
    val valid: Boolean get() = newName.isNotBlank()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchRenameScreen(onBack: () -> Unit) {
    var dirPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var mode by remember { mutableStateOf(RenameMode.REPLACE) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var regexPattern by remember { mutableStateOf("") }
    var regexReplace by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var numberTemplate by remember { mutableStateOf("{n}") }
    var startNumber by remember { mutableStateOf("1") }
    var message by remember { mutableStateOf("") }

    val renameItems = remember(files, mode, findText, replaceText, regexPattern, regexReplace, prefix, suffix, numberTemplate, startNumber) {
        buildList {
            for (f in files) {
                add(RenameItem(f, computeNewName(f, mode, findText, replaceText, regexPattern, regexReplace, prefix, suffix, numberTemplate, startNumber, files.indexOf(f))))
            }
        }
    }

    val duplicateNames = remember(renameItems) {
        renameItems.groupBy { it.newName }.filterValues { it.size > 1 }.keys
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量重命名") },
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
                    value = dirPath,
                    onValueChange = { dirPath = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("目标目录") },
                    placeholder = { Text("选择或输入目录路径") }
                )

                AppOutlinedButton(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            dirPath = chooser.selectedFile.absolutePath
                        }
                    }
                ) {
                    Text("浏览...")
                }

                Button(
                    onClick = {
                        val dir = File(dirPath)
                        if (!dir.isDirectory) {
                            message = "目录不存在"
                            return@Button
                        }
                        files = dir.listFiles { f -> f.isFile }?.sortedBy { it.name } ?: emptyList()
                        message = "已加载 ${files.size} 个文件"
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("加载文件")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RenameMode.values().forEach { m ->
                    AppFilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (mode) {
                RenameMode.REPLACE -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("查找") }
                    )
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("替换为") }
                    )
                }
                RenameMode.REGEX -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = regexPattern,
                        onValueChange = { regexPattern = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("正则表达式") }
                    )
                    OutlinedTextField(
                        value = regexReplace,
                        onValueChange = { regexReplace = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("替换为") }
                    )
                }
                RenameMode.PREFIX_SUFFIX -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("前缀") }
                    )
                    OutlinedTextField(
                        value = suffix,
                        onValueChange = { suffix = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("后缀") }
                    )
                }
                RenameMode.NUMBERING -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = numberTemplate,
                        onValueChange = { numberTemplate = it },
                        modifier = Modifier.weight(2f),
                        label = { Text("命名模板（{n} 为序号）") }
                    )
                    OutlinedTextField(
                        value = startNumber,
                        onValueChange = { startNumber = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("起始序号") }
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        var ok = 0
                        var failed = 0
                        for (item in renameItems) {
                            if (!item.changed || !item.valid || item.newName in duplicateNames) continue
                            val target = File(item.file.parentFile, item.newName)
                            if (target.exists() && target != item.file) {
                                failed++
                                continue
                            }
                            if (item.file.renameTo(target)) ok++ else failed++
                        }
                        message = "重命名完成：成功 $ok 个" + if (failed > 0) "，失败 $failed 个" else ""
                        files = File(dirPath).listFiles { f -> f.isFile }?.sortedBy { it.name } ?: emptyList()
                    },
                    enabled = files.isNotEmpty() && duplicateNames.isEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("应用重命名")
                }

                if (duplicateNames.isNotEmpty()) {
                    Text(
                        text = "存在重名，请调整规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "原文件名",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "新文件名",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(renameItems) { item ->
                    RenameRow(item, item.newName in duplicateNames)
                }
            }
        }
    }
}

@Composable
private fun RenameRow(item: RenameItem, isDuplicate: Boolean) {
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.file.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.newName,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isDuplicate -> MaterialTheme.colorScheme.error
                    item.changed -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun computeNewName(
    file: File,
    mode: RenameMode,
    findText: String,
    replaceText: String,
    regexPattern: String,
    regexReplace: String,
    prefix: String,
    suffix: String,
    numberTemplate: String,
    startNumber: String,
    index: Int
): String {
    val name = file.name
    val base = file.nameWithoutExtension
    val ext = file.extension
    val numbered = (startNumber.toIntOrNull() ?: 1) + index
    return when (mode) {
        RenameMode.REPLACE -> if (findText.isEmpty()) name else name.replace(findText, replaceText)
        RenameMode.REGEX -> try {
            if (regexPattern.isEmpty()) name else Regex(regexPattern).replace(name, regexReplace)
        } catch (_: Exception) {
            name
        }
        RenameMode.PREFIX_SUFFIX -> listOfNotNull(
            prefix.ifEmpty { null },
            base,
            suffix.ifEmpty { null }
        ).joinToString("") + if (ext.isNotEmpty()) ".$ext" else ""
        RenameMode.NUMBERING -> numberTemplate.replace("{n}", numbered.toString()) +
            if (ext.isNotEmpty()) ".$ext" else ""
        RenameMode.UPPER -> name.uppercase()
        RenameMode.LOWER -> name.lowercase()
        RenameMode.TITLE -> base.lowercase().replaceFirstChar { it.uppercase() } +
            if (ext.isNotEmpty()) ".${ext.lowercase()}" else ""
    }
}
