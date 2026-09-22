package com.toolbox.ui.life.level

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager }
    var xAngle by remember { mutableFloatStateOf(0f) }
    var yAngle by remember { mutableFloatStateOf(0f) }
    var isLevel by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    xAngle = Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
                    yAngle = Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble()))).toFloat()

                    isLevel = abs(xAngle) < 1f && abs(yAngle) < 1f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水平仪") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLevel) "水平" else "倾斜",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLevel) Color(0xFF4ADE80) else MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2 - 20

                    // Outer circle
                    drawCircle(
                        color = Color.Gray,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Center dot
                    drawCircle(
                        color = if (isLevel) Color(0xFF4ADE80) else Color(0xFFEF4444),
                        radius = 8f,
                        center = center
                    )

                    // Ball position
                    val ballX = center.x + (yAngle / 45f) * radius
                    val ballY = center.y + (xAngle / 45f) * radius
                    val ballRadius = 20f

                    drawCircle(
                        color = if (isLevel) Color(0xFF4ADE80) else Color(0xFF3B82F6),
                        radius = ballRadius,
                        center = Offset(ballX.coerceIn(center.x - radius, center.x + radius),
                            ballY.coerceIn(center.y - radius, center.y + radius))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("X", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${String.format("%.1f", xAngle)}°",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Y", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${String.format("%.1f", yAngle)}°",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
