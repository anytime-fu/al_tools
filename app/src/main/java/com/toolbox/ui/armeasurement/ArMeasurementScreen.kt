package com.toolbox.ui.armeasurement

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.toolbox.ui.armeasurement.components.*
import io.github.sceneview.ar.ARScene

enum class MeasureMode {
    AR, REFERENCE, AUTO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArMeasurementScreen(
    onBack: () -> Unit,
    viewModel: ArMeasurementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()

    var arCoreAvailable by remember { mutableStateOf<Boolean?>(null) }
    var currentMode by remember { mutableStateOf(MeasureMode.AUTO) }
    var showModeDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Check ARCore availability
    LaunchedEffect(Unit) {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            arCoreAvailable = availability.isSupported
            if (!availability.isSupported) {
                currentMode = MeasureMode.REFERENCE
            }
        } catch (e: Exception) {
            arCoreAvailable = false
            currentMode = MeasureMode.REFERENCE
        }
    }

    // Determine actual mode
    val actualMode = when (currentMode) {
        MeasureMode.AUTO -> if (arCoreAvailable == true) MeasureMode.AR else MeasureMode.REFERENCE
        else -> currentMode
    }

    // Mode selection dialog
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("选择测量模式") },
            text = {
                Column {
                    MeasureModeOption(
                        title = "AR测量",
                        description = if (arCoreAvailable == true) "使用ARCore进行精确测量" else "需要ARCore支持",
                        icon = Icons.Default.ViewInAr,
                        enabled = arCoreAvailable == true,
                        selected = currentMode == MeasureMode.AR,
                        onClick = {
                            currentMode = MeasureMode.AR
                            showModeDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MeasureModeOption(
                        title = "参照物测量",
                        description = "使用银行卡作为参照物测量",
                        icon = Icons.Default.CreditCard,
                        enabled = true,
                        selected = currentMode == MeasureMode.REFERENCE,
                        onClick = {
                            currentMode = MeasureMode.REFERENCE
                            showModeDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MeasureModeOption(
                        title = "自动选择",
                        description = "根据设备情况自动选择最佳模式",
                        icon = Icons.Default.AutoMode,
                        enabled = true,
                        selected = currentMode == MeasureMode.AUTO,
                        onClick = {
                            currentMode = MeasureMode.AUTO
                            showModeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    when (actualMode) {
        MeasureMode.AR -> {
            ArMeasureContent(
                onBack = onBack,
                onSwitchMode = { showModeDialog = true },
                viewModel = viewModel
            )
        }
        MeasureMode.REFERENCE -> {
            ReferenceMeasureScreen(
                onBack = onBack,
                onSwitchMode = { showModeDialog = true },
                viewModel = viewModel
            )
        }
        MeasureMode.AUTO -> {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在检测设备能力...")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasureModeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArMeasureContent(
    onBack: () -> Unit,
    onSwitchMode: () -> Unit,
    viewModel: ArMeasurementViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var currentFrame by remember { mutableStateOf<Frame?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.savedMessage) {
        uiState.savedMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearSavedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AR测量") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleHistory() }) {
                        Icon(Icons.Default.History, contentDescription = "历史记录")
                    }
                    TextButton(onClick = onSwitchMode) {
                        Text("切换模式", color = MaterialTheme.colorScheme.primary)
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
            } else if (errorMessage != null) {
                ErrorContent(
                    message = errorMessage!!,
                    onBack = onBack
                )
            } else {
                ARScene(
                    planeRenderer = true,
                    sessionConfiguration = { session, config ->
                        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                        config.focusMode = Config.FocusMode.AUTO
                    },
                    onSessionUpdated = { session, frame ->
                        currentFrame = frame
                    },
                    onSessionFailed = { exception ->
                        errorMessage = "AR初始化失败: ${exception.message}"
                    }
                )

                // Tap overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val frame = currentFrame ?: return@detectTapGestures
                                if (frame.camera.trackingState != TrackingState.TRACKING) return@detectTapGestures

                                val hits = frame.hitTest(offset.x, offset.y)
                                for (hit in hits) {
                                    val trackable = hit.trackable
                                    if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                                        val pose = hit.hitPose
                                        viewModel.addPoint(pose.tx(), pose.ty(), pose.tz())
                                        break
                                    }
                                }
                            }
                        }
                )

                // Overlay UI
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(1f))

                    MeasurementInfoPanel(
                        pointCount = uiState.points.size,
                        totalDistanceCm = uiState.totalDistanceCm,
                        segments = uiState.segments,
                        currentUnit = uiState.currentUnit,
                        formatDistance = viewModel::formatDistance
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MeasurementControlBar(
                        pointCount = uiState.points.size,
                        measureMode = uiState.measureMode,
                        currentUnit = uiState.currentUnit,
                        onUndo = { viewModel.undoLastPoint() },
                        onClear = { viewModel.clearPoints() },
                        onSave = { showSaveDialog = true },
                        onModeChange = { viewModel.setMode(it) },
                        onUnitChange = { viewModel.setUnit(it) }
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { title ->
                viewModel.saveMeasurement(title)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("返回")
        }
    }
}
