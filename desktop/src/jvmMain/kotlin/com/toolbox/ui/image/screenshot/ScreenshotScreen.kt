package com.toolbox.ui.image.screenshot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toolbox.ui.image.ImageIo
import com.toolbox.ui.image.ZoomViewport
import com.toolbox.util.AppWindowBridge
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.JFileChooser
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ScreenshotScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewport = remember { ZoomViewport() }
    var captured by remember { mutableStateOf<BufferedImage?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var selection by remember { mutableStateOf<Rect?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var capturing by remember { mutableStateOf(false) }
    var panMode by remember { mutableStateOf(false) }
    var fitted by remember(captured) { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun doCapture(hint: String) {
        if (capturing) return
        capturing = true
        message = "正在隐藏窗口并截取屏幕..."
        scope.launch {
            AppWindowBridge.hideForCapture()
            delay(600)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val shot = ImageIo.captureScreen()
                    shot to ImageIo.toImageBitmap(shot)
                }
            }
            AppWindowBridge.restoreAfterCapture()
            capturing = false
            result.onSuccess { (shot, bmp) ->
                captured = shot
                preview = bmp
                selection = null
                message = hint
            }.onFailure {
                message = "截图失败: ${it.message}"
            }
        }
    }

    fun currentImage(): BufferedImage? {
        val shot = captured ?: return null
        val sel = selection ?: return shot
        val x = sel.left.roundToInt().coerceIn(0, shot.width - 1)
        val y = sel.top.roundToInt().coerceIn(0, shot.height - 1)
        val w = sel.width.roundToInt().coerceIn(1, shot.width - x)
        val h = sel.height.roundToInt().coerceIn(1, shot.height - y)
        return shot.getSubimage(x, y, w, h)
    }

    LaunchedEffect(captured, boxSize) {
        val shot = captured ?: return@LaunchedEffect
        if (boxSize.width == 0 || boxSize.height == 0) return@LaunchedEffect
        val w = boxSize.width.toFloat()
        val h = boxSize.height.toFloat()
        if (!fitted) {
            viewport.resetToFit(w, h, shot.width, shot.height)
            fitted = true
        } else {
            viewport.clampPan(w, h, shot.width, shot.height)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("屏幕截图") },
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
                    onClick = { doCapture("已截取全屏，可直接保存/复制，或拖拽选择区域") },
                    enabled = !capturing
                ) {
                    Icon(Icons.Default.Screenshot, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (capturing) "截取中..." else "全屏截图")
                }
                OutlinedButton(
                    onClick = { doCapture("请在预览图上拖拽选择截图区域") },
                    enabled = !capturing
                ) {
                    Text("区域截图")
                }
                OutlinedButton(
                    onClick = {
                        selection = null
                        message = "已清除区域选择"
                    },
                    enabled = selection != null
                ) {
                    Text("清除区域")
                }
                OutlinedButton(
                    onClick = {
                        val image = currentImage() ?: return@OutlinedButton
                        val result = runCatching { ImageIo.copyImageToClipboard(image) }
                        message = if (result.isSuccess) "已复制到剪贴板" else "复制失败: ${result.exceptionOrNull()?.message}"
                    },
                    enabled = captured != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }
                Button(
                    onClick = {
                        val image = currentImage() ?: return@Button
                        val chooser = JFileChooser()
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                        chooser.selectedFile = File("screenshot_$stamp.png")
                        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return@Button
                        val target = chooser.selectedFile.let {
                            if (it.extension.isEmpty()) File(it.parentFile, "${it.name}.png") else it
                        }
                        val result = runCatching {
                            ImageIo.write(image, target, ImageIo.formatOf(target).ifEmpty { "png" })
                        }
                        message = result.fold(
                            onSuccess = { "已保存: ${target.absolutePath}" },
                            onFailure = { "保存失败: ${it.message}" }
                        )
                    },
                    enabled = captured != null
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !panMode,
                    onClick = { panMode = false },
                    label = { Text("框选区域") },
                    enabled = captured != null
                )
                FilterChip(
                    selected = panMode,
                    onClick = { panMode = true },
                    label = { Text("平移移动") },
                    enabled = captured != null
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val shot = captured ?: return@IconButton
                        viewport.zoomBy(1f / 1.25f, null, boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = captured != null
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "缩小")
                }
                Text(
                    text = "${viewport.zoomPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )
                IconButton(
                    onClick = {
                        val shot = captured ?: return@IconButton
                        viewport.zoomBy(1.25f, null, boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = captured != null
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "放大")
                }
                OutlinedButton(
                    onClick = {
                        val shot = captured ?: return@OutlinedButton
                        viewport.setZoom(1f, null, boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = captured != null
                ) {
                    Text("1:1")
                }
                OutlinedButton(
                    onClick = {
                        val shot = captured ?: return@OutlinedButton
                        viewport.resetToFit(boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = captured != null
                ) {
                    Text("适应")
                }
                Text(
                    text = "滚轮缩放；平移模式下拖动移动视图",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("已")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val shot = captured
            val bmp = preview
            if (shot == null || bmp == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击「全屏截图」或「区域截图」开始",
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
                            val change = event.changes.firstOrNull() ?: return@onPointerEvent
                            val delta = change.scrollDelta.y
                            val factor = if (delta < 0f) 1.15f else 1f / 1.15f
                            viewport.zoomBy(
                                factor = factor,
                                anchor = change.position,
                                boxW = boxSize.width.toFloat(),
                                boxH = boxSize.height.toFloat(),
                                imgW = shot.width,
                                imgH = shot.height
                            )
                        }
                        .pointerInput(panMode, bmp, boxSize) {
                            if (panMode) {
                                detectDragGestures { _, dragAmount ->
                                    viewport.panBy(
                                        dragAmount.x,
                                        dragAmount.y,
                                        boxSize.width.toFloat(),
                                        boxSize.height.toFloat(),
                                        shot.width,
                                        shot.height
                                    )
                                }
                            } else {
                                var anchor = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        anchor = offset
                                        selection = null
                                    },
                                    onDrag = { change, _ ->
                                        val layout = viewport.layout(
                                            boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height
                                        )
                                        val x1 = layout.toImageX(anchor.x)
                                        val y1 = layout.toImageY(anchor.y)
                                        val x2 = layout.toImageX(change.position.x)
                                        val y2 = layout.toImageY(change.position.y)
                                        val left = min(x1, x2).coerceIn(0f, shot.width.toFloat())
                                        val top = min(y1, y2).coerceIn(0f, shot.height.toFloat())
                                        val right = max(x1, x2).coerceIn(0f, shot.width.toFloat())
                                        val bottom = max(y1, y2).coerceIn(0f, shot.height.toFloat())
                                        if (right - left > 2f && bottom - top > 2f) {
                                            selection = Rect(left, top, right, bottom)
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    val layout = viewport.layout(size.width, size.height, shot.width, shot.height)
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(layout.offsetX.roundToInt(), layout.offsetY.roundToInt()),
                        dstSize = IntSize(layout.dispW.roundToInt(), layout.dispH.roundToInt())
                    )
                    val sel = selection
                    if (sel != null) {
                        val dLeft = layout.toDisplayX(sel.left)
                        val dTop = layout.toDisplayY(sel.top)
                        val dRight = layout.toDisplayX(sel.right)
                        val dBottom = layout.toDisplayY(sel.bottom)
                        val dim = Color.Black.copy(alpha = 0.4f)
                        drawRect(dim, Offset.Zero, Size(size.width, dTop))
                        drawRect(dim, Offset(0f, dBottom), Size(size.width, size.height - dBottom))
                        drawRect(dim, Offset(0f, dTop), Size(dLeft, dBottom - dTop))
                        drawRect(dim, Offset(dRight, dTop), Size(size.width - dRight, dBottom - dTop))
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(dLeft, dTop),
                            size = Size(dRight - dLeft, dBottom - dTop),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (selection != null) {
                    "已选区域: x=${selection!!.left.roundToInt()} y=${selection!!.top.roundToInt()} " +
                        "w=${selection!!.width.roundToInt()} h=${selection!!.height.roundToInt()}  （保存/复制将使用该区域）"
                } else if (captured != null) {
                    "当前为全屏截图 (${captured!!.width} × ${captured!!.height})"
                } else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
