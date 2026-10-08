package com.toolbox.ui.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.ui.ai.AiChatScreen
import com.toolbox.ui.calculator.CalculatorScreen
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.data.DataToolsScreen
import com.toolbox.ui.developer.DeveloperToolsScreen
import com.toolbox.ui.file.FileToolsScreen
import com.toolbox.ui.image.ImageToolsScreen
import com.toolbox.ui.productivity.ProductivityToolsScreen
import com.toolbox.ui.security.SecurityToolsScreen
import com.toolbox.ui.note.NoteScreen
import com.toolbox.ui.password.PasswordScreen
import com.toolbox.ui.schedule.ScheduleScreen
import com.toolbox.ui.network.NetworkToolsScreen
import com.toolbox.ui.system.SystemToolsScreen
import com.toolbox.ui.testgen.TestDataToolsScreen
import com.toolbox.ui.text.TextToolsScreen
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

private enum class OverviewModule(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val moduleId: ModuleId,
    val hasSub: Boolean
) {
    Note("笔记", "Markdown 编辑、标签分类、文件管理", Icons.Default.Description, ModuleId.NOTE, true),
    Ai("AI", "AI 对话、智能助手", Icons.Default.SmartToy, ModuleId.AI, true),
    Calculator("计算器", "科学计算器", Icons.Default.Calculate, ModuleId.CALCULATOR, false),
    Password("密码", "密码管理、安全存储", Icons.Default.Lock, ModuleId.PASSWORD, true),
    Schedule("日程", "日程管理、待办提醒", Icons.Default.CalendarMonth, ModuleId.SCHEDULE, true),
    Text("文本", "对比、统计、转换、清理、预览", Icons.Default.Article, ModuleId.TEXT, false),
    Data("数据", "CSV、JSON、XML、YAML、图表、数据库", Icons.Default.Storage, ModuleId.DATA, true),
    File("文件", "搜索、重命名、对比、去重", Icons.Default.FolderOpen, ModuleId.FILE, false),
    Developer("开发者", "JSON、Base64、哈希、正则等", Icons.Default.Code, ModuleId.DEVELOPER, true),
    Network("网络", "HTTP、DNS、端口扫描、Ping", Icons.Default.Wifi, ModuleId.NETWORK, true),
    System("系统", "系统信息、进程、剪贴板、启动", Icons.Default.Computer, ModuleId.SYSTEM, true),
    Image("图像", "查看、压缩、转换、裁剪、水印、截图", Icons.Default.Landscape, ModuleId.IMAGE, true),
    Productivity("生产力", "便签、番茄钟、看板、剪贴板、快捷键", Icons.Default.FlashOn, ModuleId.PRODUCTIVITY, true),
    Security("安全", "密码生成、文件加密、强度检测", Icons.Default.Security, ModuleId.SECURITY, true),
    TestGen("测试数据", "占位文本、身份证、个人资料、银行卡", Icons.Default.Casino, ModuleId.TESTGEN, true)
}

@Composable
fun OverviewScreen() {
    var module by remember { mutableStateOf<OverviewModule?>(null) }
    val onBack: () -> Unit = { module = null }

    when (module) {
        null -> OverviewMenu(onSelect = { module = it })
        OverviewModule.Note -> NoteScreen(onBack = onBack)
        OverviewModule.Ai -> AiChatScreen(onBack = onBack)
        OverviewModule.Calculator -> CalculatorScreen(onBack = onBack)
        OverviewModule.Password -> PasswordScreen(onBack = onBack)
        OverviewModule.Schedule -> ScheduleScreen(onBack = onBack)
        OverviewModule.Text -> TextToolsScreen(onBack = onBack)
        OverviewModule.Data -> DataToolsScreen(onBack = onBack)
        OverviewModule.File -> FileToolsScreen(onBack = onBack)
        OverviewModule.Developer -> DeveloperToolsScreen(onBack = onBack)
        OverviewModule.Network -> NetworkToolsScreen(onBack = onBack)
        OverviewModule.System -> SystemToolsScreen(onBack = onBack)
        OverviewModule.Image -> ImageToolsScreen(onBack = onBack)
        OverviewModule.Productivity -> ProductivityToolsScreen(onBack = onBack)
        OverviewModule.Security -> SecurityToolsScreen(onBack = onBack)
        OverviewModule.TestGen -> TestDataToolsScreen(onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewMenu(onSelect: (OverviewModule) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("总览") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(OverviewModule.values()) { m ->
                ToolGridCard(
                    title = m.title,
                    description = m.description,
                    icon = m.icon,
                    accent = moduleAccent(m.moduleId),
                    hasSub = m.hasSub,
                    onClick = { onSelect(m) }
                )
            }
        }
    }
}
