package com.toolbox.ui.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.system.clipboard.ClipboardHistoryScreen
import com.toolbox.ui.system.info.SystemInfoScreen
import com.toolbox.ui.system.launcher.QuickLaunchScreen
import com.toolbox.ui.system.process.ProcessScreen
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

data class SystemTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class SystemScreen {
    object Menu : SystemScreen()
    object Info : SystemScreen()
    object Process : SystemScreen()
    object Clipboard : SystemScreen()
    object Launcher : SystemScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<SystemScreen>(SystemScreen.Menu) }

    when (currentScreen) {
        SystemScreen.Menu -> SystemMenuScreen(onBack = onBack, onNavigate = { currentScreen = it })
        SystemScreen.Info -> SystemInfoScreen(onBack = { currentScreen = SystemScreen.Menu })
        SystemScreen.Process -> ProcessScreen(onBack = { currentScreen = SystemScreen.Menu })
        SystemScreen.Clipboard -> ClipboardHistoryScreen(onBack = { currentScreen = SystemScreen.Menu })
        SystemScreen.Launcher -> QuickLaunchScreen(onBack = { currentScreen = SystemScreen.Menu })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (SystemScreen) -> Unit
) {
    val tools = listOf(
        SystemTool("info", "系统信息", "CPU、内存、磁盘、网络", Icons.Default.Computer),
        SystemTool("process", "进程管理", "进程列表、结束进程", Icons.Default.ListAlt),
        SystemTool("clipboard", "剪贴板历史", "历史记录、快速粘贴", Icons.Default.ContentPaste),
        SystemTool("launcher", "快捷启动", "应用/文件/URL 快速启动", Icons.Default.Apps)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统工具") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
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
            items(tools) { tool ->
                ToolGridCard(
                    title = tool.title,
                    description = tool.description,
                    icon = tool.icon,
                    accent = moduleAccent(ModuleId.SYSTEM),
                    onClick = {
                        when (tool.id) {
                            "info" -> onNavigate(SystemScreen.Info)
                            "process" -> onNavigate(SystemScreen.Process)
                            "clipboard" -> onNavigate(SystemScreen.Clipboard)
                            "launcher" -> onNavigate(SystemScreen.Launcher)
                        }
                    }
                )
            }
        }
    }
}
