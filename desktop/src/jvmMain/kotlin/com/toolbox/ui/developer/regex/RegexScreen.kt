package com.toolbox.ui.developer

import com.toolbox.ui.components.AppFilterChip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexScreen(onBack: () -> Unit) {
    var pattern by remember { mutableStateOf("") }
    var testString by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<MatchResult>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var useIgnoreCase by remember { mutableStateOf(false) }
    var useMultiline by remember { mutableStateOf(false) }
    var useDotAll by remember { mutableStateOf(false) }

    fun executeRegex() {
        try {
            errorMessage = null
            if (pattern.isEmpty()) {
                matches = emptyList()
                return
            }
            var options = setOf<RegexOption>()
            if (useIgnoreCase) options = options + RegexOption.IGNORE_CASE
            if (useMultiline) options = options + RegexOption.MULTILINE
            if (useDotAll) options = options + RegexOption.DOT_MATCHES_ALL
            
            val regex = Regex(pattern, options)
            matches = regex.findAll(testString).toList()
        } catch (e: Exception) {
            errorMessage = "正则表达式错误: ${e.message}"
            matches = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("正则测试") },
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
            // 正则表达式输入
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { 
                        pattern = it
                        executeRegex()
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("正则表达式") },
                    placeholder = { Text("输入正则表达式...") },
                    singleLine = true,
                    isError = errorMessage != null,
                    textStyle = FontFamily.Monospace.let { MaterialTheme.typography.bodyMedium.copy(fontFamily = it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    selected = useIgnoreCase,
                    onClick = { 
                        useIgnoreCase = !useIgnoreCase
                        executeRegex()
                    },
                    label = { Text("忽略大小写") }
                )
                AppFilterChip(
                    selected = useMultiline,
                    onClick = { 
                        useMultiline = !useMultiline
                        executeRegex()
                    },
                    label = { Text("多行模式") }
                )
                AppFilterChip(
                    selected = useDotAll,
                    onClick = { 
                        useDotAll = !useDotAll
                        executeRegex()
                    },
                    label = { Text("点号匹配全部") }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "找到 ${matches.size} 个匹配",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 测试字符串
            OutlinedTextField(
                value = testString,
                onValueChange = { 
                    testString = it
                    executeRegex()
                },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                label = { Text("测试字符串") },
                placeholder = { Text("输入要测试的文本...") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 匹配结果
            Text(
                text = "匹配结果",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (matches.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (pattern.isEmpty()) "输入正则表达式开始测试" else "没有找到匹配",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    matches.forEachIndexed { index, match ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "匹配 ${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "位置: ${match.range.first}-${match.range.last}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${match.value}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // 显示捕获组
                                if (match.groupValues.size > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "捕获组:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    match.groupValues.drop(1).forEachIndexed { groupIndex, group ->
                                        Text(
                                            text = "  $${groupIndex + 1}: \"$group\"",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 错误提示
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}