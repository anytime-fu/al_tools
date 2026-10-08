package com.toolbox.ui.productivity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.productivity.clipboard.ClipboardToolsScreen
import com.toolbox.ui.productivity.hotkey.HotkeyScreen
import com.toolbox.ui.productivity.kanban.KanbanScreen
import com.toolbox.ui.productivity.pomodoro.PomodoroScreen
import com.toolbox.ui.productivity.sticky.StickyNotesScreen
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

data class ProductivityTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class ProductivityScreen {
    object Menu : ProductivityScreen()
    object Sticky : ProductivityScreen()
    object Pomodoro : ProductivityScreen()
    object Kanban : ProductivityScreen()
    object Hotkey : ProductivityScreen()
    object Clipboard : ProductivityScreen()
}

@Composable
fun ProductivityToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<ProductivityScreen>(ProductivityScreen.Menu) }

    when (currentScreen) {
        ProductivityScreen.Menu -> ProductivityMenuScreen(onBack = onBack, onNavigate = { currentScreen = it })
        ProductivityScreen.Sticky -> StickyNotesScreen(onBack = { currentScreen = ProductivityScreen.Menu })
        ProductivityScreen.Pomodoro -> PomodoroScreen(onBack = { currentScreen = ProductivityScreen.Menu })
        ProductivityScreen.Kanban -> KanbanScreen(onBack = { currentScreen = ProductivityScreen.Menu })
        ProductivityScreen.Hotkey -> HotkeyScreen(onBack = { currentScreen = ProductivityScreen.Menu })
        ProductivityScreen.Clipboard -> ClipboardToolsScreen(onBack = { currentScreen = ProductivityScreen.Menu })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductivityMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (ProductivityScreen) -> Unit
) {
    val tools = listOf(
        ProductivityTool("sticky", "便签管理", "多色便签、分组、置顶", Icons.Default.StickyNote2),
        ProductivityTool("pomodoro", "番茄钟", "专注计时、统计报表", Icons.Default.Timer),
        ProductivityTool("kanban", "任务看板", "看板式任务、拖拽排序", Icons.Default.Dashboard),
        ProductivityTool("hotkey", "快捷键管理", "全局快捷键、自定义映射", Icons.Default.Keyboard),
        ProductivityTool("clipboard", "剪贴板工具", "扩展历史、格式转换", Icons.Default.ContentPaste)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生产力工具") },
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
                    accent = moduleAccent(ModuleId.PRODUCTIVITY),
                    onClick = {
                        when (tool.id) {
                            "sticky" -> onNavigate(ProductivityScreen.Sticky)
                            "pomodoro" -> onNavigate(ProductivityScreen.Pomodoro)
                            "kanban" -> onNavigate(ProductivityScreen.Kanban)
                            "hotkey" -> onNavigate(ProductivityScreen.Hotkey)
                            "clipboard" -> onNavigate(ProductivityScreen.Clipboard)
                        }
                    }
                )
            }
        }
    }
}
