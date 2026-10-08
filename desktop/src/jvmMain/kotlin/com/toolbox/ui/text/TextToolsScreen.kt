package com.toolbox.ui.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
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
import com.toolbox.ui.text.cleaner.TextCleanScreen
import com.toolbox.ui.text.converter.TextConvertScreen
import com.toolbox.ui.text.counter.WordCountScreen
import com.toolbox.ui.text.diff.TextDiffScreen
import com.toolbox.ui.text.markdown.MarkdownPreviewScreen

data class TextTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class TextScreen {
    object Menu : TextScreen()
    object Diff : TextScreen()
    object Counter : TextScreen()
    object Converter : TextScreen()
    object Cleaner : TextScreen()
    object Markdown : TextScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<TextScreen>(TextScreen.Menu) }

    when (currentScreen) {
        TextScreen.Menu -> TextMenuScreen(
            onBack = onBack,
            onNavigate = { currentScreen = it }
        )
        TextScreen.Diff -> TextDiffScreen(
            onBack = { currentScreen = TextScreen.Menu }
        )
        TextScreen.Counter -> WordCountScreen(
            onBack = { currentScreen = TextScreen.Menu }
        )
        TextScreen.Converter -> TextConvertScreen(
            onBack = { currentScreen = TextScreen.Menu }
        )
        TextScreen.Cleaner -> TextCleanScreen(
            onBack = { currentScreen = TextScreen.Menu }
        )
        TextScreen.Markdown -> MarkdownPreviewScreen(
            onBack = { currentScreen = TextScreen.Menu }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (TextScreen) -> Unit
) {
    val tools = listOf(
        TextTool("diff", "文本对比", "双栏对比、高亮差异", Icons.Default.CompareArrows),
        TextTool("counter", "字数统计", "字符/词/行/段落统计", Icons.Default.TextFields),
        TextTool("converter", "文本转换", "大小写/命名风格/简繁转换", Icons.Default.SwapHoriz),
        TextTool("cleaner", "文本清理", "去空行/去空格/去重复", Icons.Default.CleaningServices),
        TextTool("markdown", "Markdown 预览", "实时预览、导出 HTML", Icons.Default.Preview)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文本工具") },
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
                    accent = moduleAccent(ModuleId.TEXT),
                    onClick = {
                        when (tool.id) {
                            "diff" -> onNavigate(TextScreen.Diff)
                            "counter" -> onNavigate(TextScreen.Counter)
                            "converter" -> onNavigate(TextScreen.Converter)
                            "cleaner" -> onNavigate(TextScreen.Cleaner)
                            "markdown" -> onNavigate(TextScreen.Markdown)
                        }
                    }
                )
            }
        }
    }
}
