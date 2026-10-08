package com.toolbox.ui.testgen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.toolbox.ui.components.AppFilterChip
import com.toolbox.util.TestDataGen
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCardScreen(onBack: () -> Unit) {
    var count by remember { mutableStateOf("5") }
    var gender by remember { mutableStateOf(TestDataGen.Gender.RANDOM) }
    var birthText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<String>()) }

    fun generate() {
        val n = (count.toIntOrNull() ?: 5).coerceIn(1, 200)
        val fixedBirth = runCatching { LocalDate.parse(birthText.trim()) }.getOrNull()
        results = (1..n).map {
            val birth = fixedBirth ?: TestDataGen.randomBirthDate()
            TestDataGen.idCardNumber(birth, gender)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("身份证号码") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "生成配置",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = count,
                            onValueChange = { count = it },
                            modifier = Modifier.width(100.dp),
                            label = { Text("数量") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = birthText,
                            onValueChange = { birthText = it },
                            modifier = Modifier.width(180.dp),
                            label = { Text("出生日期(可选)") },
                            placeholder = { Text("yyyy-MM-dd，留空随机") },
                            singleLine = true
                        )
                        AppFilterChip(
                            selected = gender == TestDataGen.Gender.RANDOM,
                            onClick = { gender = TestDataGen.Gender.RANDOM },
                            label = { Text("随机性别") }
                        )
                        AppFilterChip(
                            selected = gender == TestDataGen.Gender.MALE,
                            onClick = { gender = TestDataGen.Gender.MALE },
                            label = { Text("男") }
                        )
                        AppFilterChip(
                            selected = gender == TestDataGen.Gender.FEMALE,
                            onClick = { gender = TestDataGen.Gender.FEMALE },
                            label = { Text("女") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { generate() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("生成")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "仅供测试使用：号码格式与校验位合法，但不对应任何真实身份",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "生成结果 (${results.size} 个)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        val selection = StringSelection(results.joinToString("\n"))
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                    },
                    enabled = results.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制全部")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                results.forEach { id ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = id,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val selection = StringSelection(id)
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.width(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}