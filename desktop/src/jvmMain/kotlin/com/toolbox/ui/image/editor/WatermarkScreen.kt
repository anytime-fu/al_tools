package com.toolbox.ui.image.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.ui.image.ImageDropBridge
import com.toolbox.ui.image.ImageIo
import java.awt.AlphaComposite
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class WmType { TEXT, IMAGE }

private enum class WmPosition(val label: String) {
    TILE("平铺"), CENTER("居中"), TOP_LEFT("左上"), TOP_RIGHT("右上"), BOTTOM_LEFT("左下"), BOTTOM_RIGHT("右下")
}

private data class WmResult(val name: String, val output: String)

private val presetColors = listOf(
    "白色" to Color.White,
    "黑色" to Color.Black,
    "红色" to Color(0xFFEF4444),
    "黄色" to Color(0xFFFBBF24),
    "绿色" to Color(0xFF22C55E),
    "青色" to Color(0xFF22D3EE),
    "紫色" to Color(0xFFA78BFA)
)

private fun applyTextWatermark(
    src: BufferedImage,
    text: String,
    fontPx: Int,
    color: Color,
    alpha: Float,
    position: WmPosition
): BufferedImage {
    val out = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB)
    val g = out.createGraphics()
    g.drawImage(src, 0, 0, null)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.font = Font(Font.SANS_SERIF, Font.BOLD, fontPx)
    val awtColor = java.awt.Color(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        (alpha.coerceIn(0f, 1f) * 255).toInt()
    )
    g.color = awtColor
    val fm = g.fontMetrics
    val tw = fm.stringWidth(text)
    when (position) {
        WmPosition.TILE -> {
            val stepX = tw + fontPx * 2
            val stepY = fm.height * 3
            var y = fontPx * 2
            while (y < out.height + stepY) {
                var x = fontPx
                while (x < out.width + stepX) {
                    g.drawString(text, x, y)
                    x += stepX
                }
                y += stepY
            }
        }
        WmPosition.CENTER -> g.drawString(text, (out.width - tw) / 2, (out.height + fm.ascent) / 2)
        WmPosition.TOP_LEFT -> g.drawString(text, fontPx / 2, fontPx)
        WmPosition.TOP_RIGHT -> g.drawString(text, out.width - tw - fontPx / 2, fontPx)
        WmPosition.BOTTOM_LEFT -> g.drawString(text, fontPx / 2, out.height - fontPx / 2)
        WmPosition.BOTTOM_RIGHT -> g.drawString(text, out.width - tw - fontPx / 2, out.height - fontPx / 2)
    }
    g.dispose()
    return out
}

