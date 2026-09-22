package com.toolbox.ui.armeasurement.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.data.local.entity.Measurement
import com.toolbox.ui.armeasurement.ArMeasureMode
import com.toolbox.ui.armeasurement.MeasureUnit
import com.toolbox.ui.armeasurement.SegmentInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementTopBar(
    onBack: () -> Unit,
    onToggleHistory: () -> Unit
) {
    TopAppBar(
        title = { Text("AR测量") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onToggleHistory) {
                Icon(Icons.Default.History, contentDescription = "历史记录")
            }
        }
    )
}

@Composable
fun MeasurementInfoPanel(
    pointCount: Int,
    totalDistanceCm: Double,
    segments: List<SegmentInfo>,
    currentUnit: MeasureUnit,
    formatDistance: (Double, MeasureUnit) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (pointCount == 0) {
                Text(
                    text = "点击屏幕放置测量点",
                    color = Color.White,
                    fontSize = 14.sp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatDistance(totalDistanceCm, currentUnit)} ${currentUnit.suffix}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已放置 $pointCount 个点",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                if (segments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    segments.forEach { seg ->
                        Text(
                            text = "点${seg.from + 1} → 点${seg.to + 1}: ${formatDistance(seg.distanceCm, currentUnit)} ${currentUnit.suffix}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementControlBar(
    pointCount: Int,
    measureMode: ArMeasureMode,
    currentUnit: MeasureUnit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onModeChange: (ArMeasureMode) -> Unit,
    onUnitChange: (MeasureUnit) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ArMeasureMode.entries.forEach { mode ->
                    FilterChip(
                        selected = measureMode == mode,
                        onClick = { onModeChange(mode) },
                        label = { Text(mode.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4ADE80),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MeasureUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = currentUnit == unit,
                        onClick = { onUnitChange(unit) },
                        label = { Text(unit.suffix, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onUndo, enabled = pointCount > 0) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "撤销",
                        tint = if (pointCount > 0) Color.White else Color.Gray
                    )
                }
                IconButton(onClick = onClear, enabled = pointCount > 0) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "清除",
                        tint = if (pointCount > 0) Color(0xFFF87171) else Color.Gray
                    )
                }
                IconButton(onClick = onSave, enabled = pointCount >= 2) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "保存",
                        tint = if (pointCount >= 2) Color(0xFF4ADE80) else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun SaveDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存测量") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("测量名称") },
                placeholder = { Text("例如：桌子宽度") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(title) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPanel(
    history: List<Measurement>,
    formatDistance: (Double, MeasureUnit) -> Unit,
    onDelete: (Measurement) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("测量历史") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            )

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无测量记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.id }) { measurement ->
                        HistoryItem(
                            measurement = measurement,
                            onDelete = { onDelete(measurement) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    measurement: Measurement,
    onDelete: () -> Unit
) {
    val unit = try {
        MeasureUnit.valueOf(measurement.unit)
    } catch (e: Exception) {
        MeasureUnit.CM
    }
    val converted = when (unit) {
        MeasureUnit.CM -> measurement.totalDistanceCm
        MeasureUnit.M -> measurement.totalDistanceCm / 100.0
        MeasureUnit.FT -> measurement.totalDistanceCm / 100.0 * 3.28084
    }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = measurement.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format("%.2f", converted)} ${unit.suffix} · ${measurement.pointCount}个点",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(measurement.createdAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
