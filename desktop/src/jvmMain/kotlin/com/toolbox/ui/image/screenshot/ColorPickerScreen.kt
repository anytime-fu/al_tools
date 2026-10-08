package com.toolbox.ui.image.screenshot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PickedColor(val r: Int, val g: Int, val b: Int) {
    val hex: String get() = ImageIo.colorToHex(r, g, b)
    val rgb: String get() = "RGB($r, $g, $b)"
    val hsv: String
        get() {
            val hsb = java.awt.Color.RGBtoHSB(r, g, b, null)
            return "HSV(${(hsb[0] * 360).toInt()}°, ${(hsb[1] * 100).toInt()}%, ${(hsb[2] * 100).toInt()}%)"
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ColorPickerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewport = remember { ZoomViewport() }
    var snapshot by remember { mutableStateOf<BufferedImage?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var hovered by remember { mutableStateOf<PickedColor?>(null) }
    var picked by remember { mutableStateOf<PickedColor?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var capturing by remember { mutableStateOf(false) }
    var fitted by remember(snapshot) { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun colorAt(displayX: Float, displayY: Float): PickedColor? {
        val shot = snapshot ?: return null
        val layout = viewport.layout(boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
        val x = layout.toImageX(displayX).toInt()
        val y = layout.toImageY(displayY).toInt()
        if (x !in 0 until shot.width || y !in 0 until shot.height) return null
        val rgb = shot.getRGB(x, y)
        return PickedColor((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
    }

    LaunchedEffect(snapshot, boxSize) {
        val shot = snapshot ?: return@LaunchedEffect
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
                title = { Text("取色器") },
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
                        if (!capturing) {
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
                                    snapshot = shot
                                    preview = bmp
                                    picked = null
                                    hovered = null
                                    message = "移动鼠标查看颜色，点击图片拾取颜色"
                                }.onFailure {
                                    message = "截屏失败: ${it.message}"
                                }
                            }
                        }
                    },
                    enabled = !capturing
                ) {
                    Icon(Icons.Default.Colorize, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (capturing) "截取中..." else "开始取色")
                }
                val current = picked ?: hovered
                OutlinedButton(
                    onClick = {
                        current?.let {
                            ImageIo.copyTextToClipboard(it.hex)
                            message = "已复制 HEX: ${it.hex}"
                        }
                    },
                    enabled = current != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制 HEX")
                }
                OutlinedButton(
                    onClick = {
                        current?.let {
                            ImageIo.copyTextToClipboard(it.rgb)
                            message = "已复制 ${it.rgb}"
                        }
                    },
                    enabled = current != null
                ) {
                    Text("复制 RGB")
                }
                OutlinedButton(
                    onClick = {
                        current?.let {
                            ImageIo.copyTextToClipboard(it.hsv)
                            message = "已复制 ${it.hsv}"
                        }
                    },
                    enabled = current != null
                ) {
                    Text("复制 HSV")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val shot = snapshot ?: return@IconButton
                        viewport.zoomBy(1f / 1.25f, null, boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = snapshot != null
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
                        val shot = snapshot ?: return@IconButton
                        viewport.zoomBy(1.25f, null, boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = snapshot != null
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "放大")
                }
                OutlinedButton(
                    onClick = {
                        val shot = snapshot ?: return@OutlinedButton
                        viewport.setZoom(1f, null, boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = snapshot != null
                ) {
                    Text("1:1")
                }
                OutlinedButton(
                    onClick = {
                        val shot = snapshot ?: return@OutlinedButton
                        viewport.resetToFit(boxSize.width.toFloat(), boxSize.height.toFloat(), shot.width, shot.height)
                    },
                    enabled = snapshot != null
                ) {
                    Text("适应")
                }
                Text(
                    text = "滚轮缩放，拖动平移，点击拾取颜色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("已复制") || message.startsWith("已拾取")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val shot = snapshot
                val bmp = preview
                if (shot == null || bmp == null) {
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "点击「开始取色」截取屏幕，然后在图片上移动/点击取色",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            .onPointerEvent(PointerEventType.Move) { event ->
                                val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                                hovered = colorAt(position.x, position.y)
                            }
                            .pointerInput(bmp, boxSize) {
                                detectTapGestures { offset ->
                                    colorAt(offset.x, offset.y)?.let {
                                        picked = it
                                        message = "已拾取 ${it.hex}"
                                    }
                                }
                            }
                            .pointerInput(bmp, boxSize) {
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
                            }
                    ) {
                        val layout = viewport.layout(size.width, size.height, shot.width, shot.height)
                        drawImage(
                            image = bmp,
                            dstOffset = IntOffset(layout.offsetX.roundToInt(), layout.offsetY.roundToInt()),
                            dstSize = IntSize(layout.dispW.roundToInt(), layout.dispH.roundToInt())
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val current = picked ?: hovered
                        Text(
                            text = if (picked != null) "已拾取颜色" else if (hovered != null) "悬停颜色" else "颜色信息",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (current != null) Color(current.r, current.g, current.b)
                                    else MaterialTheme.colorScheme.surface
                                )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (current == null) {
                            Text(
                                text = "暂无颜色数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("HEX", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(current.hex, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("RGB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(current.rgb, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("HSV", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(current.hsv, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
