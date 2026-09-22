package com.toolbox.ui.calculator

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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val inputValue by viewModel.inputValue.collectAsState()
    val fromUnit by viewModel.fromUnit.collectAsState()
    val toUnit by viewModel.toUnit.collectAsState()
    val result by viewModel.result.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val currentCategory = viewModel.categories[selectedCategoryIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("计算转换") },
                actions = {
                    if (currentCategory.name == "汇率") {
                        IconButton(
                            onClick = { viewModel.refreshExchangeRates() },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新汇率")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 分类选择
            Text(
                text = "选择类型",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.categories.size) { index ->
                    FilterChip(
                        selected = index == selectedCategoryIndex,
                        onClick = {
                            selectedCategoryIndex = index
                            viewModel.selectCategory(index)
                        },
                        label = { Text(viewModel.categories[index].name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 输入值
            OutlinedTextField(
                value = inputValue,
                onValueChange = viewModel::onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输入数值") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 从单位
            Text(
                text = "从",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            var fromExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = fromExpanded,
                onExpandedChange = { fromExpanded = !fromExpanded }
            ) {
                OutlinedTextField(
                    value = currentCategory.units[fromUnit].let { "${it.name} (${it.symbol})" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.large
                )

                ExposedDropdownMenu(
                    expanded = fromExpanded,
                    onDismissRequest = { fromExpanded = false }
                ) {
                    currentCategory.units.forEachIndexed { index, unit ->
                        DropdownMenuItem(
                            text = { Text("${unit.name} (${unit.symbol})") },
                            onClick = {
                                viewModel.onFromUnitChange(index)
                                fromExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 交换按钮
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        val temp = fromUnit
                        viewModel.onFromUnitChange(toUnit)
                        viewModel.onToUnitChange(temp)
                    }
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "交换单位",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 到单位
            Text(
                text = "到",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            var toExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = toExpanded,
                onExpandedChange = { toExpanded = !toExpanded }
            ) {
                OutlinedTextField(
                    value = currentCategory.units[toUnit].let { "${it.name} (${it.symbol})" },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.large
                )

                ExposedDropdownMenu(
                    expanded = toExpanded,
                    onDismissRequest = { toExpanded = false }
                ) {
                    currentCategory.units.forEachIndexed { index, unit ->
                        DropdownMenuItem(
                            text = { Text("${unit.name} (${unit.symbol})") },
                            onClick = {
                                viewModel.onToUnitChange(index)
                                toExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 结果
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "转换结果",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.ifEmpty { "0" },
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
