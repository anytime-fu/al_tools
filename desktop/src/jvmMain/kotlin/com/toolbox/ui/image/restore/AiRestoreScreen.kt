package com.toolbox.ui.image.restore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.AppFilterChip
import com.toolbox.ui.image.ImageDropBridge
import com.toolbox.ui.image.ImageIo
import com.toolbox.ui.image.ZoomViewport
import com.toolbox.util.MosaicDetector
import com.toolbox.util.onnx.AiInpainter
import com.toolbox.util.onnx.MaskOps
import com.toolbox.util.onnx.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JFileChooser

private enum class RestoreTool { RECT, BRUSH, ERASER, PAN }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AiRestoreScreen(onBack: () -> Unit, mosaicPreset: Boolean = false) {
    var source by remember { mutableStateOf<BufferedImage?>(null) }
    var sourceFile by remember { mutableStateOf<File?>(null) }
    var mask by remember { mutableStateOf<BufferedImage?>(null) }
    var overlay by remember { mutableStateOf<BufferedImage?>(null) }
    var overlayImg by remember { mutableStateOf<ImageBitmap?>(null) }
    var result by remember { mutableStateOf<BufferedImage?>(null) }
    var displayImg by remember { mutableStateOf<ImageBitmap?>(null) }

    var tool by remember { mutableStateOf(if (mosaicPreset) RestoreTool.BRUSH else RestoreTool.RECT) }
    var mosaicMode by remember { mutableStateOf(mosaicPreset) }
    var brushSize by remember { mutableStateOf(28f) }
    var showResult by remember { mutableStateOf(false) }
    var rectPreview by remember { mutableStateOf<Rect?>(null) }
    var rectAnchor by remember { mutableStateOf<Offset?>(null) }
    var message by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var maskVersion by remember { mutableStateOf(0) }
    var modelReady by remember { mutableStateOf(ModelManager.isModelReady()) }

    val scope = rememberCoroutineScope()
    val view = remember { ZoomViewport() }

    fun refreshDisplay() {
        val img = if (showResult) result else source
        displayImg = img?.let { ImageIo.toImageBitmap(it) }
    }

    fun rebuildOverlay() {
        overlayImg = overlay?.let { ImageIo.toImageBitmap(it) }
    }

    fun openImage(file: File) {
        val img = runCatching { ImageIo.read(file) }.getOrNull()
        if (img == null) {
            message = "无法读取图片: ${file.name}"
            return
        }
        val rgb = ImageIo.flattenToRgb(img)
        source = rgb
        sourceFile = file
        mask = MaskOps.create(rgb.width, rgb.height)
        overlay = BufferedImage(rgb.width, rgb.height, BufferedImage.TYPE_INT_ARGB)
        result = null
        showResult = false
        rectPreview = null
        rebuildOverlay()
        refreshDisplay()
        message = ""
    }

    fun paintStamp(imgX: Int, imgY: Int, radius: Int, erase: Boolean) {
        val m = mask ?: return
        val o = overlay ?: return
        if (erase) {
            MaskOps.stampCircle(m, imgX, imgY, radius, 0)
            val g = o.createGraphics()
            g.composite = AlphaComposite.Clear
            g.color = Color(0, 0, 0, 0)
            g.fillOval(imgX - radius, imgY - radius, radius * 2, radius * 2)
            g.dispose()
        } else {
            MaskOps.stampCircle(m, imgX, imgY, radius, 255)
            val g = o.createGraphics()
            g.composite = AlphaComposite.SrcOver
            g.color = Color(255, 70, 70, 110)
            g.fillOval(imgX - radius, imgY - radius, radius * 2, radius * 2)
            g.dispose()
        }
        maskVersion++
    }

