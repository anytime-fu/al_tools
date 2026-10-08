package com.toolbox.ui.file.dedup

import com.toolbox.ui.components.appClickable

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import java.security.MessageDigest
import javax.swing.JFileChooser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DupFile(val file: File, val checked: Boolean = true)

private data class DupGroup(
    val hash: String,
    val size: Long,
    val files: List<DupFile>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDedupScreen(onBack: () -> Unit) {
    var dirPath by remember { mutableStateOf("") }
    var recursive by remember { mutableStateOf(true) }
    var groups by remember { mutableStateOf<List<DupGroup>>(emptyList()) }
    var checkedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件去重") },
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
                    label = { Text("扫描目录") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    singleLine = true
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
                        if (scanning) {
                            scanJob?.cancel()
                            return@Button
                        }
                        val root = File(dirPath)
                        if (!root.isDirectory) {
                            message = "目录不存在"
                            return@Button
                        }
                        groups = emptyList()
                        checkedMap = emptyMap()
                        message = ""
                        scanning = true
                        scanned = 0
                        scanJob = scope.launch {
                            var cancelled = false
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    scanDuplicates(root, recursive, { scanned = it }) { isActive }
                                }
                                groups = result
                                val map = mutableMapOf<String, Boolean>()
                                result.forEach { group ->
                                    group.files.drop(1).forEach { map[it.file.absolutePath] = true }
                                }
                                checkedMap = map
                                val dupCount = result.sumOf { it.files.size - 1 }
                                val wasted = result.sumOf { g -> g.size * (g.files.size - 1) }
                                message = "发现 ${result.size} 组重复，共 $dupCount 个多余文件，可释放 ${formatSize(wasted)}"
                            } catch (e: CancellationException) {
                                cancelled = true
                            } finally {
                                scanning = false
                                if (cancelled) message = "已取消扫描"
                            }
                        }
                    },
                    colors = if (scanning) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (scanning) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (scanning) "停止" else "扫描")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Checkbox(checked = recursive, onCheckedChange = { recursive = it })
                Text("包含子目录", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.weight(1f))
                if (scanning) {
                    Text(
                        text = "已扫描 $scanned 个文件...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (groups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        var deleted = 0
                        var failed = 0
                        groups.forEach { group ->
                            group.files.forEach { f ->
                                val key = "${f.file.absolutePath}"
                                if (checkedMap[key] == true) {
                                    if (f.file.delete()) deleted++ else failed++
                                }
                            }
                        }
                        message = "删除完成：$deleted 个文件已删除" + if (failed > 0) "，$failed 个失败" else ""
                        groups = emptyList()
                        checkedMap = emptyMap()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    val toDelete = checkedMap.values.count { it }
                    Text("删除选中的 $toDelete 个文件")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups) { group ->
                    DupGroupCard(
                        group = group,
                        checkedMap = checkedMap,
                        onToggle = { path, checked ->
                            checkedMap = checkedMap + (path to checked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DupGroupCard(
    group: DupGroup,
    checkedMap: Map<String, Boolean>,
    onToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "MD5 ${group.hash.take(16)}… · ${formatSize(group.size)} · ${group.files.size} 个副本",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            group.files.forEachIndexed { index, f ->
                val path = f.file.absolutePath
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .appClickable {
                            if (index > 0) onToggle(path, checkedMap[path] != true)
                        }
                ) {
                    if (index == 0) {
                        Text(
                            text = "保留",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(48.dp)
                        )
                    } else {
                        Checkbox(
                            checked = checkedMap[path] == true,
                            onCheckedChange = { onToggle(path, it) },
                            modifier = Modifier.width(48.dp)
                        )
                    }
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun scanDuplicates(
    root: File,
    recursive: Boolean,
    onScanned: (Int) -> Unit,
    isActive: () -> Boolean
): List<DupGroup> {
    val seq = if (recursive) root.walkTopDown() else root.listFiles()?.asSequence() ?: emptySequence()
    val bySize = mutableMapOf<Long, MutableList<File>>()
    var count = 0
    for (f in seq) {
        if (!isActive()) break
        if (!f.isFile) continue
        count++
        if (count % 20 == 0) onScanned(count)
        bySize.getOrPut(f.length()) { mutableListOf() }.add(f)
    }
    onScanned(count)

    val groups = mutableListOf<DupGroup>()
    for ((size, files) in bySize) {
        if (!isActive()) break
        if (size == 0L || files.size < 2) continue
        val byHash = mutableMapOf<String, MutableList<File>>()
        for (f in files) {
            val hash = md5(f)
            byHash.getOrPut(hash) { mutableListOf() }.add(f)
        }
        for ((hash, same) in byHash) {
            if (same.size >= 2) {
                groups.add(DupGroup(hash, size, same.sortedBy { it.absolutePath }.map { DupFile(it) }))
            }
        }
    }
    return groups.sortedByDescending { it.size * (it.files.size - 1) }
}

private fun md5(file: File): String {
    val digest = MessageDigest.getInstance("MD5")
    file.inputStream().use { input ->
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            digest.update(buf, 0, n)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
