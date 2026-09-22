package com.toolbox.ui.ai

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.toolbox.di.PlainPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

data class AiTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class CustomPrompt(
    val id: String,
    val name: String,
    val prompt: String
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiToolsEntryPoint {
    @PlainPrefs fun prefs(): SharedPreferences
    fun gson(): Gson
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToCustomPromptManager: () -> Unit,
    onNavigateToAiToolResult: (String, String) -> Unit
) {
    var showInputDialog by remember { mutableStateOf<AiTool?>(null) }
    var showCustomPromptDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context, AiToolsEntryPoint::class.java)
    }
    val prefs = remember { entryPoint.prefs() }
    val gson = remember { entryPoint.gson() }

    var customPrompts by remember {
        mutableStateOf(loadCustomPrompts(prefs, gson))
    }

    val tools = listOf(
        AiTool("chat", "AI对话", "与AI自由对话", Icons.Default.Chat),
        AiTool("summary", "文本摘要", "提取文本要点", Icons.Default.Summarize),
        AiTool("translate", "全文翻译", "多语言翻译", Icons.Default.Translate),
        AiTool("code_explain", "代码解释", "解释代码逻辑", Icons.Default.Code),
        AiTool("code_gen", "代码生成", "自然语言生成代码", Icons.Default.Add),
        AiTool("creative", "创意助手", "头脑风暴", Icons.Default.Lightbulb),
        AiTool("calculate", "AI计算", "自然语言计算", Icons.Default.Calculate),
        AiTool("custom", "自定义Prompt", "使用自定义提示词", Icons.Default.Tune)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI工具箱") },
                actions = {
                    IconButton(onClick = onNavigateToCustomPromptManager) {
                        Icon(Icons.Default.Settings, contentDescription = "管理Prompt")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools) { tool ->
                AiToolCard(
                    tool = tool,
                    onClick = {
                        when (tool.id) {
                            "chat" -> onNavigateToChat()
                            "custom" -> showCustomPromptDialog = true
                            else -> showInputDialog = tool
                        }
                    }
                )
            }
        }
    }

    // 输入对话框
    showInputDialog?.let { tool ->
        AiToolInputDialog(
            tool = tool,
            onConfirm = { inputText ->
                val prompt = when (tool.id) {
                    "summary" -> "请总结以下内容：\n$inputText"
                    "translate" -> "请将以下内容翻译成英文：\n$inputText"
                    "code_explain" -> "请解释以下代码的逻辑：\n$inputText"
                    "code_gen" -> "请根据以下描述生成代码：\n$inputText"
                    "creative" -> "请帮我进行头脑风暴：$inputText"
                    "calculate" -> "请计算以下数学表达式并给出结果：\n$inputText"
                    else -> inputText
                }
                onNavigateToAiToolResult(tool.title, prompt)
                showInputDialog = null
            },
            onDismiss = { showInputDialog = null }
        )
    }

    // 自定义Prompt选择对话框
    if (showCustomPromptDialog) {
        CustomPromptSelectDialog(
            prompts = customPrompts,
            onSelect = { prompt ->
                showCustomPromptDialog = false
                onNavigateToAiToolResult(prompt.name, prompt.prompt)
            },
            onDismiss = { showCustomPromptDialog = false }
        )
    }
}

@Composable
fun AiToolCard(
    tool: AiTool,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AiToolInputDialog(
    tool: AiTool,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tool.title) },
        text = {
            Column {
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("输入内容") },
                    minLines = 5,
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(inputText) },
                enabled = inputText.isNotBlank()
            ) {
                Text("处理")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CustomPromptSelectDialog(
    prompts: List<CustomPrompt>,
    onSelect: (CustomPrompt) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择自定义Prompt") },
        text = {
            Column {
                if (prompts.isEmpty()) {
                    Text("暂无自定义Prompt，请先创建")
                } else {
                    prompts.forEach { prompt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(prompt) },
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = prompt.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = prompt.prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


internal fun loadCustomPrompts(prefs: SharedPreferences, gson: Gson): List<CustomPrompt> {
    val json = prefs.getString("custom_prompts", null) ?: return emptyList()
    return try {
        val type = object : TypeToken<List<CustomPrompt>>() {}.type
        gson.fromJson(json, type)
    } catch (e: Exception) {
        emptyList()
    }
}

internal fun saveCustomPrompts(prefs: SharedPreferences, gson: Gson, prompts: List<CustomPrompt>) {
    prefs.edit().putString("custom_prompts", gson.toJson(prompts)).apply()
}
