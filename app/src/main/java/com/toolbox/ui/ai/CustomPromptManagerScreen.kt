package com.toolbox.ui.ai

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.toolbox.di.PlainPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CustomPromptManagerEntryPoint {
    @PlainPrefs fun prefs(): SharedPreferences
    fun gson(): Gson
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPromptManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context, CustomPromptManagerEntryPoint::class.java)
    }
    val prefs = remember { entryPoint.prefs() }
    val gson = remember { entryPoint.gson() }

    var prompts by remember {
        mutableStateOf(loadCustomPrompts(prefs, gson))
    }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<CustomPrompt?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理Prompt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPrompt = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { paddingValues ->
        if (prompts.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无自定义Prompt",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角+按钮创建",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(prompts) { prompt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prompt.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prompt.prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            IconButton(onClick = {
                                editingPrompt = prompt
                                showAddEditDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "编辑",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                prompts = prompts.toMutableList().apply {
                                    remove(prompt)
                                }
                                saveCustomPrompts(prefs, gson, prompts)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showAddEditDialog) {
        var name by remember { mutableStateOf(editingPrompt?.name ?: "") }
        var promptContent by remember { mutableStateOf(editingPrompt?.prompt ?: "") }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (editingPrompt != null) "编辑Prompt" else "添加Prompt") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("名称") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = promptContent,
                        onValueChange = { promptContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Prompt内容") },
                        minLines = 3,
                        shape = MaterialTheme.shapes.large
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && promptContent.isNotBlank()) {
                            if (editingPrompt != null) {
                                prompts = prompts.toMutableList().apply {
                                    val index = indexOf(editingPrompt)
                                    if (index != -1) {
                                        set(index, CustomPrompt(editingPrompt!!.id, name, promptContent))
                                    }
                                }
                            } else {
                                prompts = prompts.toMutableList().apply {
                                    add(CustomPrompt(
                                        id = System.currentTimeMillis().toString(),
                                        name = name,
                                        prompt = promptContent
                                    ))
                                }
                            }
                            saveCustomPrompts(prefs, gson, prompts)
                            showAddEditDialog = false
                        }
                    },
                    enabled = name.isNotBlank() && promptContent.isNotBlank()
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
