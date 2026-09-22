package com.toolbox.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toolbox.ui.components.AppCard
import com.toolbox.ui.components.AppToolItem
import com.toolbox.ui.navigation.Screen
import com.toolbox.ui.theme.CategoryBlue
import com.toolbox.ui.theme.CategoryBrown
import com.toolbox.ui.theme.CategoryGreen
import com.toolbox.ui.theme.CategoryOrange
import com.toolbox.ui.theme.CategoryPink
import com.toolbox.ui.theme.CategoryPurple
import com.toolbox.ui.theme.CategoryRed
import com.toolbox.ui.theme.CategoryTeal
import com.toolbox.ui.theme.Primary
import java.text.SimpleDateFormat
import java.util.*

data class ToolItem(
    val id: String,
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

private val defaultTools = mapOf(
    "note" to ToolItem("note", Screen.Note, "笔记", Icons.Default.Note, CategoryBlue),
    "document" to ToolItem("document", Screen.Document, "文档", Icons.Default.Description, CategoryGreen),
    "ai_tools" to ToolItem("ai_tools", Screen.AiTools, "AI工具", Icons.Default.SmartToy, CategoryPurple),
    "calculator" to ToolItem("calculator", Screen.Calculator, "计算", Icons.Default.Calculate, CategoryOrange),
    "bmi" to ToolItem("bmi", Screen.Bmi, "BMI", Icons.Default.MonitorWeight, CategoryTeal),
    "schedule" to ToolItem("schedule", Screen.Schedule, "日程", Icons.Default.CalendarMonth, CategoryPink),
    "personal" to ToolItem("personal", Screen.Personal, "个人", Icons.Default.Person, CategoryBrown),
    "id_card" to ToolItem("id_card", Screen.IdCard, "身份证", Icons.Default.CreditCard, CategoryRed),
    "password" to ToolItem("password", Screen.Password, "密码", Icons.Default.Lock, CategoryPurple),
    "ar_measurement" to ToolItem("ar_measurement", Screen.ArMeasurement, "AR测量", Icons.Default.Straighten, CategoryTeal),
    "life_assistant" to ToolItem("life_assistant", Screen.LifeAssistant, "生活小助手", Icons.Default.Favorite, CategoryPink)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentNotes by viewModel.recentNotes.collectAsState(initial = emptyList())
    val toolOrder by viewModel.toolOrder.collectAsState()
    var isReorderMode by remember { mutableStateOf(false) }

    val tools = toolOrder.mapNotNull { id -> defaultTools[id] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI工具箱") },
                actions = {
                    IconButton(onClick = { isReorderMode = !isReorderMode }) {
                        Icon(
                            if (isReorderMode) Icons.Default.Done else Icons.Default.DragHandle,
                            contentDescription = if (isReorderMode) "完成排序" else "调整顺序"
                        )
                    }
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.AiChat.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI对话")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 工具网格标题
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "功能",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isReorderMode) {
                        Text(
                            text = "拖拽调整顺序",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W400,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 工具网格
            item {
                val rows = (tools.size + 2) / 3
                val gridHeight = with(LocalDensity.current) {
                    val cellSizeDp = ((LocalConfiguration.current.screenWidthDp.dp - 16.dp * 2 - 12.dp * 2) / 3)
                    val totalHeight = cellSizeDp * rows + 12.dp * (rows - 1)
                    totalHeight + 8.dp
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(tools.size) { index ->
                        val tool = tools[index]
                        AppToolItem(
                            icon = tool.icon,
                            title = tool.title,
                            onClick = {
                                if (isReorderMode) {
                                    if (index > 0) {
                                        viewModel.reorderTools(index, index - 1)
                                    }
                                } else {
                                    onNavigate(tool.screen.route)
                                }
                            },
                            iconTint = tool.color
                        )
                    }
                }
            }

            // 间距
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 最近笔记标题
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近笔记",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { onNavigate(Screen.Note.route) }) {
                        Text("查看全部")
                    }
                }
            }

            // 最近笔记列表
            if (recentNotes.isEmpty()) {
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无笔记，点击+开始创建",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W400,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentNotes.size) { index ->
                    val note = recentNotes[index]
                    NoteQuickCard(
                        note = note,
                        onClick = { onNavigate(Screen.NoteDetail.createRoute(note.id)) }
                    )
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun NoteQuickCard(
    note: com.toolbox.data.local.entity.Note,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = note.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content.take(100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDate(note.updatedAt),
                fontSize = 11.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
