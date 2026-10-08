package com.toolbox.ui.network

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.network.http.HttpClientScreen
import com.toolbox.ui.network.info.NetworkInfoScreen
import com.toolbox.ui.network.ping.PingScreen
import com.toolbox.ui.network.scanner.PortScanScreen
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

data class NetworkTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class NetworkScreen {
    object Menu : NetworkScreen()
    object Http : NetworkScreen()
    object Info : NetworkScreen()
    object Scanner : NetworkScreen()
    object Ping : NetworkScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<NetworkScreen>(NetworkScreen.Menu) }

    when (currentScreen) {
        NetworkScreen.Menu -> NetworkMenuScreen(onBack = onBack, onNavigate = { currentScreen = it })
        NetworkScreen.Http -> HttpClientScreen(onBack = { currentScreen = NetworkScreen.Menu })
        NetworkScreen.Info -> NetworkInfoScreen(onBack = { currentScreen = NetworkScreen.Menu })
        NetworkScreen.Scanner -> PortScanScreen(onBack = { currentScreen = NetworkScreen.Menu })
        NetworkScreen.Ping -> PingScreen(onBack = { currentScreen = NetworkScreen.Menu })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (NetworkScreen) -> Unit
) {
    val tools = listOf(
        NetworkTool("http", "HTTP 客户端", "API 测试、请求构建", Icons.Default.Http),
        NetworkTool("info", "网络信息", "IP、DNS、网卡信息", Icons.Default.Dns),
        NetworkTool("scanner", "端口扫描", "TCP 端口扫描、服务识别", Icons.Default.SettingsEthernet),
        NetworkTool("ping", "Ping 工具", "Ping、路由追踪", Icons.Default.NetworkCheck)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网络工具") },
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
                    accent = moduleAccent(ModuleId.NETWORK),
                    onClick = {
                        when (tool.id) {
                            "http" -> onNavigate(NetworkScreen.Http)
                            "info" -> onNavigate(NetworkScreen.Info)
                            "scanner" -> onNavigate(NetworkScreen.Scanner)
                            "ping" -> onNavigate(NetworkScreen.Ping)
                        }
                    }
                )
            }
        }
    }
}
