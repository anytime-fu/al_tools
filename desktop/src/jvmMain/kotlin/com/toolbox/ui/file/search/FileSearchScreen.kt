package com.toolbox.ui.file.search

import com.toolbox.ui.components.appClickable

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser

private data class FileResult(
    val file: File,
    val matchedInContent: Boolean
)

private val JUNK_DIRS = setOf(
    ".git", ".svn", ".hg", ".gradle", ".idea", ".vscode", ".m2", ".npm", ".cache",
    "node_modules", "build", "out", "target", "dist", "__pycache__", ".venv", "venv"
)

private const val MAX_CONTENT_SIZE = 5L * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSearchScreen(onBack: () -> Unit) {
    var dirPath by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var searchContent by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var recursive by remember { mutableStateOf(true) }
    var skipJunkDirs by remember { mutableStateOf(true) }
    val results = remember { mutableStateListOf<FileResult>() }
    var searching by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableStateOf(0) }
    var currentFile by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件搜索") },
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
                    label = { Text("搜索目录") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("搜索关键词") },
                    placeholder = { Text(if (useRegex) "正则表达式" else "文件名或内容关键词") }
                )

                Button(
                    onClick = {
                        if (searching) {
                            searchJob?.cancel()
                            return@Button
                        }
                        if (dirPath.isBlank() || keyword.isBlank()) {
                            message = "请选择目录并输入关键词"
                            return@Button
                        }
                        val root = File(dirPath)
                        if (!root.isDirectory) {
                            message = "目录不存在"
                            return@Button
                        }
                        val regex = try {
                            if (useRegex) Regex(keyword) else null
                        } catch (e: Exception) {
                            message = "正则表达式无效：${e.message}"
                            return@Button
                        }
                        results.clear()
                        scannedCount = 0
                        currentFile = ""
                        message = ""
                        searching = true
                        searchJob = scope.launch {
                            val needle = keyword
                            val matchContent = searchContent
                            val recursiveSearch = recursive
                            val skipJunk = skipJunkDirs
                            var scanned = 0
                            var cancelled = false
                            try {
                                withContext(Dispatchers.IO) {
                                    val walk: Sequence<File> = if (recursiveSearch) {
                                        root.walkTopDown().onEnter { dir ->
                                            !skipJunk || dir == root || !isJunkDir(dir)
                                        }
                                    } else {
                                        root.listFiles()?.asSequence() ?: emptySequence()
                                    }
                                    var lastPaint = 0L
                                    for (f in walk) {
                                        if (!isActive) break
                                        if (!f.isFile) continue
                                        scanned++
                                        val now = System.currentTimeMillis()
                                        if (now - lastPaint > 100) {
                                            lastPaint = now
                                            scannedCount = scanned
                                            currentFile = f.name
                                        }
                                        val nameMatched = if (regex != null) {
                                            regex.containsMatchIn(f.name)
                                        } else {
                                            f.name.contains(needle, ignoreCase = true)
                                        }
                                        if (nameMatched) {
                                            results.add(FileResult(f, matchedInContent = false))
                                        } else if (matchContent && contentMatches(f, needle, regex)) {
                                            results.add(FileResult(f, matchedInContent = true))
                                        }
                                    }
                                }
                            } catch (e: CancellationException) {
                                cancelled = true
                            } finally {
                                scannedCount = scanned
                                currentFile = ""
                                searching = false
                                message = when {
                                    cancelled -> "已取消：已找到 ${results.size} 个结果（扫描 $scanned 个文件）"
                                    results.isEmpty() -> "搜索完成：未找到匹配文件（扫描 $scanned 个文件）"
                                    else -> "搜索完成：找到 ${results.size} 个文件（扫描 $scanned 个）"
                                }
                            }
                        }
                    },
                    colors = if (searching) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (searching) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (searching) "停止" else "搜索")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recursive, onCheckedChange = { recursive = it })
                    Text("包含子目录", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = searchContent, onCheckedChange = { searchContent = it })
                    Text("搜索文件内容", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useRegex, onCheckedChange = { useRegex = it })
                    Text("正则匹配", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = skipJunkDirs, onCheckedChange = { skipJunkDirs = it })
                    Text("跳过构建/隐藏目录", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (searching) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("已扫描 $scannedCount 个文件 · 已找到 ${results.size} 个")
                        if (currentFile.isNotEmpty()) append(" · 正在搜索：$currentFile")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(results) { result ->
                    FileResultRow(result)
                }
            }
        }
    }
}

@Composable
private fun FileResultRow(result: FileResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .appClickable {
                try {
                    Desktop.getDesktop().open(result.file)
                } catch (_: Exception) {
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = result.file.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatSize(result.file.length()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (result.matchedInContent) {
                Text(
                    text = "内容匹配",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(
                onClick = { openContainingFolder(result.file) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "打开所在文件夹",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun openContainingFolder(file: File) {
    val desktop = runCatching { Desktop.getDesktop() }.getOrNull()
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
        val ok = runCatching { desktop.browseFileDirectory(file) }.isSuccess
        if (ok) return
    }
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val opened = if (os.contains("win")) {
        runCatching { ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start() }.isSuccess
    } else {
        val dir = file.parentFile
        dir != null && desktop != null && runCatching { desktop.open(dir) }.isSuccess
    }
    if (!opened && file.parentFile != null && desktop != null && desktop.isSupported(Desktop.Action.OPEN)) {
        runCatching { desktop.open(file.parentFile) }
    }
}

private fun isJunkDir(dir: File): Boolean {
    val name = dir.name
    return name in JUNK_DIRS || (name.startsWith(".") && name != "." && name != "..")
}

private fun contentMatches(file: File, keyword: String, regex: Regex?): Boolean {
    if (file.length() > MAX_CONTENT_SIZE) return false
    if (looksBinary(file)) return false
    return try {
        file.bufferedReader(Charsets.UTF_8).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (regex != null) {
                    if (regex.containsMatchIn(line)) return true
                } else {
                    if (line.contains(keyword, ignoreCase = true)) return true
                }
                line = reader.readLine()
            }
        }
        false
    } catch (_: Exception) {
        false
    }
}

private fun looksBinary(file: File): Boolean {
    return try {
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            val n = input.read(buf)
            for (i in 0 until n) {
                if (buf[i] == 0.toByte()) return true
            }
        }
        false
    } catch (_: Exception) {
        true
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
