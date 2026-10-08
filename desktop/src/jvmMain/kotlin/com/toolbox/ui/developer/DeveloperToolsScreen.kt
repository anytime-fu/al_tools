package com.toolbox.ui.developer

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

data class DeveloperTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class DeveloperScreen {
    object Menu : DeveloperScreen()
    object Json : DeveloperScreen()
    object Base64 : DeveloperScreen()
    object Url : DeveloperScreen()
    object Timestamp : DeveloperScreen()
    object Hash : DeveloperScreen()
    object Regex : DeveloperScreen()
    object Uuid : DeveloperScreen()
    object Color : DeveloperScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<DeveloperScreen>(DeveloperScreen.Menu) }

    when (currentScreen) {
        DeveloperScreen.Menu -> DeveloperMenuScreen(
            onBack = onBack,
            onNavigate = { currentScreen = it }
        )
        DeveloperScreen.Json -> JsonFormatScreen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
        DeveloperScreen.Base64 -> Base64Screen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
        DeveloperScreen.Url -> UrlEncodeScreen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
        DeveloperScreen.Timestamp -> TimestampScreen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
        DeveloperScreen.Hash -> HashScreen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
        DeveloperScreen.Regex -> RegexScreen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
        DeveloperScreen.Uuid -> UuidScreen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
        DeveloperScreen.Color -> ColorScreen(
            onBack = { currentScreen = DeveloperScreen.Menu }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (DeveloperScreen) -> Unit
) {
    val tools = listOf(
        DeveloperTool("json", "JSON 格式化", "美化、压缩、验证 JSON", Icons.Default.DataObject),
        DeveloperTool("base64", "Base64 编解码", "文本/文件 Base64 编解码", Icons.Default.Code),
        DeveloperTool("url", "URL 编解码", "URL 编码/解码", Icons.Default.Link),
        DeveloperTool("timestamp", "时间戳工具", "时间戳与日期互转", Icons.Default.AccessTime),
        DeveloperTool("hash", "哈希计算", "MD5/SHA1/SHA256", Icons.Default.Fingerprint),
        DeveloperTool("regex", "正则测试", "正则表达式实时匹配", Icons.Default.TextFields),
        DeveloperTool("uuid", "UUID 生成", "批量生成 UUID", Icons.Default.Tag),
        DeveloperTool("color", "颜色工具", "颜色选择与格式转换", Icons.Default.Palette)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("开发者工具") },
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
                    accent = moduleAccent(ModuleId.DEVELOPER),
                    onClick = {
                        when (tool.id) {
                            "json" -> onNavigate(DeveloperScreen.Json)
                            "base64" -> onNavigate(DeveloperScreen.Base64)
                            "url" -> onNavigate(DeveloperScreen.Url)
                            "timestamp" -> onNavigate(DeveloperScreen.Timestamp)
                            "hash" -> onNavigate(DeveloperScreen.Hash)
                            "regex" -> onNavigate(DeveloperScreen.Regex)
                            "uuid" -> onNavigate(DeveloperScreen.Uuid)
                            "color" -> onNavigate(DeveloperScreen.Color)
                        }
                    }
                )
            }
        }
    }
}