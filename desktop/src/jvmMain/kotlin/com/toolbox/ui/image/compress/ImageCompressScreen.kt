package com.toolbox.ui.image.compress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.ui.image.ImageIo
import java.io.File
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CompressResult(
    val name: String,
    val before: String,
    val after: String,
    val saved: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var quality by remember { mutableStateOf(0.75f) }
    var maxSideText by remember { mutableStateOf("") }
    var outputDirPath by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CompressResult>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片压缩") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.isMultiSelectionEnabled = true
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val selected = (chooser.selectedFiles?.toList() ?: listOfNotNull(chooser.selectedFile))
                                .filter { ImageIo.isImageFile(it) }
                            files = (files + selected).distinctBy { it.absolutePath }
                            message = ""
                        }
                    },
                    enabled = !processing
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加图片")
                }
                OutlinedButton(onClick = { files = emptyList(); results = emptyList() }, enabled = !processing) {
                    Text("清空列表")
                }
                Text(
                    text = "已选 ${files.size} 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("质量: ${"%.0f".format(quality * 100)}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 0.1f..1f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "质量参数对 JPG/WebP 生效，PNG/BMP 仅支持尺寸压缩",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = maxSideText,
                            onValueChange = { input -> maxSideText = input.filter { it.isDigit() } },
                            modifier = Modifier.width(180.dp),
                            label = { Text("最长边限制(px)") },
                            placeholder = { Text("0 = 不限制") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = outputDirPath,
                            onValueChange = { outputDirPath = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("输出目录（留空则输出到源文件目录）") },
                            singleLine = true
                        )
                        OutlinedButton(
                            onClick = {
                                val chooser = JFileChooser()
                                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    outputDirPath = chooser.selectedFile.absolutePath
                                }
                            }
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("浏览")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val maxSide = maxSideText.toIntOrNull() ?: 0
                        val outDir = if (outputDirPath.isBlank()) null else File(outputDirPath)
                        processing = true
                        results = emptyList()
                        message = ""
                        scope.launch {
                            val done = mutableListOf<CompressResult>()
                            files.forEachIndexed { index, file ->
                                progressText = "处理中 ${index + 1}/${files.size}: ${file.name}"
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val image = ImageIo.read(file) ?: error("无法读取图片")
                                        val resized = ImageIo.resizeMaxSide(image, maxSide)
                                        val format = ImageIo.formatOf(file)
                                        val dir = outDir ?: file.parentFile
                                        dir.mkdirs()
                                        val target = ImageIo.uniqueTarget(dir, file.nameWithoutExtension + "_compressed", format)
                                        val qualityParam = if (format == "jpg" || format == "webp") quality else null
                                        ImageIo.write(resized, target, format, qualityParam)
                                        CompressResult(
                                            name = file.name,
                                            before = ImageIo.formatSize(file.length()),
                                            after = ImageIo.formatSize(target.length()),
                                            saved = if (file.length() > 0) {
                                                "%.1f%%".format(100.0 - target.length() * 100.0 / file.length())
                                            } else "0%"
                                        )
                                    }
                                }
                                result.onSuccess { done.add(it) }
                                    .onFailure { message = "处理失败: ${file.name} - ${it.message}" }
                                results = done.toList()
                            }
                            progressText = ""
                            processing = false
                            message = if (message.isEmpty()) "压缩完成，共 ${done.size} 张" else message
                        }
                    },
                    enabled = !processing && files.isNotEmpty()
                ) {
                    Text(if (processing) "处理中..." else "开始压缩")
                }
                if (processing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(progressText, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("处理失败")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "原文件",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "压缩结果",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(files) { file ->
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
                                text = file.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ImageIo.formatSize(file.length()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val result = results.firstOrNull { it.name == file.name }
                            if (result != null) {
                                Text(
                                    text = "→ ${result.after} (省 ${result.saved})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!processing) {
                                IconButton(
                                    onClick = { files = files - file },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "移除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