private fun applyImageWatermark(
    src: BufferedImage,
    wm: BufferedImage,
    alpha: Float,
    position: WmPosition
): BufferedImage {
    val out = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB)
    val g = out.createGraphics()
    g.drawImage(src, 0, 0, null)
    var w = wm.width
    var h = wm.height
    val maxW = src.width / 3
    val maxH = src.height / 3
    if (w > maxW || h > maxH) {
        val scale = minOf(maxW.toFloat() / w, maxH.toFloat() / h)
        w = (w * scale).toInt().coerceAtLeast(1)
        h = (h * scale).toInt().coerceAtLeast(1)
    }
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.coerceIn(0f, 1f))
    when (position) {
        WmPosition.TILE -> {
            val stepX = w * 2
            val stepY = h * 2
            var y = h / 2
            while (y < out.height + stepY) {
                var x = w / 2
                while (x < out.width + stepX) {
                    g.drawImage(wm, x, y, w, h, null)
                    x += stepX
                }
                y += stepY
            }
        }
        WmPosition.CENTER -> g.drawImage(wm, (out.width - w) / 2, (out.height - h) / 2, w, h, null)
        WmPosition.TOP_LEFT -> g.drawImage(wm, w / 2, h / 2, w, h, null)
        WmPosition.TOP_RIGHT -> g.drawImage(wm, out.width - w - w / 2, h / 2, w, h, null)
        WmPosition.BOTTOM_LEFT -> g.drawImage(wm, w / 2, out.height - h - h / 2, w, h, null)
        WmPosition.BOTTOM_RIGHT -> g.drawImage(wm, out.width - w - w / 2, out.height - h - h / 2, w, h, null)
    }
    g.dispose()
    return out
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var wmType by remember { mutableStateOf(WmType.TEXT) }
    var wmText by remember { mutableStateOf("水印文字") }
    var fontSize by remember { mutableStateOf(48f) }
    var textColor by remember { mutableStateOf(presetColors.first().second) }
    var opacity by remember { mutableStateOf(0.5f) }
    var wmImageFile by remember { mutableStateOf<File?>(null) }
    var wmImage by remember { mutableStateOf<BufferedImage?>(null) }
    var position by remember { mutableStateOf(WmPosition.BOTTOM_RIGHT) }
    var outputDirPath by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WmResult>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(files, wmType, wmText, fontSize, textColor, opacity, wmImage, position) {
        val first = files.firstOrNull()
        if (first == null) {
            preview = null
            return@LaunchedEffect
        }
        preview = withContext(Dispatchers.IO) {
            runCatching {
                val src = ImageIo.read(first) ?: error("无法读取图片")
                val marked = if (wmType == WmType.TEXT) {
                    applyTextWatermark(src, wmText, fontSize.toInt(), textColor, opacity, position)
                } else {
                    val wm = wmImage ?: error("请选择水印图片")
                    applyImageWatermark(src, wm, opacity, position)
                }
                ImageIo.toImageBitmap(marked)
            }.getOrNull()
        }
    }

    val dropped by ImageDropBridge.dropped.collectAsState()
    LaunchedEffect(dropped) {
        if (dropped.isNotEmpty()) {
            files = (files + dropped.filter { ImageIo.isImageFile(it) }).distinctBy { it.absolutePath }
            ImageDropBridge.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水印工具") },
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                Row(
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
                            }
                        },
                        enabled = !processing
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加图片")
                    }
                    OutlinedButton(onClick = { files = emptyList() }, enabled = !processing) {
                        Text("清空")
                    }
                    Text(
                        text = "已选 ${files.size} 张",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = wmType == WmType.TEXT,
                        onClick = { wmType = WmType.TEXT },
                        label = { Text("文字水印") }
                    )
                    FilterChip(
                        selected = wmType == WmType.IMAGE,
                        onClick = { wmType = WmType.IMAGE },
                        label = { Text("图片水印") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (wmType == WmType.TEXT) {
                    OutlinedTextField(
                        value = wmText,
                        onValueChange = { wmText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("水印文字") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("字号: ${fontSize.toInt()}px", style = MaterialTheme.typography.bodySmall)
                    Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 12f..200f)
                    Text("颜色", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetColors.forEach { (_, color) ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color)
                                    .clickable { textColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (textColor == color) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (color == Color.White) Color.Black else Color.White
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            val chooser = JFileChooser()
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                wmImageFile = chooser.selectedFile
                                wmImage = runCatching { ImageIo.read(chooser.selectedFile) }.getOrNull()
                            }
                        }) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("选择水印图片")
                        }
                        Text(
                            text = wmImageFile?.name ?: "未选择",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("不透明度: ${"%.0f".format(opacity * 100)}%", style = MaterialTheme.typography.bodySmall)
                Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.05f..1f)

                Text("位置", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WmPosition.values().forEach { pos ->
                        FilterChip(
                            selected = position == pos,
                            onClick = { position = pos },
                            label = { Text(pos.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
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
                    OutlinedButton(onClick = {
                        val chooser = JFileChooser()
                        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            outputDirPath = chooser.selectedFile.absolutePath
                        }
                    }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("浏览")
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
                                val done = mutableListOf<WmResult>()
                                files.forEachIndexed { index, file ->
                                    progressText = "添加水印 ${index + 1}/${files.size}: ${file.name}"
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val src = ImageIo.read(file) ?: error("无法读取图片")
                                            val marked = if (wmType == WmType.TEXT) {
                                                applyTextWatermark(src, wmText, fontSize.toInt(), textColor, opacity, position)
                                            } else {
                                                val wm = wmImage ?: error("请选择水印图片")
                                                applyImageWatermark(src, wm, opacity, position)
                                            }
                                            val dir = outDir ?: file.parentFile
                                            dir.mkdirs()
                                            val format = ImageIo.formatOf(file)
                                            val target = ImageIo.uniqueTarget(dir, file.nameWithoutExtension + "_wm", format)
                                            ImageIo.write(marked, target, format)
                                            WmResult(name = file.name, output = target.name)
                                        }
                                    }
                                    result.onSuccess { done.add(it) }
                                        .onFailure { message = "处理失败: ${file.name} - ${it.message}" }
                                    results = done.toList()
                                }
                                progressText = ""
                                processing = false
                                message = if (message.isEmpty()) "水印添加完成，共 ${done.size} 张" else message
                            }
                        },
                        enabled = !processing && files.isNotEmpty() &&
                            (wmType == WmType.TEXT && wmText.isNotBlank() || wmType == WmType.IMAGE && wmImage != null)
                    ) {
                        Text(if (processing) "处理中..." else "批量添加水印")
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
                        color = if (message.startsWith("水印添加完成")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text("预览（第一张）", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                val previewBitmap = preview
                if (previewBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "添加图片后显示水印预览",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    androidx.compose.foundation.Image(
                        bitmap = previewBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}
