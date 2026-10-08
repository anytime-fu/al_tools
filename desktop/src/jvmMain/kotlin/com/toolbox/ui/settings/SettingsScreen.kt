package com.toolbox.ui.settings

import com.toolbox.ui.components.appClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.toolbox.data.local.CryptoManager
import com.toolbox.data.remote.AiConfig
import com.toolbox.data.remote.AiConfigManager
import com.toolbox.data.repository.SettingsRepository
import com.toolbox.ui.theme.ThemeController
import com.toolbox.ui.theme.ThemeScheme
import com.toolbox.ui.uninstall.UninstallScreen
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val coroutineScope = rememberCoroutineScope()
    
    val currentConfig by AiConfigManager.config.collectAsState()
    
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var selectedModel by remember { mutableStateOf(currentConfig.model) }
    var customApiUrl by remember { mutableStateOf(currentConfig.customApiUrl ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    var showUninstall by remember { mutableStateOf(false) }
    
    val models = listOf(
        "gemini-2.0-flash",
        "gemini-1.5-pro",
        "gemini-1.5-flash",
        "deepseek-chat",
        "deepseek-coder",
        "deepseek-reasoner",
        "claude-3-5-sonnet",
        "claude-3-opus",
        "claude-3-haiku"
    )
    
    // Load settings from database
    LaunchedEffect(Unit) {
        val savedApiKey = CryptoManager.decrypt(settingsRepository.getSettingValue("api_key", ""))
        val savedModel = settingsRepository.getSettingValue("ai_model", "gemini-2.0-flash")
        val savedApiUrl = settingsRepository.getSettingValue("custom_api_url", "")
        
        apiKey = savedApiKey
        selectedModel = savedModel
        customApiUrl = savedApiUrl
        
        // Update AiConfigManager
        AiConfigManager.updateConfig(
            AiConfig(
                apiKey = savedApiKey,
                model = savedModel,
                customApiUrl = savedApiUrl.takeIf { it.isNotBlank() }
            )
        )
        
        isLoaded = true
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Theme Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "主题配置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "配色方案",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "切换后立即生效，自动保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val currentScheme = ThemeController.scheme.value
                val darkMode = ThemeController.darkMode.value
                
                ThemeScheme.values().forEach { scheme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appClickable { ThemeController.setScheme(scheme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentScheme == scheme,
                            onClick = { ThemeController.setScheme(scheme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scheme.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = scheme.keyword,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val mode = if (darkMode) scheme.palette.dark else scheme.palette.light
                            listOf(
                                mode.primary,
                                mode.bg,
                                mode.card,
                                mode.textPrimary,
                                mode.border
                            ).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(c, RoundedCornerShape(4.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "深色模式",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "浅色 / 深色外观切换",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { ThemeController.setDarkMode(it) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // AI Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "AI 配置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    placeholder = { Text("输入你的API Key") },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showApiKey) "隐藏" else "显示",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Model selection
                Text(
                    text = "选择模型",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "可从预置列表选择，也可手动输入任意模型名（配合自定义 API 地址接入任意网关）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("模型名称") },
                        placeholder = { Text("选择预置模型或输入自定义模型名") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    selectedModel = model
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Custom API URL
                OutlinedTextField(
                    value = customApiUrl,
                    onValueChange = { customApiUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义API地址 (可选)") },
                    placeholder = { Text("https://api.example.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Save button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Save to database
                            settingsRepository.saveSetting("api_key", CryptoManager.encrypt(apiKey))
                            settingsRepository.saveSetting("ai_model", selectedModel)
                            settingsRepository.saveSetting("custom_api_url", customApiUrl)
                            
                            // Update AiConfigManager
                            AiConfigManager.updateConfig(
                                AiConfig(
                                    apiKey = apiKey,
                                    model = selectedModel,
                                    customApiUrl = customApiUrl.takeIf { it.isNotBlank() }
                                )
                            )
                            
                            showSaveSuccess = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存设置")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // About
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "AI 工具箱 v1.0.0",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "一款隐私优先、离线可用的多功能工具箱应用",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "支持的AI模型:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "• Google Gemini",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "• DeepSeek",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "• Claude",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showUninstall = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("卸载应用", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    
    // Success message
    if (showSaveSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSaveSuccess = false
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {
            Text("设置已保存")
        }
    }
    
    if (showUninstall) {
        Dialog(onDismissRequest = { showUninstall = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                UninstallScreen(
                    onKeep = { showUninstall = false },
                    onUninstallRequested = {
                        Thread {
                            Thread.sleep(1500)
                            kotlin.system.exitProcess(0)
                        }.start()
                    }
                )
            }
        }
    }
}
