package com.toolbox.ui.productivity.clipboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.data.repository.SettingsRepository
import com.toolbox.ui.image.ImageIo
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject

private const val SETTINGS_KEY = "clipboard_history"

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class ClipItem(
    val id: String,
    val content: String,
    val pinned: Boolean,
    val timestamp: Long
)

private fun convert(op: String, input: String): String = when (op) {
    "upper" -> input.uppercase()
    "lower" -> input.lowercase()
    "title" -> input.split(Regex("\\s+")).joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
    "b64enc" -> Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))
    "b64dec" -> runCatching {
        String(Base64.getDecoder().decode(input.trim()), Charsets.UTF_8)
    }.getOrDefault("【Base64 解码失败】")
    "urlenc" -> URLEncoder.encode(input, "UTF-8")
    "urldec" -> runCatching { URLDecoder.decode(input, "UTF-8") }.getOrDefault(input)
    "jsonesc" -> Json.encodeToString(input).let { it.substring(1, it.length - 1) }
    "jsonuns" -> runCatching { Json.decodeFromString<String>("\"$input\"") }.getOrDefault(input)
    "noblank" -> input.lines().filter { it.isNotBlank() }.joinToString("\n")
    "squeeze" -> input.trim().replace(Regex("\\s+"), " ")
    "reverse" -> input.reversed()
    else -> input
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardToolsScreen(onBack: () -> Unit) {
    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<ClipItem>() }
    var watching by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }

    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }

    fun save() {
        val trimmed = history
            .sortedWith(compareByDescending<ClipItem> { it.pinned }.thenByDescending { it.timestamp })
            .let { list ->
                val pinned = list.filter { it.pinned }
                val others = list.filter { !it.pinned }.take((100 - pinned.size).coerceAtLeast(0))
                pinned + others
            }
        val snapshot = trimmed.toList()
        scope.launch {
            settingsRepository.saveSetting(SETTINGS_KEY, json.encodeToString(snapshot))
        }
    }

    fun rememberContent(content: String) {
        if (content.isBlank()) return
        val existing = history.firstOrNull { it.content == content }
        if (existing != null) {
            history.remove(existing)
            history.add(0, existing.copy(timestamp = System.currentTimeMillis()))
        } else {
            history.add(0, ClipItem(UUID.randomUUID().toString(), content, false, System.currentTimeMillis()))
        }
    }

    LaunchedEffect(Unit) {
        val saved = settingsRepository.getSettingValue(SETTINGS_KEY, "")
        if (saved.isNotBlank()) {
            val loaded = runCatching { json.decodeFromString<List<ClipItem>>(saved) }.getOrDefault(emptyList())
            history.clear()
            history.addAll(loaded)
        }
    }

    LaunchedEffect(watching) {
        var last: String? = null
        while (watching) {
            val content = runCatching {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val contents = clipboard.getContents(null)
                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    contents.getTransferData(DataFlavor.stringFlavor) as? String
                } else {
                    null
                }
            }.getOrNull()
            if (content != null && content != last) {
                last = content
                rememberContent(content)
                save()
            }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("剪贴板工具") },
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
                .padding(12.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("历史记录 (${history.size})") }
                )
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("格式转换") })
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(checked = watching, onCheckedChange = { watching = it })
                    Text(
                        text = if (watching) "监听中" else "已暂停",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("搜索历史...") },
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            history.removeAll { !it.pinned }
                            save()
                            message = "已清空未置顶记录"
                        },
                        enabled = history.any { !it.pinned }
                    ) {
                        Text("清空未置顶")
                    }
                }

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val visible = history
                    .filter { search.isBlank() || it.content.contains(search, ignoreCase = true) }
                    .sortedWith(compareByDescending<ClipItem> { it.pinned }.thenByDescending { it.timestamp })

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(visible, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.content.replace('\n', ' '),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val target = history.first { it.id == item.id }
                                        history.remove(target)
                                        history.add(0, target.copy(pinned = !target.pinned))
                                        save()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = "置顶",
                                        tint = if (item.pinned) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        ImageIo.copyTextToClipboard(item.content)
                                        message = "已复制到剪贴板"
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        history.removeAll { it.id == item.id }
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
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {
                        inputText = runCatching {
                            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                            val contents = clipboard.getContents(null)
                            if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                contents.getTransferData(DataFlavor.stringFlavor) as String
                            } else {
                                ""
                            }
                        }.getOrDefault("")
                        message = "已读取剪贴板"
                    }) {
                        Text("读取剪贴板")
                    }
                    Button(
                        onClick = {
                            rememberContent(outputText)
                            save()
                            ImageIo.copyTextToClipboard(outputText)
                            message = "结果已写入剪贴板并加入历史"
                        },
                        enabled = outputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制结果")
                    }
                    if (message.isNotEmpty()) {
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(220.dp),
                        label = { Text("输入") }
                    )
                    OutlinedTextField(
                        value = outputText,
                        onValueChange = { outputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(220.dp),
                        label = { Text("结果") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val ops = listOf(
                    "upper" to "全大写",
                    "lower" to "全小写",
                    "title" to "首字母大写",
                    "b64enc" to "Base64 编码",
                    "b64dec" to "Base64 解码",
                    "urlenc" to "URL 编码",
                    "urldec" to "URL 解码",
                    "jsonesc" to "JSON 转义",
                    "jsonuns" to "JSON 反转义",
                    "noblank" to "去除空行",
                    "squeeze" to "压缩空白",
                    "reverse" to "反转文本"
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ops) { (op, label) ->
                        OutlinedButton(
                            onClick = { outputText = convert(op, inputText) },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
