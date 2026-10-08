package com.toolbox.ui.file

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent
import com.toolbox.ui.file.compare.FileCompareScreen
import com.toolbox.ui.file.dedup.FileDedupScreen
import com.toolbox.ui.file.rename.BatchRenameScreen
import com.toolbox.ui.file.search.FileSearchScreen

data class FileTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class FileScreen {
    object Menu : FileScreen()
    object Search : FileScreen()
    object Rename : FileScreen()
    object Compare : FileScreen()
    object Dedup : FileScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<FileScreen>(FileScreen.Menu) }

    when (currentScreen) {
        FileScreen.Menu -> FileMenuScreen(
            onBack = onBack,
            onNavigate = { currentScreen = it }
        )
        FileScreen.Search -> FileSearchScreen(
            onBack = { currentScreen = FileScreen.Menu }
        )
        FileScreen.Rename -> BatchRenameScreen(
            onBack = { currentScreen = FileScreen.Menu }
        )
        FileScreen.Compare -> FileCompareScreen(
            onBack = { currentScreen = FileScreen.Menu }
        )
        FileScreen.Dedup -> FileDedupScreen(
            onBack = { currentScreen = FileScreen.Menu }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (FileScreen) -> Unit
) {
    val tools = listOf(
        FileTool("search", "文件搜索", "按文件名/内容搜索", Icons.Default.Search),
        FileTool("rename", "批量重命名", "规则重命名、支持预览", Icons.Default.DriveFileRenameOutline),
        FileTool("compare", "文件对比", "文本/二进制文件对比", Icons.Default.CompareArrows),
        FileTool("dedup", "文件去重", "MD5 查找重复文件", Icons.Default.DeleteSweep)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件工具") },
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
                    accent = moduleAccent(ModuleId.FILE),
                    onClick = {
                        when (tool.id) {
                            "search" -> onNavigate(FileScreen.Search)
                            "rename" -> onNavigate(FileScreen.Rename)
                            "compare" -> onNavigate(FileScreen.Compare)
                            "dedup" -> onNavigate(FileScreen.Dedup)
                        }
                    }
                )
            }
        }
    }
}
