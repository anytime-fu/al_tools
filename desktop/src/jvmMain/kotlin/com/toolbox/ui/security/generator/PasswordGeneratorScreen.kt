package com.toolbox.ui.security.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.toolbox.ui.image.ImageIo
import com.toolbox.ui.security.strength.PasswordStrength

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(onBack: () -> Unit) {
    var length by remember { mutableStateOf(16f) }
    var countText by remember { mutableStateOf("5") }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useDigit by remember { mutableStateOf(true) }
    var useSymbol by remember { mutableStateOf(true) }
    var excludeConfusing by remember { mutableStateOf(true) }
    var passwords by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("密码生成器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("长度: ${length.toInt()} 位", style = MaterialTheme.typography.bodyMedium)
                    Slider(value = length, onValueChange = { length = it }, valueRange = 4f..64f)

                    OutlinedTextField(
                        value = countText,
                        onValueChange = { input -> countText = input.filter { it.isDigit() }.take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("生成数量 (1-100)") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useUpper, onCheckedChange = { useUpper = it })
                        Text("大写 A-Z", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useLower, onCheckedChange = { useLower = it })
                        Text("小写 a-z", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useDigit, onCheckedChange = { useDigit = it })
                        Text("数字 0-9", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useSymbol, onCheckedChange = { useSymbol = it })
                        Text("符号 !@#\$...", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = excludeConfusing, onCheckedChange = { excludeConfusing = it })
                        Text("排除易混淆字符 (0OoIl1)", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val count = (countText.toIntOrNull() ?: 5).coerceIn(1, 100)
                            passwords = List(count) {
                                PasswordStrength.generate(
                                    length = length.toInt(),
                                    useUpper = useUpper,
                                    useLower = useLower,
                                    useDigit = useDigit,
                                    useSymbol = useSymbol,
                                    excludeConfusing = excludeConfusing
                                )
                            }
                            message = "已生成 ${passwords.size} 个密码"
                        },
                        enabled = useUpper || useLower || useDigit || useSymbol,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("生成密码")
                    }

                    OutlinedButton(
                        onClick = {
                            if (passwords.isNotEmpty()) {
                                ImageIo.copyTextToClipboard(passwords.joinToString("\n"))
                                message = "已复制全部 ${passwords.size} 个密码"
                            }
                        },
                        enabled = passwords.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制全部")
                    }

                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("生成结果", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                if (passwords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "配置选项后点击「生成密码」",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(passwords) { pw ->
                            val result = remember(pw) { PasswordStrength.evaluate(pw) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = pw,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = result.level,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            result.score >= 80 -> Color(0xFF22C55E)
                                            result.score >= 40 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        }
                                    )
                                    IconButton(
                                        onClick = {
                                            ImageIo.copyTextToClipboard(pw)
                                            message = "已复制密码"
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "复制",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
