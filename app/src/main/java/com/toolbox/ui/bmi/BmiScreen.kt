package com.toolbox.ui.bmi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmiScreen(
    viewModel: BmiViewModel = hiltViewModel()
) {
    val weight by viewModel.weight.collectAsState()
    val height by viewModel.height.collectAsState()
    val bmiResult by viewModel.bmiResult.collectAsState()
    val bmiCategory by viewModel.bmiCategory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BMI计算器") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Icon(
                imageVector = Icons.Default.MonitorWeight,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // 体重输入
            OutlinedTextField(
                value = weight,
                onValueChange = viewModel::onWeightChange,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                label = { Text("体重 (公斤)") },
                leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 身高输入
            OutlinedTextField(
                value = height,
                onValueChange = viewModel::onHeightChange,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                label = { Text("身高 (厘米)") },
                leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 计算按钮
            Button(
                onClick = { viewModel.calculateBmi() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("计算BMI")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 结果显示
            if (bmiResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BMI指数",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bmiResult,
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bmiCategory,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BMI参考表
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "BMI参考标准",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BmiReferenceItem("偏瘦", "< 18.5")
                        BmiReferenceItem("正常", "18.5 - 23.9")
                        BmiReferenceItem("偏胖", "24.0 - 27.9")
                        BmiReferenceItem("肥胖", "≥ 28.0")
                    }
                }
            }
        }
    }
}

@Composable
private fun BmiReferenceItem(category: String, range: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = category, style = MaterialTheme.typography.bodyMedium)
        Text(text = range, style = MaterialTheme.typography.bodyMedium)
    }
}
