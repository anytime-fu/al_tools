package com.toolbox.ui.note

import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveComplete by viewModel.saveComplete.collectAsState()
    val isAiProcessing by viewModel.isAiProcessing.collectAsState()
    val aiError by viewModel.aiError.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val scope = rememberCoroutineScope()

    var isPreviewMode by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }

    LaunchedEffect(saveComplete) {
        if (saveComplete) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == -1L) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isPreviewMode = !isPreviewMode }
                    ) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreviewMode) "编辑" else "预览"
                        )
                    }
                    IconButton(
                        onClick = { 
                            showExportMenu = true
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "导出")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                viewModel.save()
                            }
                        },
                        enabled = !isSaving && title.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isPreviewMode) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AssistChip(
                            onClick = { viewModel.aiAssist("continue") },
                            label = { Text("续写") },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            },
                            enabled = !isAiProcessing
                        )
                        AssistChip(
                            onClick = { viewModel.aiAssist("polish") },
                            label = { Text("润色") },
                            leadingIcon = {
                                Icon(Icons.Default.Spa, contentDescription = null)
                            },
                            enabled = !isAiProcessing
                        )
                        AssistChip(
                            onClick = { viewModel.aiAssist("summarize") },
                            label = { Text("总结") },
                            leadingIcon = {
                                Icon(Icons.Default.Summarize, contentDescription = null)
                            },
                            enabled = !isAiProcessing
                        )
                        AssistChip(
                            onClick = { showAiMenu = true },
                            label = { Text("翻译") },
                            leadingIcon = {
                                Icon(Icons.Default.Translate, contentDescription = null)
                            },
                            enabled = !isAiProcessing
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (aiError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = aiError ?: "",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(onClick = { viewModel.clearAiError() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                placeholder = { Text("标题") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 标签选择区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Label,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "标签:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showTagDialog = true }) {
                    Text("选择标签")
                }
            }

            if (selectedTagIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allTags.filter { selectedTagIds.contains(it.id) }.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleTag(tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 文件夹选择区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "文件夹:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showFolderDialog = true }) {
                    Text("选择文件夹")
                }
            }

            if (selectedFolderId != null) {
                val selectedFolder = allFolders.find { it.id == selectedFolderId }
                if (selectedFolder != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.selectFolder(null) },
                            label = { Text(selectedFolder.name) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "移除")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isPreviewMode) {
                val textColor = MaterialTheme.colorScheme.onSurface
                val textColorInt = textColor.toArgb()
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            setPadding(16, 8, 16, 8)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    update = { textView ->
                        textView.setTextColor(textColorInt)
                        markwon.setMarkdown(textView, content)
                    }
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = viewModel::onContentChange,
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.large,
                        placeholder = { Text("内容 (支持Markdown)") }
                    )

                    if (isAiProcessing) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "AI处理中...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showAiMenu,
                onDismissRequest = { showAiMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("翻译成英文") },
                    onClick = {
                        viewModel.aiAssist("translate_en")
                        showAiMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("翻译成中文") },
                    onClick = {
                        viewModel.aiAssist("translate_cn")
                        showAiMenu = false
                    }
                )
            }

            // 导出菜单
            DropdownMenu(
                expanded = showExportMenu,
                onDismissRequest = { showExportMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("导出为 Markdown (.md)") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    onClick = {
                        viewModel.exportNote(context, "md")
                        showExportMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("导出为纯文本 (.txt)") },
                    leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null) },
                    onClick = {
                        viewModel.exportNote(context, "txt")
                        showExportMenu = false
                    }
                )
            }
        }
    }

    // 标签选择对话框
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("选择标签") },
            text = {
                Column {
                    if (allTags.isEmpty()) {
                        Text(
                            "暂无标签，请先创建标签",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        allTags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedTagIds.contains(tag.id),
                                    onCheckedChange = { viewModel.toggleTag(tag.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    tag.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text(
                        "确定",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTagDialog = false
                    showCreateTagDialog = true
                }) {
                    Text(
                        "新建标签",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    // 创建标签对话框
    if (showCreateTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        val primaryColor = MaterialTheme.colorScheme.primary.value
        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = { Text("新建标签") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    shape = MaterialTheme.shapes.large,
                    placeholder = { Text("标签名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.createTag(newTagName, primaryColor.toLong())
                            newTagName = ""
                            showCreateTagDialog = false
                        }
                    }
                ) {
                    Text(
                        "创建",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTagDialog = false }) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    // 文件夹选择对话框
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("选择文件夹") },
            text = {
                Column {
                    if (allFolders.isEmpty()) {
                        Text(
                            "暂无文件夹，请先创建文件夹",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        allFolders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFolderId == folder.id,
                                    onClick = { viewModel.selectFolder(folder.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    folder.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text(
                        "确定",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFolderDialog = false
                    showCreateFolderDialog = true
                }) {
                    Text(
                        "新建文件夹",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    // 创建文件夹对话框
    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    shape = MaterialTheme.shapes.large,
                    placeholder = { Text("文件夹名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName)
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text(
                        "创建",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }
}
