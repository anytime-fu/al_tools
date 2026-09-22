package com.toolbox.ui.life.noise

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var currentDb by remember { mutableFloatStateOf(0f) }
    var maxDb by remember { mutableFloatStateOf(0f) }
    var minDb by remember { mutableFloatStateOf(120f) }
    var avgDb by remember { mutableFloatStateOf(0f) }
    var history by remember { mutableStateOf(listOf<Float>()) }
    var recordCount by remember { mutableIntStateOf(0) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Store AudioRecord reference for cleanup
    var audioRecordRef by remember { mutableStateOf<AudioRecord?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            errorMessage = null
        } else {
            errorMessage = "权限被拒绝，请在设置中授权"
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                audioRecordRef?.stop()
                audioRecordRef?.release()
                audioRecordRef = null
            } catch (_: Exception) {}
        }
    }

    // Recording logic
    LaunchedEffect(isRecording) {
        if (isRecording && hasPermission) {
            withContext(Dispatchers.IO) {
                try {
                    val sampleRate = 44100
                    val channelConfig = AudioFormat.CHANNEL_IN_MONO
                    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                    if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                        withContext(Dispatchers.Main) {
                            errorMessage = "音频配置错误"
                            isRecording = false
                        }
                        return@withContext
                    }

                    val record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize * 2
                    )

                    if (record.state != AudioRecord.STATE_INITIALIZED) {
                        record.release()
                        withContext(Dispatchers.Main) {
                            errorMessage = "录音器初始化失败"
                            isRecording = false
                        }
                        return@withContext
                    }

                    audioRecordRef = record
                    val buffer = ShortArray(bufferSize)
                    record.startRecording()
                    val dbList = mutableListOf<Float>()

                    while (isActive && isRecording) {
                        val read = record.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read) {
                                val sample = buffer[i].toDouble() / Short.MAX_VALUE
                                sum += sample * sample
                            }
                            val rms = sqrt(sum / read)
                            val db = if (rms > 0.0001) (20 * log10(rms) + 90).coerceIn(0.0, 120.0) else 0.0

                            withContext(Dispatchers.Main) {
                                currentDb = db.toFloat()
                                if (currentDb > maxDb) maxDb = currentDb
                                if (currentDb < minDb && currentDb > 0) minDb = currentDb

                                dbList.add(currentDb)
                                if (dbList.size > 100) dbList.removeAt(0)
                                avgDb = dbList.average().toFloat()

                                history = history + currentDb
                                if (history.size > 100) history = history.takeLast(100)
                            }
                        }
                    }

                    record.stop()
                    record.release()
                    audioRecordRef = null
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "录音错误: ${e.message}"
                        isRecording = false
                    }
                    try {
                        audioRecordRef?.release()
                        audioRecordRef = null
                    } catch (_: Exception) {}
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("噪音检测") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = {
                            maxDb = 0f
                            minDb = 120f
                            avgDb = 0f
                            history = emptyList()
                            errorMessage = null
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "重置")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!hasPermission) {
            // Permission request UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "需要麦克风权限",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "噪音检测功能需要使用麦克风来测量环境声音分贝",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        try {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } catch (e: Exception) {
                            errorMessage = "无法请求权限: ${e.message}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("授权麦克风权限")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("去设置手动授权")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("返回")
                }

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // Main noise detection UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "${currentDb.toInt()}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = getNoiseColor(currentDb)
                )
                Text(
                    text = "分贝 (dB)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = getNoiseLevel(currentDb),
                    style = MaterialTheme.typography.titleLarge,
                    color = getNoiseColor(currentDb)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("最低", "${minDb.toInt()} dB", Color(0xFF4ADE80))
                    StatItem("平均", "${avgDb.toInt()} dB", Color(0xFF22D3EE))
                    StatItem("最高", "${maxDb.toInt()} dB", Color(0xFFEF4444))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 16.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        if (history.size >= 2) {
                            val path = Path()
                            val stepX = size.width / (history.size - 1)
                            val centerY = size.height

                            history.forEachIndexed { index, db ->
                                val x = index * stepX
                                val y = centerY - (db / 120f) * centerY
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = getNoiseColor(currentDb),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        errorMessage = null
                        if (isRecording) {
                            isRecording = false
                        } else {
                            isRecording = true
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isRecording) "点击停止检测" else "点击开始检测",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium, color = color)
    }
}

private fun getNoiseColor(db: Float): Color = when {
    db < 40 -> Color(0xFF4ADE80)
    db < 60 -> Color(0xFF22D3EE)
    db < 80 -> Color(0xFFFBBF24)
    db < 100 -> Color(0xFFF97316)
    else -> Color(0xFFEF4444)
}

private fun getNoiseLevel(db: Float): String = when {
    db < 30 -> "非常安静 - 图书馆级别"
    db < 40 -> "安静 - 耳语级别"
    db < 60 -> "正常 - 对话级别"
    db < 70 -> "较吵 - 办公室级别"
    db < 80 -> "吵闹 - 交通级别"
    db < 90 -> "很吵 - 工厂级别"
    db < 100 -> "非常吵 - 临界危险"
    else -> "危险 - 损伤听力"
}
