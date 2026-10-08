package com.toolbox.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.security.encrypt.FileEncryptScreen
import com.toolbox.ui.security.generator.PasswordGeneratorScreen
import com.toolbox.ui.security.strength.PasswordStrengthScreen
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

data class SecurityTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class SecurityScreen {
    object Menu : SecurityScreen()
    object Generator : SecurityScreen()
    object Encrypt : SecurityScreen()
    object Strength : SecurityScreen()
}

@Composable
fun SecurityToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<SecurityScreen>(SecurityScreen.Menu) }

    when (currentScreen) {
        SecurityScreen.Menu -> SecurityMenuScreen(onBack = onBack, onNavigate = { currentScreen = it })
        SecurityScreen.Generator -> PasswordGeneratorScreen(onBack = { currentScreen = SecurityScreen.Menu })
        SecurityScreen.Encrypt -> FileEncryptScreen(onBack = { currentScreen = SecurityScreen.Menu })
        SecurityScreen.Strength -> PasswordStrengthScreen(onBack = { currentScreen = SecurityScreen.Menu })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (SecurityScreen) -> Unit
) {
    val tools = listOf(
        SecurityTool("generator", "密码生成器", "密码强度、批量生成", Icons.Default.VpnKey),
        SecurityTool("encrypt", "文件加密", "AES 加密/解密文件", Icons.Default.Lock),
        SecurityTool("strength", "密码强度计", "可视化强度、改进建议", Icons.Default.Speed)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("安全工具") },
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
                    accent = moduleAccent(ModuleId.SECURITY),
                    onClick = {
                        when (tool.id) {
                            "generator" -> onNavigate(SecurityScreen.Generator)
                            "encrypt" -> onNavigate(SecurityScreen.Encrypt)
                            "strength" -> onNavigate(SecurityScreen.Strength)
                        }
                    }
                )
            }
        }
    }
}