    fun commitRect(rect: Rect) {
        val m = mask ?: return
        val o = overlay ?: return
        val x = rect.left.toInt().coerceIn(0, m.width - 1)
        val y = rect.top.toInt().coerceIn(0, m.height - 1)
        val w = rect.width.toInt().coerceAtMost(m.width - x)
        val h = rect.height.toInt().coerceAtMost(m.height - y)
        if (w <= 0 || h <= 0) return
        MaskOps.fillRect(m, x, y, w, h, 255)
        val g = o.createGraphics()
        g.composite = AlphaComposite.SrcOver
        g.color = Color(255, 70, 70, 110)
        g.fillRect(x, y, w, h)
        g.dispose()
        maskVersion++
    }

    val dropped by ImageDropBridge.dropped.collectAsState()
    LaunchedEffect(dropped) {
        if (dropped.isNotEmpty()) {
            openImage(dropped.first())
            ImageDropBridge.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mosaicMode) "AI 打码修复" else "AI 去水印") },
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
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            openImage(chooser.selectedFile)
                        }
                    }
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("打开图片")
                }
                AppFilterChip(
                    selected = !mosaicMode,
                    onClick = { mosaicMode = false },
                    label = { Text("去水印") }
                )
                AppFilterChip(
                    selected = mosaicMode,
                    onClick = { mosaicMode = true },
                    label = { Text("打码修复") }
                )
                if (mosaicMode) {
                    OutlinedButton(
                        onClick = {
                            val img = source
                            val m = mask
                            if (img == null || m == null) return@OutlinedButton
                            busy = true
                            scope.launch(Dispatchers.Default) {
                                try {
                                    val regions = MosaicDetector.detect(img) { stage = it }
                                    if (regions.isEmpty()) {
                                        message = "未检测到明显马赛克区域，可手动涂抹标记"
                                    } else {
                                        MaskOps.clear(m)
                                        val o = overlay ?: return@launch
                                        val g = o.createGraphics()
                                        g.composite = AlphaComposite.Clear
                                        g.fillRect(0, 0, o.width, o.height)
                                        g.dispose()
                                        regions.forEach { r ->
                                            MaskOps.fillRect(m, r.x, r.y, r.width, r.height, 255)
                                            val og = o.createGraphics()
                                            og.composite = AlphaComposite.SrcOver
                                            og.color = Color(255, 70, 70, 110)
                                            og.fillRect(r.x, r.y, r.width, r.height)
                                            og.dispose()
                                        }
                                        maskVersion++
                                        message = "检测到 ${regions.size} 个疑似马赛克区域，请核对后修复"
                                    }
                                } catch (e: Exception) {
                                    message = "检测失败: ${e.message}"
                                } finally {
                                    busy = false
                                    stage = ""
                                }
                            }
                        },
                        enabled = source != null && !busy
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("自动检测")
                    }
                }
                OutlinedButton(
                    onClick = {
                        val m = mask ?: return@OutlinedButton
                        MaskOps.clear(m)
                        val o = overlay ?: return@OutlinedButton
                        val g = o.createGraphics()
                        g.composite = AlphaComposite.Clear
                        g.fillRect(0, 0, o.width, o.height)
                        g.dispose()
                        rectPreview = null
                        maskVersion++
                    },
                    enabled = source != null && !busy
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空标记")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RestoreTool.RECT to Icons.Default.Crop,
                    RestoreTool.BRUSH to Icons.Default.Brush,
                    RestoreTool.ERASER to Icons.Default.CenterFocusStrong,
                    RestoreTool.PAN to Icons.Default.PanTool
                ).forEach { (t, icon) ->
                    OutlinedButton(
                        onClick = { tool = t },
                        enabled = source != null
                    ) {
                        Icon(
                            icon,
                            contentDescription = t.name,
                            tint = if (tool == t) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "笔刷",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = brushSize,
                    onValueChange = { brushSize = it },
                    valueRange = 6f..128f,
                    modifier = Modifier.width(140.dp)
                )
                OutlinedButton(
                    onClick = { showResult = !showResult; refreshDisplay() },
                    enabled = result != null
                ) {
                    Text(text = if (showResult) "看原图" else "看结果")
                }
                OutlinedButton(
                    onClick = {
                        val img = result ?: return@OutlinedButton
                        ImageIo.copyImageToClipboard(img)
                        message = "结果已复制到剪贴板"
                    },
                    enabled = result != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }
                OutlinedButton(
                    onClick = {
                        val img = result ?: return@OutlinedButton
                        val chooser = JFileChooser()
                        chooser.selectedFile = File(sourceFile?.parentFile, "restored.png")
                        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val target = chooser.selectedFile
                            val fmt = ImageIo.formatOf(target)
                            runCatching { ImageIo.write(img, target, fmt) }
                                .onSuccess { message = "已保存: ${target.name}" }
                                .onFailure { message = "保存失败: ${it.message}" }
                        }
                    },
                    enabled = result != null
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val img = source
            val display = displayImg
            val overlayBmp = overlayImg
            if (img == null || display == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "打开或拖入图片，标记水印/码区后点击修复\n修复结果为 AI 推测生成内容，非真实还原",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { boxSize = it }
                        .onPointerEvent(PointerEventType.Scroll) { event ->
                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (delta != 0f) {
                                val anchor = event.changes.firstOrNull()?.position
                                view.zoomBy(
                                    if (delta > 0) 1.1f else 1f / 1.1f,
                                    anchor,
                                    boxSize.width.toFloat(), boxSize.height.toFloat(),
                                    img.width, img.height
                                )
                            }
                        }
                        .pointerInput(img, tool, boxSize) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val layout = view.layout(
                                        boxSize.width.toFloat(), boxSize.height.toFloat(), img.width, img.height
                                    )
                                    val imgX = layout.toImageX(offset.x)
                                    val imgY = layout.toImageY(offset.y)
                                    when (tool) {
                                        RestoreTool.PAN -> Unit
                                        RestoreTool.RECT -> {
                                            rectAnchor = Offset(imgX, imgY)
                                            rectPreview = Rect(imgX, imgY, imgX, imgY)
                                        }
                                        RestoreTool.BRUSH -> paintStamp(
                                            imgX.toInt(), imgY.toInt(),
                                            (brushSize / 2f / layout.scale).toInt().coerceAtLeast(1), false
                                        )
                                        RestoreTool.ERASER -> paintStamp(
                                            imgX.toInt(), imgY.toInt(),
                                            (brushSize / 2f / layout.scale).toInt().coerceAtLeast(1), true
                                        )
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val layout = view.layout(
                                        boxSize.width.toFloat(), boxSize.height.toFloat(), img.width, img.height
                                    )
                                    when (tool) {
                                        RestoreTool.PAN -> view.panBy(
                                            dragAmount.x, dragAmount.y,
                                            boxSize.width.toFloat(), boxSize.height.toFloat(), img.width, img.height
                                        )
                                        RestoreTool.RECT -> {
                                            val anchor = rectAnchor
                                            if (anchor != null) {
                                                val endX = layout.toImageX(change.position.x)
                                                val endY = layout.toImageY(change.position.y)
                                                rectPreview = Rect(
                                                    minOf(anchor.x, endX), minOf(anchor.y, endY),
                                                    maxOf(anchor.x, endX), maxOf(anchor.y, endY)
                                                )
                                            }
                                        }
                                        RestoreTool.BRUSH -> paintStamp(
                                            layout.toImageX(change.position.x).toInt(),
                                            layout.toImageY(change.position.y).toInt(),
                                            (brushSize / 2f / layout.scale).toInt().coerceAtLeast(1), false
                                        )
                                        RestoreTool.ERASER -> paintStamp(
                                            layout.toImageX(change.position.x).toInt(),
                                            layout.toImageY(change.position.y).toInt(),
                                            (brushSize / 2f / layout.scale).toInt().coerceAtLeast(1), true
                                        )
                                    }
                                },
                                onDragEnd = {
                                    val preview = rectPreview
                                    if (preview != null) {
                                        commitRect(preview)
                                        rectPreview = null
                                        rectAnchor = null
                                    }
                                    rebuildOverlay()
                                }
                            )
                        }
                ) {
                    val layout = view.layout(size.width, size.height, img.width, img.height)
                    val dispW = (img.width * layout.scale).toInt().coerceAtLeast(1)
                    val dispH = (img.height * layout.scale).toInt().coerceAtLeast(1)
                    val dstOffset = IntOffset(layout.offsetX.toInt(), layout.offsetY.toInt())
                    val dstSize = IntSize(dispW, dispH)
                    drawImage(
                        display,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(img.width, img.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize
                    )
                    if (!showResult && overlayBmp != null) {
                        drawImage(
                            overlayBmp,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(img.width, img.height),
                            dstOffset = dstOffset,
                            dstSize = dstSize
                        )
                    }
                    val preview = rectPreview
                    if (preview != null) {
                        val topLeft = Offset(layout.toDisplayX(preview.left), layout.toDisplayY(preview.top))
                        val sizeRect = androidx.compose.ui.geometry.Size(
                            preview.width * layout.scale,
                            preview.height * layout.scale
                        )
                        drawRect(
                            color = androidx.compose.ui.graphics.Color(1f, 0.27f, 0.27f, 0.8f),
                            topLeft = topLeft,
                            size = sizeRect,
                            style = Stroke(width = 2f)
                        )
                        drawRect(
                            color = androidx.compose.ui.graphics.Color(1f, 0.27f, 0.27f, 0.18f),
                            topLeft = topLeft,
                            size = sizeRect
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val s = source
                                val m = mask
                                if (s == null || m == null) return@Button
                                if (MaskOps.coverage(m) == 0) {
                                    message = "请先标记要修复的区域"
                                    return@Button
                                }
                                val modelPath = ModelManager.resolveModel()
                                if (modelPath == null) {
                                    message = "模型不可用：请下载或导入 ONNX 模型"
                                    return@Button
                                }
                                busy = true
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        val res = AiInpainter.inpaint(
                                            s, m, modelPath.absolutePath
                                        ) { stage = it }
                                        result = res
                                        showResult = true
                                        message = "修复完成（推理引擎: ${AiInpainter.providerLabel}）"
                                    } catch (e: Exception) {
                                        message = "修复失败: ${e.message}"
                                    } finally {
                                        busy = false
                                        stage = ""
                                    }
                                }
                            },
                            enabled = source != null && !busy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("AI 修复")
                        }
                        OutlinedButton(
                            onClick = {
                                busy = true
                                stage = "下载模型…"
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        ModelManager.download(
                                            onProgress = { done, total ->
                                                if (total > 0) {
                                                    stage = "下载模型 ${(done * 100 / total)}%"
                                                } else {
                                                    stage = "下载模型 ${"%.1f".format(done / 1024.0 / 1024.0)} MB"
                                                }
                                            }
                                        )
                                        modelReady = ModelManager.isModelReady()
                                        message = "模型就绪"
                                    } catch (e: Exception) {
                                        message = "模型下载失败: ${e.message}（可手动下载后用「导入模型」）"
                                    } finally {
                                        busy = false
                                        stage = ""
                                    }
                                }
                            },
                            enabled = !busy && !modelReady
                        ) {
                            Text("下载模型")
                        }
                        OutlinedButton(
                            onClick = {
                                val chooser = JFileChooser()
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    runCatching {
                                        ModelManager.installFromFile(chooser.selectedFile)
                                    }.onSuccess {
                                        modelReady = ModelManager.isModelReady()
                                        message = "模型导入成功"
                                    }.onFailure {
                                        message = "导入失败: ${it.message}"
                                    }
                                }
                            },
                            enabled = !busy
                        ) {
                            Text("导入模型")
                        }
                        Text(
                            text = ModelManager.statusText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (stage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "修复结果为 AI 推测生成内容，非真实还原；模型与处理全程本地运行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}