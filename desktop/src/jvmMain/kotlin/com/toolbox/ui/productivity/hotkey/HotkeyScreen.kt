package com.toolbox.ui.productivity.hotkey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.data.repository.SettingsRepository
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

private const val SETTINGS_KEY = "hotkey_mappings"

private data class HotkeyMapping(
    val name: String,
    val hotkey: String,
    val type: String,
    val target: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotkeyScreen(onBack: () -> Unit) {
    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val scope = rememberCoroutineScope()
    val mappings = remember { mutableStateListOf<HotkeyMapping>() }
    var listening by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }
    var draftName by remember { mutableStateOf("") }
    var draftHotkey by remember { mutableStateOf("") }
    var draftType by remember { mutableStateOf("FILE") }
    var draftTarget by remember { mutableStateOf("") }

    fun save() {
        val encoded = mappings.joinToString("\n") { "${it.name}\t${it.hotkey}\t${it.type}\t${it.target}" }
        scope.launch {
            settingsRepository.saveSetting(SETTINGS_KEY, encoded)
        }
    }

    fun runAction(mapping: HotkeyMapping) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    when (mapping.type) {
                        "URL" -> Desktop.getDesktop().browse(URI(mapping.target))
                        "CMD" -> {
                            val os = System.getProperty("os.name").orEmpty().lowercase()
                            if (os.contains("win")) {
                                ProcessBuilder("cmd", "/c", mapping.target).start()
                            } else {
                                ProcessBuilder("sh", "-c", mapping.target).start()
                            }
                        }
                        else -> Desktop.getDesktop().open(File(mapping.target))
                    }
                }
            }
            message = result.fold(
                onSuccess = { "已触发「${mapping.name}」(${mapping.hotkey})" },
                onFailure = { "执行失败: ${it.message}" }
            )
        }
    }

    LaunchedEffect(Unit) {
        val saved = settingsRepository.getSettingValue(SETTINGS_KEY, "")
        if (saved.isNotBlank()) {
            saved.lines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split("\t", limit = 4)
                if (parts.size == 4) {
                    mappings.add(HotkeyMapping(parts[0], parts[1], parts[2], parts[3]))
                }
            }
        }
    }

    DisposableEffect(Unit) {
        HotkeyManager.onCombo = { combo ->
            SwingUtilities.invokeLater {
                if (recording) {
                    draftHotkey = combo
                    recording = false
                    message = "已录制快捷键: $combo"
                } else {
                    mappings.firstOrNull { it.hotkey == combo }?.let { runAction(it) }
                }
            }
        }
        onDispose {
            HotkeyManager.onCombo = null
            HotkeyManager.stop()
            listening = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("快捷键管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingIndex = -1
                        draftName = ""
                        draftHotkey = ""
                        draftType = "FILE"
                        draftTarget = ""
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加映射")
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
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = listening,
                    onCheckedChange = { enable ->
                        if (enable) {
                            val ok = HotkeyManager.start()
                            listening = ok
                            message = if (ok) "全局快捷键监听已开启" else "开启失败：无法注册全局钩子（可能被安全软件拦截）"
                        } else {
                            HotkeyManager.stop()
                            listening = false
                            recording = false
                            message = "全局快捷键监听已关闭"
                        }
                    }
                )
                Text(
                    text = if (listening) "全局监听中" else "全局监听已关闭",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { recording = true; message = "请按下要录制的快捷键组合..." },
                    enabled = listening && !recording
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (recording) "请按键..." else "录制")
                }
            }

            Text(
                text = "映射的快捷键在应用运行期间全局生效（系统托盘支持暂不实现）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("已触发") || message.startsWith("已录制") || message.contains("开启"))
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (mappings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击右上角 + 添加快捷键映射",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mappings) { mapping ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = mapping.hotkey,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(140.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mapping.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "[${
                                            when (mapping.type) {
                                                "URL" -> "URL"
                                                "CMD" -> "命令"
                                                else -> "文件"
                                            }
                                        }] ${mapping.target}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingIndex = mappings.indexOf(mapping)
                                        draftName = mapping.name
                                        draftHotkey = mapping.hotkey
                                        draftType = mapping.type
                                        draftTarget = mapping.target
                                        showDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Keyboard,
                                        contentDescription = "编辑",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        mappings.remove(mapping)
                                        save()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                recording = false
            },
            title = { Text(if (editingIndex < 0) "添加快捷键映射" else "编辑快捷键映射") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("名称") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draftHotkey,
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            label = { Text("快捷键") },
                            placeholder = { Text("如 Ctrl+Shift+T") },
                            singleLine = true,
                            readOnly = true
                        )
                        OutlinedButton(
                            onClick = {
                                if (listening) {
                                    recording = true
                                    message = "请按下要录制的快捷键组合..."
                                } else {
                                    message = "请先开启全局监听再录制"
                                }
                            },
                            enabled = listening && !recording
                        ) {
                            Text(if (recording) "请按键..." else "录制")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("FILE" to "文件", "URL" to "URL", "CMD" to "命令").forEach { (type, label) ->
                            FilterChip(
                                selected = draftType == type,
                                onClick = { draftType = type },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draftTarget,
                            onValueChange = { draftTarget = it },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    when (draftType) {
                                        "URL" -> "网址，如 https://example.com"
                                        "CMD" -> "命令，如 notepad"
                                        else -> "程序/文件路径"
                                    }
                                )
                            },
                            singleLine = true
                        )
                        if (draftType == "FILE") {
                            OutlinedButton(onClick = {
                                val chooser = JFileChooser()
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    draftTarget = chooser.selectedFile.absolutePath
                                    if (draftName.isBlank()) draftName = chooser.selectedFile.name
                                }
                            }) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mapping = HotkeyMapping(draftName.trim(), draftHotkey, draftType, draftTarget.trim())
                        if (editingIndex < 0) {
                            mappings.add(mapping)
                        } else {
                            mappings[editingIndex] = mapping
                        }
                        save()
                        showDialog = false
                        recording = false
                    },
                    enabled = draftName.isNotBlank() && draftHotkey.isNotBlank() && draftTarget.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    recording = false
                }) { Text("取消") }
            }
        )
    }
}
