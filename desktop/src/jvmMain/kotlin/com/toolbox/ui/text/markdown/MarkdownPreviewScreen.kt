package com.toolbox.ui.text.markdown

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.MarkdownText
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownPreviewScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var showHtml by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val extensions = remember { listOf(TablesExtension.create()) }
    val parser = remember { Parser.builder().extensions(extensions).build() }
    val renderer = remember { HtmlRenderer.builder().extensions(extensions).build() }
    val html = remember(input) {
        if (input.isBlank()) ""
        else renderer.render(parser.parse(input))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Markdown 预览") },
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    selected = !showHtml,
                    onClick = { showHtml = false },
                    label = { Text("预览") }
                )
                AppFilterChip(
                    selected = showHtml,
                    onClick = { showHtml = true },
                    label = { Text("HTML 源码") }
                )

                Spacer(modifier = Modifier.weight(1f))

                AppOutlinedButton(
                    onClick = {
                        if (html.isNotEmpty()) {
                            Toolkit.getDefaultToolkit().systemClipboard
                                .setContents(StringSelection(html), null)
                            message = "HTML 已复制到剪贴板"
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制 HTML")
                }

                AppOutlinedButton(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.selectedFile = File("output.html")
                        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                            try {
                                var target = chooser.selectedFile
                                if (!target.name.endsWith(".html")) {
                                    target = File(target.absolutePath + ".html")
                                }
                                target.writeText(
                                    "<!DOCTYPE html>\n<html>\n<head>\n" +
                                        "<meta charset=\"UTF-8\">\n" +
                                        "<title>Markdown Export</title>\n</head>\n<body>\n" +
                                        html +
                                        "\n</body>\n</html>"
                                )
                                message = "已导出：${target.absolutePath}"
                            } catch (e: Exception) {
                                message = "导出失败：${e.message}"
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出 HTML")
                }

                AppOutlinedButton(
                    onClick = {
                        input = ""
                        message = ""
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Markdown 源码",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("# 标题\n\n输入 Markdown 内容...") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showHtml) "HTML 源码" else "渲染预览",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            if (input.isBlank()) {
                                Text(
                                    text = "预览区域",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (showHtml) {
                                Text(
                                    text = html,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                MarkdownText(markdown = input)
                            }
                        }
                    }
                }
            }
        }
    }
}
