package com.toolbox.ui.image

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.image.compress.ImageCompressScreen
import com.toolbox.ui.image.converter.ImageConvertScreen
import com.toolbox.ui.image.editor.ImageCropScreen
import com.toolbox.ui.image.editor.WatermarkScreen
import com.toolbox.ui.image.restore.AiRestoreScreen
import com.toolbox.ui.image.screenshot.ColorPickerScreen
import com.toolbox.ui.image.screenshot.ScreenshotScreen
import com.toolbox.ui.image.viewer.ImageViewerScreen
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

data class ImageTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class ImageScreen {
    object Menu : ImageScreen()
    object Viewer : ImageScreen()
    object Compress : ImageScreen()
    object Converter : ImageScreen()
    object Crop : ImageScreen()
    object Watermark : ImageScreen()
    object AiRestore : ImageScreen()
    object MosaicRestore : ImageScreen()
    object Screenshot : ImageScreen()
    object ColorPicker : ImageScreen()
}

@Composable
fun ImageToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<ImageScreen>(ImageScreen.Menu) }

    when (currentScreen) {
        ImageScreen.Menu -> ImageMenuScreen(onBack = onBack, onNavigate = { currentScreen = it })
        ImageScreen.Viewer -> ImageViewerScreen(onBack = { currentScreen = ImageScreen.Menu })
        ImageScreen.Compress -> ImageCompressScreen(onBack = { currentScreen = ImageScreen.Menu })
        ImageScreen.Converter -> ImageConvertScreen(onBack = { currentScreen = ImageScreen.Menu })
        ImageScreen.Crop -> ImageCropScreen(onBack = { currentScreen = ImageScreen.Menu })
        ImageScreen.Watermark -> WatermarkScreen(onBack = { currentScreen = ImageScreen.Menu })
        ImageScreen.AiRestore -> AiRestoreScreen(onBack = { currentScreen = ImageScreen.Menu }, mosaicPreset = false)
        ImageScreen.MosaicRestore -> AiRestoreScreen(onBack = { currentScreen = ImageScreen.Menu }, mosaicPreset = true)
        ImageScreen.Screenshot -> ScreenshotScreen(onBack = { currentScreen = ImageScreen.Menu })
        ImageScreen.ColorPicker -> ColorPickerScreen(onBack = { currentScreen = ImageScreen.Menu })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (ImageScreen) -> Unit
) {
    val tools = listOf(
        ImageTool("viewer", "图片查看器", "浏览、缩放、旋转", Icons.Default.ZoomIn),
        ImageTool("compress", "图片压缩", "批量压缩、质量/尺寸调整", Icons.Default.Compress),
        ImageTool("converter", "格式转换", "PNG/JPG/WebP/BMP 互转", Icons.Default.SwapHoriz),
        ImageTool("crop", "图片裁剪", "可视裁剪、固定比例裁剪", Icons.Default.Crop),
        ImageTool("watermark", "水印工具", "文字/图片水印、批量添加", Icons.Default.BrandingWatermark),
        ImageTool("ai_restore", "AI 去水印", "标记区域智能修复", Icons.Default.AutoFixHigh),
        ImageTool("mosaic_restore", "AI 打码修复", "马赛克检测与智能修复", Icons.Default.GridOff),
        ImageTool("screenshot", "屏幕截图", "全屏截图、区域截图", Icons.Default.Screenshot),
        ImageTool("colorpicker", "取色器", "屏幕取色、颜色代码复制", Icons.Default.Colorize)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图像工具") },
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
                    accent = moduleAccent(ModuleId.IMAGE),
                    onClick = {
                        when (tool.id) {
                            "viewer" -> onNavigate(ImageScreen.Viewer)
                            "compress" -> onNavigate(ImageScreen.Compress)
                            "converter" -> onNavigate(ImageScreen.Converter)
                            "crop" -> onNavigate(ImageScreen.Crop)
                            "watermark" -> onNavigate(ImageScreen.Watermark)
                            "ai_restore" -> onNavigate(ImageScreen.AiRestore)
                            "mosaic_restore" -> onNavigate(ImageScreen.MosaicRestore)
                            "screenshot" -> onNavigate(ImageScreen.Screenshot)
                            "colorpicker" -> onNavigate(ImageScreen.ColorPicker)
                        }
                    }
                )
            }
        }
    }
}
