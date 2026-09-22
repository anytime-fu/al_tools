package com.toolbox.ui.life

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.ui.navigation.Screen
import com.toolbox.ui.theme.*

data class LifeTool(
    val id: String,
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
)

private val lifeTools = listOf(
    LifeTool("qrcode", Screen.LifeQrCode, "二维码", Icons.Default.QrCode, CategoryBlue, "扫码/生成"),
    LifeTool("todo", Screen.LifeTodo, "待办清单", Icons.Default.CheckCircle, CategoryGreen, "任务管理"),
    LifeTool("level", Screen.LifeLevel, "水平仪", Icons.Default.Straighten, CategoryOrange, "水平检测"),
    LifeTool("compass", Screen.LifeCompass, "指南针", Icons.Default.Explore, CategoryTeal, "方向定位"),
    LifeTool("timer", Screen.LifeTimer, "番茄钟", Icons.Default.Timer, CategoryRed, "专注计时"),
    LifeTool("water", Screen.LifeWater, "喝水提醒", Icons.Default.WaterDrop, CategoryBlue, "健康提醒"),
    LifeTool("parking", Screen.LifeParking, "车牌记忆", Icons.Default.LocalParking, CategoryPurple, "停车位"),
    LifeTool("age", Screen.LifeAge, "年龄计算", Icons.Default.Cake, CategoryPink, "日期计算"),
    LifeTool("noise", Screen.LifeNoise, "噪音检测", Icons.Default.Mic, CategoryBrown, "分贝仪"),
    LifeTool("translator", Screen.LifeTranslator, "翻译", Icons.Default.Translate, CategoryGreen, "多语言")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeAssistantScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生活小助手") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lifeTools) { tool ->
                LifeToolItem(
                    tool = tool,
                    onClick = { onNavigate(tool.screen.route) }
                )
            }
        }
    }
}

@Composable
private fun LifeToolItem(
    tool: LifeTool,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = tool.color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.title,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = tool.description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
