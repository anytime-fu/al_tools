package com.toolbox.ui.data.chart

import com.toolbox.ui.components.AppFilterChip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ChartType(val label: String) {
    BAR("柱状图"),
    LINE("折线图"),
    PIE("饼图")
}

private data class ChartPoint(val label: String, val value: Double)

private val ChartColors = listOf(
    Color(0xFF4285F4),
    Color(0xFFEA4335),
    Color(0xFFFBBC05),
    Color(0xFF34A853),
    Color(0xFF9C27B0),
    Color(0xFFFF6D00),
    Color(0xFF00ACC1),
    Color(0xFF8D6E63)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataChartScreen(onBack: () -> Unit) {
    var input by remember {
        mutableStateOf("一月,120\n二月,200\n三月,150\n四月,280\n五月,310")
    }
    var chartType by remember { mutableStateOf(ChartType.BAR) }
    var points by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据可视化") },
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartType.values().forEach { t ->
                    AppFilterChip(
                        selected = chartType == t,
                        onClick = { chartType = t },
                        label = { Text(t.label) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        message = ""
                        try {
                            points = input.lines()
                                .filter { it.isNotBlank() }
                                .map { line ->
                                    val parts = line.split(",", "，", "\t", limit = 2)
                                    if (parts.size < 2) throw IllegalArgumentException("每行需要“标签,数值”格式：$line")
                                    val value = parts[1].trim().toDoubleOrNull()
                                        ?: throw IllegalArgumentException("数值无效：${parts[1]}")
                                    ChartPoint(parts[0].trim(), value)
                                }
                            if (points.isEmpty()) throw IllegalArgumentException("没有有效数据")
                            message = "共 ${points.size} 个数据点"
                        } catch (e: Exception) {
                            points = emptyList()
                            message = "解析失败：${e.message}"
                        }
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("生成")
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("解析失败")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    placeholder = { Text("标签,数值（每行一条）") }
                )

                Card(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (points.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("图表区域", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else when (chartType) {
                        ChartType.BAR -> BarChart(points, Modifier.fillMaxSize().padding(16.dp))
                        ChartType.LINE -> LineChart(points, Modifier.fillMaxSize().padding(16.dp))
                        ChartType.PIE -> PieChart(points, Modifier.fillMaxSize().padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BarChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val max = (points.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(0.001)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEachIndexed { i, p ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = p.value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .fillMaxHeight((p.value / max).toFloat().coerceIn(0.02f, 1f))
                            .background(ChartColors[i % ChartColors.size])
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { p ->
                Text(
                    text = p.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LineChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val max = (points.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(0.001)
    val min = (points.minOfOrNull { it.value } ?: 0.0)
    val range = (max - min).coerceAtLeast(0.001)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val path = Path()
            val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
            points.forEachIndexed { i, p ->
                val x = if (points.size > 1) i * stepX else size.width / 2
                val y = (size.height * (1 - (p.value - min) / range)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = ChartColors[0], style = Stroke(width = 5f))

            points.forEachIndexed { i, p ->
                val x = if (points.size > 1) i * stepX else size.width / 2
                val y = (size.height * (1 - (p.value - min) / range)).toFloat()
                drawCircle(color = ChartColors[1], radius = 8f, center = Offset(x, y))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { p ->
                Text(
                    text = "${p.label} ${p.value}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PieChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val total = points.sumOf { it.value }.coerceAtLeast(0.001)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val diameter = minOf(size.width, size.height) * 0.85f
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            var startAngle = -90f
            points.forEachIndexed { i, p ->
                val sweep = (p.value / total * 360.0).toFloat()
                drawArc(
                    color = ChartColors[i % ChartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(diameter, diameter)
                )
                startAngle += sweep
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 130.dp)
                .verticalScroll(rememberScrollState())
        ) {
            points.forEachIndexed { i, p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(ChartColors[i % ChartColors.size])
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${p.label}  ${p.value}  (${String.format("%.1f", p.value / total * 100)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
