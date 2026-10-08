package com.toolbox.ui.data.database

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_ROWS = 200

private data class QueryResult(
    val columns: List<String>,
    val rows: List<List<String>>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatabaseViewerScreen(onBack: () -> Unit) {
    var dbPath by remember { mutableStateOf("") }
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTable by remember { mutableStateOf("") }
    var sqlText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<QueryResult?>(null) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun withConnection(block: (Connection) -> Unit) {
        val file = File(dbPath)
        if (!file.isFile) {
            message = "数据库文件不存在"
            return
        }
        loading = true
        message = ""
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Class.forName("org.sqlite.JDBC")
                    DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
                        block(conn)
                    }
                }
            } catch (e: Exception) {
                message = "操作失败：${e.message}"
                result = null
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据库查看器") },
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
                    value = dbPath,
                    onValueChange = { dbPath = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("SQLite 数据库文件") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    singleLine = true
                )

                AppOutlinedButton(
                    onClick = {
                        val chooser = JFileChooser()
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            dbPath = chooser.selectedFile.absolutePath
                        }
                    }
                ) {
                    Text("浏览...")
                }

                Button(
                    onClick = {
                        withConnection { conn ->
                            val list = mutableListOf<String>()
                            conn.createStatement().use { stmt ->
                                stmt.executeQuery(
                                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
                                ).use { rs ->
                                    while (rs.next()) list.add(rs.getString(1))
                                }
                            }
                            tables = list
                            message = "共 ${list.size} 张表"
                            result = null
                        }
                    }
                ) {
                    Text("加载表")
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("操作失败")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            if (tables.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tables.forEach { table ->
                            AppFilterChip(
                                selected = selectedTable == table,
                                onClick = {
                                    selectedTable = table
                                    sqlText = "SELECT * FROM \"$table\" LIMIT $MAX_ROWS"
                                    withConnection { conn ->
                                        result = executeQuery(conn, sqlText)
                                        message = "表 $table：${result?.rows?.size ?: 0} 行"
                                    }
                                },
                                label = {
                                    Text(
                                        text = table,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = sqlText,
                    onValueChange = { sqlText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("SQL 查询") },
                    placeholder = { Text("SELECT * FROM ...") }
                )

                Button(
                    onClick = {
                        withConnection { conn ->
                            result = executeQuery(conn, sqlText)
                            message = "查询完成：${result?.rows?.size ?: 0} 行"
                        }
                    },
                    enabled = !loading && sqlText.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("执行")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            result?.let { r ->
                if (r.columns.isNotEmpty()) {
                    val tableScroll = rememberScrollState()
                    val cellWidth = 140.dp
                    val totalWidth = cellWidth * r.columns.size

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
                                r.columns.forEach { col ->
                                    Text(
                                        text = col,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
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
                                r.rows.forEach { row ->
                                    Row(modifier = Modifier.width(totalWidth)) {
                                        row.forEach { cell ->
                                            Text(
                                                text = cell,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .width(cellWidth)
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (loading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun executeQuery(conn: Connection, sql: String): QueryResult {
    val trimmed = sql.trim()
    if (trimmed.isEmpty()) throw IllegalArgumentException("SQL 为空")
    conn.createStatement().use { stmt ->
        val isResultSet = stmt.execute(trimmed)
        if (!isResultSet) {
            return QueryResult(listOf("影响行数"), listOf(listOf(stmt.updateCount.toString())))
        }
        stmt.resultSet.use { rs ->
            val meta = rs.metaData
            val columns = (1..meta.columnCount).map { meta.getColumnLabel(it) }
            val rows = mutableListOf<List<String>>()
            while (rs.next() && rows.size < MAX_ROWS) {
                rows.add((1..meta.columnCount).map { rs.getString(it) ?: "NULL" })
            }
            return QueryResult(columns, rows)
        }
    }
}
