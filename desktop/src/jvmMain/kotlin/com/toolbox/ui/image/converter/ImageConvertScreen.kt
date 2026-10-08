package com.toolbox.ui.image.converter

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

private data class ConvertResult(
    val name: String,
    val output: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageConvertScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var targetFormat by remember { mutableStateOf("jpg") }
    var outputDirPath by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ConvertResult>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    val formats = listOf("jpg", "png", "bmp", "webp").filter { ImageIo.canWrite(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("格式转换") },
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
                    Text("目标格式", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { format ->
                            FilterChip(
                                selected = targetFormat == format,
                                onClick = { targetFormat = format },
                                label = { Text(format.uppercase()) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        val outDir = if (outputDirPath.isBlank()) null else File(outputDirPath)
                        processing = true
                        results = emptyList()
                        message = ""
                        scope.launch {
                            val done = mutableListOf<ConvertResult>()
                            files.forEachIndexed { index, file ->
                                progressText = "转换中 ${index + 1}/${files.size}: ${file.name}"
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val image = ImageIo.read(file) ?: error("无法读取图片")
                                        val dir = outDir ?: file.parentFile
                                        dir.mkdirs()
                                        val target = ImageIo.uniqueTarget(dir, file.nameWithoutExtension, targetFormat)
                                        val quality = if (targetFormat == "jpg" || targetFormat == "webp") 0.9f else null
                                        ImageIo.write(image, target, targetFormat, quality)
                                        ConvertResult(name = file.name, output = target.name)
                                    }
                                }
                                result.onSuccess { done.add(it) }
                                    .onFailure { message = "转换失败: ${file.name} - ${it.message}" }
                                results = done.toList()
                            }
                            progressText = ""
                            processing = false
                            message = if (message.isEmpty()) "转换完成，共 ${done.size} 张 → ${targetFormat.uppercase()}" else message
                        }
                    },
                    enabled = !processing && files.isNotEmpty()
                ) {
                    Text(if (processing) "转换中..." else "开始转换")
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
                    color = if (message.startsWith("转换失败")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                                text = ImageIo.formatOf(file).uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val result = results.firstOrNull { it.name == file.name }
                            if (result != null) {
                                Text(
                                    text = "→ ${result.output}",
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
