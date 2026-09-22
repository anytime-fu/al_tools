package com.toolbox.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val customApiUrl by viewModel.customApiUrl.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val restoreStatus by viewModel.restoreStatus.collectAsState()
    val cacheCleared by viewModel.cacheCleared.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showCustomApiUrlDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.backupData(uri)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.restoreData(uri)
            }
        }
    }

    LaunchedEffect(backupStatus) {
        if (backupStatus != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearBackupStatus()
        }
    }

    LaunchedEffect(restoreStatus) {
        if (restoreStatus != null) {
            if (restoreStatus!!.contains("成功")) {
                kotlinx.coroutines.delay(1500)
                // 杀死当前进程，系统会自动重启应用
                android.os.Process.killProcess(android.os.Process.myPid())
            } else {
                kotlinx.coroutines.delay(3000)
                viewModel.clearRestoreStatus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "AI设置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("API Key") },
                supportingContent = {
                    Text(
                        text = if (apiKey.isNotEmpty()) "已配置" else "未配置",
                        color = if (apiKey.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = { Icon(Icons.Default.Key, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                }
            )

            Divider()

            var modelExpanded by remember { mutableStateOf(false) }
            val models = listOf(
                "gemini-pro",           // Google Gemini
                "gemini-pro-vision",    // Google Gemini (支持图片)
                "deepseek-chat",        // DeepSeek 通用对话
                "deepseek-coder",       // DeepSeek 代码专用
                "deepseek-reasoner",    // DeepSeek 推理专用
                "claude-3-sonnet",      // Claude 3 Sonnet
                "claude-3-haiku"        // Claude 3 Haiku
            )

            ListItem(
                headlineContent = { Text("AI模型") },
                supportingContent = { Text(selectedModel) },
                leadingContent = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                modifier = Modifier.clickable { modelExpanded = true }
            )

            DropdownMenu(
                expanded = modelExpanded,
                onDismissRequest = { modelExpanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            viewModel.onModelChange(model)
                            modelExpanded = false
                        }
                    )
                }
            }

            Divider()

            ListItem(
                headlineContent = { Text("自定义API地址") },
                supportingContent = {
                    Text(
                        text = if (customApiUrl.isNotEmpty()) customApiUrl else "默认 (Google Gemini)",
                        color = if (customApiUrl.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = { Icon(Icons.Default.Link, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { showCustomApiUrlDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                }
            )

            Text(
                text = "外观",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            var darkModeExpanded by remember { mutableStateOf(false) }
            val darkModeOptions = listOf("跟随系统", "浅色", "深色")

            ListItem(
                headlineContent = { Text("主题") },
                supportingContent = { Text(darkModeOptions[darkMode]) },
                leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                modifier = Modifier.clickable { darkModeExpanded = true }
            )

            DropdownMenu(
                expanded = darkModeExpanded,
                onDismissRequest = { darkModeExpanded = false }
            ) {
                darkModeOptions.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.onDarkModeChange(index)
                            darkModeExpanded = false
                        }
                    )
                }
            }

            Divider()

            Text(
                text = "数据",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("备份") },
                supportingContent = { 
                    Column {
                        Text("导出数据库")
                        if (backupStatus != null) {
                            Text(
                                text = backupStatus!!,
                                color = if (backupStatus!!.contains("成功"))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                leadingContent = { Icon(Icons.Default.Backup, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip"
                        putExtra(Intent.EXTRA_TITLE, "toolbox_backup.zip")
                    }
                    backupLauncher.launch(intent)
                }
            )

            Divider()

            ListItem(
                headlineContent = { Text("恢复") },
                supportingContent = {
                    Column {
                        Text("导入数据库")
                        if (restoreStatus != null) {
                            Text(
                                text = restoreStatus!!,
                                color = if (restoreStatus!!.contains("成功"))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                leadingContent = { Icon(Icons.Default.Restore, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip"
                    }
                    restoreLauncher.launch(intent)
                }
            )

            Divider()

            ListItem(
                headlineContent = { Text("重置数据库") },
                supportingContent = {
                    Text("数据库损坏时使用，会清除所有数据")
                },
                leadingContent = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable {
                    viewModel.resetDatabase()
                }
            )

            Divider()

            ListItem(
                headlineContent = { Text("清除缓存") },
                supportingContent = {
                    Column {
                        Text("清除应用缓存")
                        if (cacheCleared) {
                            Text(
                                text = "缓存已清除",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                modifier = Modifier.clickable {
                    viewModel.clearCache()
                }
            )

            Divider()

            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            ListItem(
                headlineContent = { Text("版本") },
                supportingContent = { Text("1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
    }

    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(apiKey) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("配置 API Key") },
            text = {
                Column {
                    Text("输入你的AI API Key")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onApiKeyChange(tempKey)
                        showApiKeyDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCustomApiUrlDialog) {
        var tempUrl by remember { mutableStateOf(customApiUrl) }

        AlertDialog(
            onDismissRequest = { showCustomApiUrlDialog = false },
            title = { Text("自定义API地址") },
            text = {
                Column {
                    Text("留空则使用默认Google Gemini API")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "示例: https://api.example.com/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Base URL") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onCustomApiUrlChange(tempUrl)
                        showCustomApiUrlDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomApiUrlDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
