package com.toolbox.ui.text.counter

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordCountScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val stats = remember(input) { calcStats(input) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("字数统计") },
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
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("输入或粘贴文本，实时统计...") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppOutlinedButton(onClick = { input = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("字符数（含空格）", stats.charsWithSpaces)
                StatCard("字符数（不含空格）", stats.charsWithNoSpaces)
                StatCard("中文字符数", stats.chineseChars)
                StatCard("英文字母数", stats.letters)
                StatCard("英文单词数", stats.words)
                StatCard("数字位数", stats.digits)
                StatCard("标点符号数", stats.punctuation)
                StatCard("行数", stats.lines)
                StatCard("段落数", stats.paragraphs)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private data class TextStats(
    val charsWithSpaces: Int,
    val charsWithNoSpaces: Int,
    val chineseChars: Int,
    val letters: Int,
    val words: Int,
    val digits: Int,
    val punctuation: Int,
    val lines: Int,
    val paragraphs: Int
)

private fun calcStats(text: String): TextStats {
    val charsWithSpaces = text.length
    val charsWithNoSpaces = text.count { !it.isWhitespace() }
    val chineseChars = text.count { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }
    val letters = text.count { it.isLetter() && it.code !in 0x4E00..0x9FFF && it.code !in 0x3400..0x4DBF }
    val words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val digits = text.count { it.isDigit() }
    val punctuationCount = text.count { ch ->
        ch.category == CharCategory.CONNECTOR_PUNCTUATION ||
            ch.category == CharCategory.DASH_PUNCTUATION ||
            ch.category == CharCategory.START_PUNCTUATION ||
            ch.category == CharCategory.END_PUNCTUATION ||
            ch.category == CharCategory.INITIAL_QUOTE_PUNCTUATION ||
            ch.category == CharCategory.FINAL_QUOTE_PUNCTUATION ||
            ch.category == CharCategory.OTHER_PUNCTUATION
    }
    val lines = if (text.isEmpty()) 0 else text.split("\n").size
    val paragraphs = text.split(Regex("\\n\\s*\\n")).count { it.isNotBlank() }
    return TextStats(
        charsWithSpaces = charsWithSpaces,
        charsWithNoSpaces = charsWithNoSpaces,
        chineseChars = chineseChars,
        letters = letters,
        words = words,
        digits = digits,
        punctuation = punctuationCount,
        lines = lines,
        paragraphs = paragraphs
    )
}
