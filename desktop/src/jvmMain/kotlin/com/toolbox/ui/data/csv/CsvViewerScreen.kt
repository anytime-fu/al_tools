package com.toolbox.ui.data.csv

import com.toolbox.ui.components.appClickable

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import javax.swing.JFileChooser
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

private const val MAX_DISPLAY_ROWS = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvViewerScreen(onBack: () -> Unit) {
    var filePath by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    var headers by remember { mutableStateOf<List<String>>(emptyList()) }
    var rows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var sortColumn by remember { mutableStateOf(-1) }
    var sortAsc by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var delimiter by remember { mutableStateOf(',') }

    val visibleRows = remember(rows, searchText, sortColumn, sortAsc) {
        var list = rows
        if (searchText.isNotBlank()) {
            list = list.filter { row -> row.any { it.contains(searchText, ignoreCase = true) } }
        }
        if (sortColumn >= 0) {
            list = list.sortedWith(
                compareBy<List<String>> { it.getOrNull(sortColumn) ?: "" }
                    .let { if (sortAsc) it else it.reversed() }
            )
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CSV 查看器") },
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("CSV 文件") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    singleLine = true
                )

                AppOutlinedButton(
                    onClick = {
                        val chooser = JFileChooser()
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            filePath = chooser.selectedFile.absolutePath
                        }
                    }
                ) {
                    Text("浏览...")
                }

                Button(
                    onClick = {
                        val file = File(filePath)
                        if (!file.isFile) {
                            message = "文件不存在"
                            return@Button
                        }
                        try {
                            val text = file.readText()
                            val format = CSVFormat.DEFAULT.builder()
                                .setDelimiter(delimiter)
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreEmptyLines(true)
                                .setTrim(true)
                                .build()
                            CSVParser.parse(text, format).use { parser ->
                                headers = parser.headerNames
                                rows = parser.records.map { rec ->
                                    headers.indices.map { i ->
                                        try {
                                            rec.get(i)
                                        } catch (_: Exception) {
                                            ""
                                        }
                                    }
                                }
                            }
                            message = "已加载 ${rows.size} 行 × ${headers.size} 列"
                        } catch (e: Exception) {
                            headers = emptyList()
                            rows = emptyList()
                            message = "解析失败：${e.message}"
                        }
                    }
                ) {
                    Text("加载")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("分隔符", style = MaterialTheme.typography.labelMedium)
                listOf(',' to "逗号", ';' to "分号", '\t' to "制表符").forEach { (d, label) ->
                    AppFilterChip(
                        selected = delimiter == d,
                        onClick = { delimiter = d },
                        label = { Text(label) }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("搜索过滤行...") },
                    singleLine = true
                )
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (headers.isNotEmpty()) {
                val tableScroll = rememberScrollState()
                val cellWidth = 130.dp
                val totalWidth = cellWidth * headers.size

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(tableScroll)
                    ) {
                        Row(modifier = Modifier.width(totalWidth)) {
                            headers.forEachIndexed { i, h ->
                                HeaderCell(
                                    text = h,
                                    active = sortColumn == i,
                                    asc = sortAsc,
                                    onClick = {
                                        if (sortColumn == i) sortAsc = !sortAsc
                                        else {
                                            sortColumn = i
                                            sortAsc = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Divider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(tableScroll)
                    ) {
                        Column(
                            modifier = Modifier
                                .width(totalWidth)
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            visibleRows.take(MAX_DISPLAY_ROWS).forEach { row ->
                                Row(modifier = Modifier.width(totalWidth)) {
                                    row.forEach { cell ->
                                        DataCell(cell)
                                    }
                                }
                            }
                            if (visibleRows.size > MAX_DISPLAY_ROWS) {
                                Text(
                                    text = "仅显示前 $MAX_DISPLAY_ROWS 行（共 ${visibleRows.size} 行）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, active: Boolean, asc: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .width(130.dp)
            .appClickable(onClick = onClick)
    ) {
        Text(
            text = text + if (active) (if (asc) " ↑" else " ↓") else "",
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DataCell(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .width(130.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
