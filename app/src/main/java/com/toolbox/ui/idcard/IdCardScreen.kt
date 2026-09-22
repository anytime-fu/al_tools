package com.toolbox.ui.idcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toolbox.util.IdCardUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCardScreen(
    onBack: () -> Unit,
    viewModel: IdCardViewModel = hiltViewModel()
) {
    val idNumber by viewModel.idNumber.collectAsState()
    val idCardInfo by viewModel.idCardInfo.collectAsState()
    val history by viewModel.history.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("身份证识别") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入区域
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "输入身份证号码",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = idNumber,
                            onValueChange = { viewModel.onIdNumberChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("18位身份证号码") },
                            singleLine = true,
                            trailingIcon = {
                                if (idNumber.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onIdNumberChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.parseIdCard() },
                                modifier = Modifier.weight(1f),
                                enabled = idNumber.length == 18
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("识别")
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearResult() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("清空")
                            }
                        }
                    }
                }
            }

            // 识别结果
            idCardInfo?.let { info ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (info.isValid)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (info.isValid) "识别成功" else "识别失败",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    if (info.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (info.isValid)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }

                            if (info.isValid) {
                                Spacer(modifier = Modifier.height(16.dp))

                                IdCardInfoItem(label = "籍贯", value = info.province)
                                IdCardInfoItem(label = "出生日期", value = info.birthday.toString())
                                IdCardInfoItem(label = "年龄", value = "${info.age}岁")
                                IdCardInfoItem(label = "性别", value = info.gender)
                                IdCardInfoItem(label = "身份证号", value = IdCardUtil.formatIdNumber(info.idNumber))
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = info.errorMessage ?: "未知错误",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // 历史记录
            if (history.isNotEmpty()) {
                item {
                    Text(
                        text = "识别历史",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(history) { info ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${info.province} · ${info.gender} · ${info.age}岁",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = IdCardUtil.formatIdNumber(info.idNumber),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.removeFromHistory(info) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdCardInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
