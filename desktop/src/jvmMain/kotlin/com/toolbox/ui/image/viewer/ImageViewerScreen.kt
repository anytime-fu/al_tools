package com.toolbox.ui.image.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.ui.image.ImageDropBridge
import com.toolbox.ui.image.ImageIo
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ImageViewerScreen(onBack: () -> Unit) {
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(-1) }
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var zoom by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var message by remember { mutableStateOf("") }

    fun loadAt(index: Int) {
        if (files.isEmpty() || index !in files.indices) return
        currentIndex = index
        zoom = 1f
        rotation = 0f
        val file = files[index]
        bitmap = runCatching { file.inputStream().use { loadImageBitmap(it) } }.getOrNull()
        message = if (bitmap == null) "无法读取图片: ${file.name}" else ""
    }

    fun openFiles(list: List<File>) {
        val images = list.filter { ImageIo.isImageFile(it) }.sortedBy { it.name.lowercase() }
        if (images.isEmpty()) {
            message = "未找到可用图片文件"
            return
        }
        files = images
        loadAt(0)
    }

    val dropped by ImageDropBridge.dropped.collectAsState()
    LaunchedEffect(dropped) {
        if (dropped.isNotEmpty()) {
            openFiles(dropped)
            ImageDropBridge.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片查看器") },
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
                Button(onClick = {
                    val chooser = JFileChooser()
                    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                    chooser.isMultiSelectionEnabled = true
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        val selected = chooser.selectedFiles?.toList()
                            ?: listOfNotNull(chooser.selectedFile)
                        openFiles(selected)
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("打开图片")
                }
                OutlinedButton(onClick = { loadAt(currentIndex - 1) }, enabled = currentIndex > 0) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("上一张")
                }
                OutlinedButton(
                    onClick = { loadAt(currentIndex + 1) },
                    enabled = currentIndex in 0 until files.size - 1
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("下一张")
                }
                IconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(8f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "放大")
                }
                IconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(0.2f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "缩小")
                }
                IconButton(onClick = { zoom = 1f }) {
                    Icon(Icons.Default.Refresh, contentDescription = "适应窗口")
                }
                IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                    Icon(Icons.Default.Rotate90DegreesCw, contentDescription = "旋转")
                }
                IconButton(onClick = {
                    val file = files.getOrNull(currentIndex) ?: return@IconButton
                    runCatching { Desktop.getDesktop().open(file) }
                }, enabled = currentIndex >= 0) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "系统打开")
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        zoom = if (delta < 0f) {
                            (zoom * 1.15f).coerceAtMost(8f)
                        } else {
                            (zoom / 1.15f).coerceAtLeast(0.2f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val current = bitmap
                if (current == null) {
                    Text(
                        text = "拖入图片文件或点击「打开图片」开始浏览",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Image(
                        bitmap = current,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                rotationZ = rotation
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val file = files.getOrNull(currentIndex)
                Text(
                    text = if (file == null) "未打开图片"
                    else "${file.name}    ${bitmap?.width ?: 0} × ${bitmap?.height ?: 0}    ${
                        file.length().let { ImageIo.formatSize(it) }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (files.isEmpty()) "" else "${currentIndex + 1} / ${files.size}    缩放 ${"%.0f".format(zoom * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
