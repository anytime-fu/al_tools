package com.toolbox.ui.image.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toolbox.ui.image.ImageDropBridge
import com.toolbox.ui.image.ImageIo
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JFileChooser
import kotlin.math.roundToInt

private const val HANDLE_HIT = 22f
private const val MIN_CROP = 8f

private enum class DragMode { NONE, MOVE, TL, TR, BL, BR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(onBack: () -> Unit) {
    var source by remember { mutableStateOf<BufferedImage?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var crop by remember { mutableStateOf(Rect.Zero) }
    var ratio by remember { mutableStateOf<Float?>(null) }
    var message by remember { mutableStateOf("") }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var sourceFile by remember { mutableStateOf<File?>(null) }

    fun resetCrop(img: BufferedImage) {
        crop = Rect(0f, 0f, img.width.toFloat(), img.height.toFloat())
    }

    fun openImage(file: File) {
        val img = runCatching { ImageIo.read(file) }.getOrNull()
        if (img == null) {
            message = "无法读取图片: ${file.name}"
            return
        }
        source = img
        sourceFile = file
        preview = ImageIo.toImageBitmap(img)
        resetCrop(img)
        message = ""
    }

    fun applyRatio(newRatio: Float?) {
        ratio = newRatio
        val img = source ?: return
        if (newRatio == null) return
        var w = img.width.toFloat()
        var h = w / newRatio
        if (h > img.height) {
            h = img.height.toFloat()
            w = h * newRatio
        }
        val left = (img.width - w) / 2f
        val top = (img.height - h) / 2f
        crop = Rect(left, top, left + w, top + h)
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
                title = { Text("图片裁剪") },
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
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        openImage(chooser.selectedFile)
                    }
                }) {
                    Icon(Icons.Default.OpenInFull, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("打开图片")
                }
                OutlinedButton(onClick = { source?.let { resetCrop(it) } }, enabled = source != null) {
                    Text("重置")
                }
                Text(
                    text = "比例",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(null to "自由", 1f to "1:1", 4f / 3f to "4:3", 3f / 2f to "3:2", 16f / 9f to "16:9").forEach { (value, label) ->
                    FilterChip(
                        selected = ratio == value,
                        onClick = { applyRatio(value) },
                        label = { Text(label) },
                        enabled = source != null
                    )
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("已保存")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val img = source
            val bmp = preview
            if (img == null || bmp == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "打开或拖入图片，拖动边角调整裁剪区域",
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
                        .pointerInput(bmp, boxSize) {
                            var dragMode = DragMode.NONE
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val layout = ImageIo.fitLayout(
                                        boxSize.width.toFloat(), boxSize.height.toFloat(), img.width, img.height
                                    )
                                    val dLeft = layout.toDisplayX(crop.left)
                                    val dTop = layout.toDisplayY(crop.top)
                                    val dRight = layout.toDisplayX(crop.right)
                                    val dBottom = layout.toDisplayY(crop.bottom)
                                    fun near(a: Float, b: Float) = kotlin.math.abs(a - b) < HANDLE_HIT
                                    dragMode = when {
                                        near(offset.x, dLeft) && near(offset.y, dTop) -> DragMode.TL
                                        near(offset.x, dRight) && near(offset.y, dTop) -> DragMode.TR
                                        near(offset.x, dLeft) && near(offset.y, dBottom) -> DragMode.BL
                                        near(offset.x, dRight) && near(offset.y, dBottom) -> DragMode.BR
                                        offset.x in dLeft..dRight && offset.y in dTop..dBottom -> DragMode.MOVE
                                        else -> DragMode.NONE
                                    }
                                },
                                onDrag = { _, dragAmount ->
                                    val layout = ImageIo.fitLayout(
                                        boxSize.width.toFloat(), boxSize.height.toFloat(), img.width, img.height
                                    )
                                    val dx = dragAmount.x / layout.scale
                                    val dy = dragAmount.y / layout.scale
                                    val imgW = img.width.toFloat()
                                    val imgH = img.height.toFloat()
                                    crop = when (dragMode) {
                                        DragMode.MOVE -> {
                                            val w = crop.width
                                            val h = crop.height
                                            val left = (crop.left + dx).coerceIn(0f, imgW - w)
                                            val top = (crop.top + dy).coerceIn(0f, imgH - h)
                                            Rect(left, top, left + w, top + h)
                                        }
                                        DragMode.TL -> {
                                            val left = (crop.left + dx).coerceIn(0f, crop.right - MIN_CROP)
                                            val top = (crop.top + dy).coerceIn(0f, crop.bottom - MIN_CROP)
                                            Rect(left, top, crop.right, crop.bottom)
                                        }
                                        DragMode.TR -> {
                                            val right = (crop.right + dx).coerceIn(crop.left + MIN_CROP, imgW)
                                            val top = (crop.top + dy).coerceIn(0f, crop.bottom - MIN_CROP)
                                            Rect(crop.left, top, right, crop.bottom)
                                        }
                                        DragMode.BL -> {
                                            val left = (crop.left + dx).coerceIn(0f, crop.right - MIN_CROP)
                                            val bottom = (crop.bottom + dy).coerceIn(crop.top + MIN_CROP, imgH)
                                            Rect(left, crop.top, crop.right, bottom)
                                        }
                                        DragMode.BR -> {
                                            val right = (crop.right + dx).coerceIn(crop.left + MIN_CROP, imgW)
                                            val bottom = (crop.bottom + dy).coerceIn(crop.top + MIN_CROP, imgH)
                                            Rect(crop.left, crop.top, right, bottom)
                                        }
                                        DragMode.NONE -> crop
                                    }
                                },
                                onDragEnd = { dragMode = DragMode.NONE },
                                onDragCancel = { dragMode = DragMode.NONE }
                            )
                        }
                ) {
                    val layout = ImageIo.fitLayout(size.width, size.height, img.width, img.height)
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(layout.offsetX.roundToInt(), layout.offsetY.roundToInt()),
                        dstSize = IntSize(layout.dispW.roundToInt(), layout.dispH.roundToInt())
                    )
                    val dLeft = layout.toDisplayX(crop.left)
                    val dTop = layout.toDisplayY(crop.top)
                    val dRight = layout.toDisplayX(crop.right)
                    val dBottom = layout.toDisplayY(crop.bottom)
                    val dim = Color.Black.copy(alpha = 0.45f)
                    drawRect(dim, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, dTop))
                    drawRect(dim, topLeft = Offset(0f, dBottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - dBottom))
                    drawRect(dim, topLeft = Offset(0f, dTop), size = androidx.compose.ui.geometry.Size(dLeft, dBottom - dTop))
                    drawRect(dim, topLeft = Offset(dRight, dTop), size = androidx.compose.ui.geometry.Size(size.width - dRight, dBottom - dTop))
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(dLeft, dTop),
                        size = androidx.compose.ui.geometry.Size(dRight - dLeft, dBottom - dTop),
                        style = Stroke(width = 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (source == null) "" else "裁剪区域: x=${crop.left.roundToInt()} y=${crop.top.roundToInt()} " +
                        "w=${crop.width.roundToInt()} h=${crop.height.roundToInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val image = source ?: return@Button
                        val chooser = JFileChooser()
                        chooser.selectedFile = File(sourceFile?.let { "${it.nameWithoutExtension}_crop.png" } ?: "crop.png")
                        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return@Button
                        val target = chooser.selectedFile.let {
                            if (it.extension.isEmpty()) File(it.parentFile, "${it.name}.png") else it
                        }
                        val x = crop.left.roundToInt().coerceIn(0, image.width - 1)
                        val y = crop.top.roundToInt().coerceIn(0, image.height - 1)
                        val w = crop.width.roundToInt().coerceIn(1, image.width - x)
                        val h = crop.height.roundToInt().coerceIn(1, image.height - y)
                        val result = runCatching {
                            val cropped = image.getSubimage(x, y, w, h)
                            ImageIo.write(cropped, target, ImageIo.formatOf(target).ifEmpty { "png" })
                        }
                        message = result.fold(
                            onSuccess = { "已保存: ${target.absolutePath}" },
                            onFailure = { "保存失败: ${it.message}" }
                        )
                    },
                    enabled = source != null
                ) {
                    Text("裁剪并保存")
                }
            }
        }
    }
}
