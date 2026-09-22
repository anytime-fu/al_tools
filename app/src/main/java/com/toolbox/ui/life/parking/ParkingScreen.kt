package com.toolbox.ui.life.parking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class ParkingRecord(
    val id: String = UUID.randomUUID().toString(),
    val plateNumber: String,
    val location: String,
    val floor: String = "",
    val zone: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    onBack: () -> Unit
) {
    var records by remember { mutableStateOf(listOf<ParkingRecord>()) }
    var showDialog by remember { mutableStateOf(false) }
    var plateInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var floorInput by remember { mutableStateOf("") }
    var zoneInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("车牌记忆") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { paddingValues ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocalParking,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无停车记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    ParkingRecordCard(
                        record = record,
                        dateFormat = dateFormat,
                        onDelete = { records = records.filter { it.id != record.id } }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("记录停车位") },
            text = {
                Column {
                    OutlinedTextField(
                        value = plateInput,
                        onValueChange = { plateInput = it },
                        label = { Text("车牌号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("停车场名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = floorInput,
                            onValueChange = { floorInput = it },
                            label = { Text("楼层") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = zoneInput,
                            onValueChange = { zoneInput = it },
                            label = { Text("区域") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (plateInput.isNotBlank() || locationInput.isNotBlank()) {
                        records = records + ParkingRecord(
                            plateNumber = plateInput,
                            location = locationInput,
                            floor = floorInput,
                            zone = zoneInput,
                            note = noteInput
                        )
                        plateInput = ""
                        locationInput = ""
                        floorInput = ""
                        zoneInput = ""
                        noteInput = ""
                        showDialog = false
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ParkingRecordCard(
    record: ParkingRecord,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalParking,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (record.plateNumber.isNotBlank()) {
                    Text(
                        text = record.plateNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = dateFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = record.location, style = MaterialTheme.typography.bodyMedium)
            }

            if (record.floor.isNotBlank() || record.zone.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        if (record.floor.isNotBlank()) append("${record.floor}层")
                        if (record.floor.isNotBlank() && record.zone.isNotBlank()) append(" ")
                        if (record.zone.isNotBlank()) append("${record.zone}区")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (record.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
