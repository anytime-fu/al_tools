package com.toolbox.ui.life.compass

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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableStateOf("北") }

    DisposableEffect(Unit) {
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    when (it.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> System.arraycopy(it.values, 0, gravity, 0, 3)
                        Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(it.values, 0, geomagnetic, 0, 3)
                    }

                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        if (azimuth < 0) azimuth += 360f

                        direction = when (azimuth.roundToInt()) {
                            in 0..22, in 338..360 -> "北"
                            in 23..67 -> "东北"
                            in 68..112 -> "东"
                            in 113..157 -> "东南"
                            in 158..202 -> "南"
                            in 203..247 -> "西南"
                            in 248..292 -> "西"
                            in 293..337 -> "西北"
                            else -> "北"
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("指南针") },
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
                text = direction,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${azimuth.roundToInt()}°",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2 - 30

                    // Outer circle
                    drawCircle(
                        color = Color.Gray,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Degree marks
                    for (i in 0 until 360 step 15) {
                        val radians = Math.toRadians(i.toDouble())
                        val startRadius = if (i % 90 == 0) radius - 30 else if (i % 45 == 0) radius - 20 else radius - 10
                        val startX = center.x + (startRadius * kotlin.math.cos(radians)).toFloat()
                        val startY = center.y + (startRadius * kotlin.math.sin(radians)).toFloat()
                        val endX = center.x + (radius * kotlin.math.cos(radians)).toFloat()
                        val endY = center.y + (radius * kotlin.math.sin(radians)).toFloat()

                        drawLine(
                            color = if (i % 90 == 0) Color.Red else Color.Gray,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (i % 90 == 0) 3f else 1f
                        )
                    }

                    // Compass needle
                    val needleAngle = Math.toRadians((-azimuth).toDouble())
                    val needleLength = radius - 40
                    val needleX = center.x + (needleLength * kotlin.math.sin(needleAngle)).toFloat()
                    val needleY = center.y - (needleLength * kotlin.math.cos(needleAngle)).toFloat()

                    drawLine(
                        color = Color.Red,
                        start = center,
                        end = Offset(needleX, needleY),
                        strokeWidth = 4f
                    )

                    // Center dot
                    drawCircle(
                        color = Color.Red,
                        radius = 8f,
                        center = center
                    )
                }

                // Direction labels
                Text("N", modifier = Modifier.offset(y = (-130).dp), color = Color.Red, fontWeight = FontWeight.Bold)
                Text("S", modifier = Modifier.offset(y = 130.dp))
                Text("E", modifier = Modifier.offset(x = 130.dp))
                Text("W", modifier = Modifier.offset(x = (-130).dp))
            }
        }
    }
}
