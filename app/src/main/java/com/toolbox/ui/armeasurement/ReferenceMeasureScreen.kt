package com.toolbox.ui.armeasurement

import android.graphics.Bitmap
import android.graphics.PointF
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.toolbox.ui.armeasurement.components.*

enum class ReferenceStep {
    CAPTURE, MARK_CARD, MARK_OBJECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceMeasureScreen(
    onBack: () -> Unit,
    onSwitchMode: () -> Unit,
    viewModel: ArMeasurementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var currentStep by remember { mutableStateOf(ReferenceStep.CAPTURE) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cardCorners by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var objectPoints by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var distanceCm by remember { mutableStateOf<Float?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    fun capturePhoto() {
        isCapturing = true
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    capturedBitmap = bitmap
                    currentStep = ReferenceStep.MARK_CARD
                    isCapturing = false
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    Toast.makeText(context, "拍照失败: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    fun reset() {
        currentStep = ReferenceStep.CAPTURE
        capturedBitmap = null
        cardCorners = emptyList()
        objectPoints = emptyList()
        distanceCm = null
    }

    fun handleTap(x: Float, y: Float) {
        when (currentStep) {
            ReferenceStep.MARK_CARD -> {
                if (cardCorners.size < 4) {
                    cardCorners = cardCorners + PointF(x, y)
                    if (cardCorners.size == 4) {
                        currentStep = ReferenceStep.MARK_OBJECT
                    }
                }
            }
            ReferenceStep.MARK_OBJECT -> {
                if (objectPoints.size < 2) {
                    objectPoints = objectPoints + PointF(x, y)
                    if (objectPoints.size == 2) {
                        distanceCm = CardDetector.calculateDistance(
                            objectPoints[0], objectPoints[1], cardCorners
                        )
                    }
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (currentStep) {
                        ReferenceStep.CAPTURE -> "拍照"
                        ReferenceStep.MARK_CARD -> "标记银行卡"
                        ReferenceStep.MARK_OBJECT -> "标记目标"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == ReferenceStep.CAPTURE) onBack()
                        else reset()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleHistory() }) {
                        Icon(Icons.Default.History, contentDescription = "历史记录")
                    }
                    TextButton(onClick = onSwitchMode) {
                        Text("切换AR", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.showHistory) {
                HistoryPanel(
                    history = uiState.history,
                    formatDistance = { _, _ -> },
                    onDelete = { viewModel.deleteMeasurement(it) },
                    onDismiss = { viewModel.toggleHistory() }
                )
            } else {
            when (currentStep) {
                ReferenceStep.CAPTURE -> {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onFrameAnalyzed = {},
                        imageCapture = imageCapture
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Button(
                            onClick = {
                                capturePhoto()
                            },
                            enabled = !isCapturing,
                            modifier = Modifier
                                .padding(32.dp)
                                .size(72.dp),
                            shape = RoundedCornerShape(36.dp)
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Camera, contentDescription = "拍照", modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }

                ReferenceStep.MARK_CARD, ReferenceStep.MARK_OBJECT -> {
                    // 显示拍摄的照片
                    capturedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "拍摄的照片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    handleTap(offset.x, offset.y)
                                }
                            }
                    ) {
                        drawRect(Color.Black.copy(alpha = 0.3f))

                        val cornerLabels = listOf("①", "②", "③", "④")
                        val sideLabels = listOf("长边85.6mm", "短边54mm", "长边85.6mm", "短边54mm")

                        cardCorners.forEachIndexed { i, p ->
                            drawCircle(Color(0xFF4ADE80), 16f, Offset(p.x, p.y))

                            if (i > 0) {
                                val prev = cardCorners[i - 1]
                                val lineColor = if (i == 1 || i == 3) Color(0xFFFFD700) else Color(0xFF4ADE80)
                                drawLine(lineColor, Offset(prev.x, prev.y), Offset(p.x, p.y), 4f)
                            }
                        }
                        if (cardCorners.size == 4) {
                            val lineColor = Color(0xFFFFD700)
                            drawLine(
                                lineColor,
                                Offset(cardCorners[3].x, cardCorners[3].y),
                                Offset(cardCorners[0].x, cardCorners[0].y),
                                4f
                            )
                        }

                        objectPoints.forEachIndexed { i, p ->
                            drawCircle(Color(0xFFEF4444), 16f, Offset(p.x, p.y))
                        }
                        if (objectPoints.size >= 2) {
                            drawLine(
                                Color.White,
                                Offset(objectPoints[0].x, objectPoints[0].y),
                                Offset(objectPoints[1].x, objectPoints[1].y),
                                3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                when (currentStep) {
                                    ReferenceStep.MARK_CARD -> {
                                        Text(
                                            text = "标记银行卡4个角（${cardCorners.size}/4）",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "①→② 为长边(85.6mm)",
                                            color = Color(0xFFFFD700),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "②→③ 为短边(54mm)",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "顺序：长边左端→长边右端→短边右端→短边左端",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    ReferenceStep.MARK_OBJECT -> {
                                        if (distanceCm != null) {
                                            Text(
                                                text = "%.1f cm".format(distanceCm),
                                                color = Color.White,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "点击目标物体的两端（${objectPoints.size}/2）",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = {
                                when (currentStep) {
                                    ReferenceStep.MARK_CARD -> {
                                        cardCorners = cardCorners.dropLast(1)
                                    }
                                    ReferenceStep.MARK_OBJECT -> {
                                        objectPoints = objectPoints.dropLast(1)
                                        distanceCm = null
                                    }
                                    else -> {}
                                }
                            }) {
                                Icon(Icons.Default.Undo, contentDescription = "撤销", tint = Color.White)
                            }

                            IconButton(onClick = { reset() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "重置", tint = Color(0xFFF87171))
                            }

                            IconButton(
                                onClick = { showSaveDialog = true },
                                enabled = distanceCm != null
                            ) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "保存",
                                    tint = if (distanceCm != null) Color(0xFF4ADE80) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { title ->
                if (objectPoints.size >= 2 && cardCorners.size >= 4 && distanceCm != null) {
                    viewModel.saveReferenceMeasurement(title, distanceCm!!, 2)
                }
                showSaveDialog = false
            }
        )
    }
}
