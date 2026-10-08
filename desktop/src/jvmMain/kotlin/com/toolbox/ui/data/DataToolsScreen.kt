package com.toolbox.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent
import com.toolbox.ui.data.csv.CsvViewerScreen
import com.toolbox.ui.data.converter.JsonCsvConverterScreen
import com.toolbox.ui.data.chart.DataChartScreen
import com.toolbox.ui.data.database.DatabaseViewerScreen
import com.toolbox.ui.data.xml.XmlFormatScreen
import com.toolbox.ui.data.yaml.YamlFormatScreen

data class DataTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class DataScreen {
    object Menu : DataScreen()
    object Csv : DataScreen()
    object JsonCsv : DataScreen()
    object Xml : DataScreen()
    object Yaml : DataScreen()
    object Chart : DataScreen()
    object Database : DataScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<DataScreen>(DataScreen.Menu) }

    when (currentScreen) {
        DataScreen.Menu -> DataMenuScreen(onBack = onBack, onNavigate = { currentScreen = it })
        DataScreen.Csv -> CsvViewerScreen(onBack = { currentScreen = DataScreen.Menu })
        DataScreen.JsonCsv -> JsonCsvConverterScreen(onBack = { currentScreen = DataScreen.Menu })
        DataScreen.Xml -> XmlFormatScreen(onBack = { currentScreen = DataScreen.Menu })
        DataScreen.Yaml -> YamlFormatScreen(onBack = { currentScreen = DataScreen.Menu })
        DataScreen.Chart -> DataChartScreen(onBack = { currentScreen = DataScreen.Menu })
        DataScreen.Database -> DatabaseViewerScreen(onBack = { currentScreen = DataScreen.Menu })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataMenuScreen(onBack: (() -> Unit)?, onNavigate: (DataScreen) -> Unit) {
    val tools = listOf(
        DataTool("csv", "CSV 查看器", "表格展示、筛选、排序、搜索", Icons.Default.TableChart),
        DataTool("jsoncsv", "JSON↔CSV", "JSON 与 CSV 互转", Icons.Default.Transform),
        DataTool("xml", "XML 格式化", "美化、压缩、XPath 查询", Icons.Default.Code),
        DataTool("yaml", "YAML 格式化", "格式化、验证、转换", Icons.Default.Tune),
        DataTool("chart", "数据可视化", "柱状图、折线图、饼图", Icons.Default.BarChart),
        DataTool("database", "数据库查看器", "SQLite 浏览、SQL 查询", Icons.Default.Storage)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据工具") },
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
                    accent = moduleAccent(ModuleId.DATA),
                    onClick = {
                        when (tool.id) {
                            "csv" -> onNavigate(DataScreen.Csv)
                            "jsoncsv" -> onNavigate(DataScreen.JsonCsv)
                            "xml" -> onNavigate(DataScreen.Xml)
                            "yaml" -> onNavigate(DataScreen.Yaml)
                            "chart" -> onNavigate(DataScreen.Chart)
                            "database" -> onNavigate(DataScreen.Database)
                        }
                    }
                )
            }
        }
    }
}
