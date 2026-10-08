package com.toolbox.ui.testgen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import com.toolbox.ui.components.ToolGridCard
import com.toolbox.ui.theme.ModuleId
import com.toolbox.ui.theme.moduleAccent

private data class TestGenTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

sealed class TestGenScreen {
    object Menu : TestGenScreen()
    object Placeholder : TestGenScreen()
    object IdCard : TestGenScreen()
    object Person : TestGenScreen()
    object BankCard : TestGenScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDataToolsScreen(onBack: (() -> Unit)? = null) {
    var currentScreen by remember { mutableStateOf<TestGenScreen>(TestGenScreen.Menu) }

    when (currentScreen) {
        TestGenScreen.Menu -> TestGenMenuScreen(
            onBack = onBack,
            onNavigate = { currentScreen = it }
        )
        TestGenScreen.Placeholder -> PlaceholderTextScreen(
            onBack = { currentScreen = TestGenScreen.Menu }
        )
        TestGenScreen.IdCard -> IdCardScreen(
            onBack = { currentScreen = TestGenScreen.Menu }
        )
        TestGenScreen.Person -> PersonScreen(
            onBack = { currentScreen = TestGenScreen.Menu }
        )
        TestGenScreen.BankCard -> BankCardScreen(
            onBack = { currentScreen = TestGenScreen.Menu }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestGenMenuScreen(
    onBack: (() -> Unit)?,
    onNavigate: (TestGenScreen) -> Unit
) {
    val tools = listOf(
        TestGenTool("placeholder", "占位文本", "中英文假文、段落生成", Icons.Default.TextSnippet),
        TestGenTool("idcard", "身份证号码", "格式合法的随机身份证号", Icons.Default.Badge),
        TestGenTool("person", "个人资料", "姓名、手机、邮箱、地址批量", Icons.Default.Person),
        TestGenTool("bankcard", "银行卡号", "符合 Luhn 校验的卡号", Icons.Default.CreditCard)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("测试数据生成") },
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
                    accent = moduleAccent(ModuleId.TESTGEN),
                    hasSub = false,
                    onClick = {
                        when (tool.id) {
                            "placeholder" -> onNavigate(TestGenScreen.Placeholder)
                            "idcard" -> onNavigate(TestGenScreen.IdCard)
                            "person" -> onNavigate(TestGenScreen.Person)
                            "bankcard" -> onNavigate(TestGenScreen.BankCard)
                        }
                    }
                )
            }
        }
    }
}