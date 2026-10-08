package com.toolbox.ui.text.diff

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.DiffRow
import com.toolbox.ui.components.SideBySideDiff
import com.toolbox.ui.components.buildDiffRows
import com.toolbox.ui.components.collapseEqualRows
import com.toolbox.ui.components.fastDiffDeltas
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDiffScreen(onBack: () -> Unit) {
    var leftText by remember { mutableStateOf("") }
    var rightText by remember { mutableStateOf("") }
    var diffRows by remember { mutableStateOf<List<DiffRow>>(emptyList()) }
    var deltaCount by remember { mutableStateOf(0) }
    var compared by remember { mutableStateOf(false) }
    var showFullText by remember { mutableStateOf(false) }
    var comparing by remember { mutableStateOf(false) }
    var compareJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文本对比") },
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "原始文本",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = leftText,
                        onValueChange = { leftText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("输入原始文本...") }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "对比文本",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = rightText,
                        onValueChange = { rightText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("输入对比文本...") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (comparing) {
                            compareJob?.cancel()
                            return@Button
                        }
                        comparing = true
                        compareJob = scope.launch {
                            var cancelled = false
                            try {
                                val linesA = leftText.lines()
                                val linesB = rightText.lines()
                                val deltas = withContext(Dispatchers.Default) {
                                    fastDiffDeltas(linesA, linesB)
                                }
                                coroutineContext.ensureActive()
                                val rows = withContext(Dispatchers.Default) {
                                    buildDiffRows(linesA, linesB, deltas)
                                }
                                diffRows = rows
                                deltaCount = deltas.size
                                compared = true
                            } catch (e: CancellationException) {
                                cancelled = true
                            } finally {
                                comparing = false
                            }
                        }
                    },
                    colors = if (comparing) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (comparing) Icons.Default.Close else Icons.Default.CompareArrows,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (comparing) "停止" else "对比")
                }

                AppOutlinedButton(
                    onClick = {
                        val tmp = leftText
                        leftText = rightText
                        rightText = tmp
                    }
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("交换")
                }

                AppOutlinedButton(
                    onClick = {
                        leftText = ""
                        rightText = ""
                        diffRows = emptyList()
                        deltaCount = 0
                        compared = false
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }

                if (comparing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在对比...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (compared && !comparing) {
                    Text(
                        text = if (deltaCount == 0) "内容完全一致"
                        else "共 $deltaCount 处差异",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (deltaCount == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (compared && deltaCount == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "两个文本完全一致，未发现差异。",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (compared && deltaCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showFullText,
                            onCheckedChange = { showFullText = it }
                        )
                        Text("显示完整内容", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                SideBySideDiff(
                    rows = if (showFullText) diffRows else collapseEqualRows(diffRows),
                    modifier = Modifier.fillMaxSize(),
                    leftTitle = "原始文本",
                    rightTitle = "对比文本"
                )
            }
        }
    }
}
