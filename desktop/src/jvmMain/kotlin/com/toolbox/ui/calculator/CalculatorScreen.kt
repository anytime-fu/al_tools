package com.toolbox.ui.calculator

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CalculatorScreen(onBack: (() -> Unit)? = null) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "计算器",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expression input
        OutlinedTextField(
            value = expression,
            onValueChange = { 
                expression = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("数学表达式") },
            placeholder = { Text("例如: 2+3*4, sqrt(16), sin(30)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    result = calculate(expression)
                }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calculate button
        Button(
            onClick = {
                result = calculate(expression)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("计算")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Result
        if (result.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "结果",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = result,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Error
        error?.let { errorMsg ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMsg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick functions
        Text(
            text = "快捷函数",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("sqrt()", "sin()", "cos()", "tan()").forEach { func ->
                AppOutlinedButton(
                    onClick = { expression += func },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(func)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Unit conversion
        Text(
            text = "单位换算",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        var convertValue by remember { mutableStateOf("") }
        var fromUnit by remember { mutableStateOf("km") }
        var toUnit by remember { mutableStateOf("miles") }
        var convertResult by remember { mutableStateOf("") }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = convertValue,
                onValueChange = { convertValue = it },
                modifier = Modifier.weight(1f),
                label = { Text("数值") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            
            Text(
                "从",
                color = MaterialTheme.colorScheme.onBackground
            )
            
            DropdownMenuBox(
                selected = fromUnit,
                options = listOf("km", "miles", "kg", "lbs", "celsius", "fahrenheit"),
                onSelected = { fromUnit = it }
            )
            
            Text(
                "到",
                color = MaterialTheme.colorScheme.onBackground
            )
            
            DropdownMenuBox(
                selected = toUnit,
                options = listOf("km", "miles", "kg", "lbs", "celsius", "fahrenheit"),
                onSelected = { toUnit = it }
            )
            
            Button(
                onClick = {
                    val value = convertValue.toDoubleOrNull()
                    if (value != null) {
                        convertResult = unitConvert(value, fromUnit, toUnit)
                    } else {
                        convertResult = "请输入有效数值"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("转换")
            }
        }
        
        if (convertResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = convertResult,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun DropdownMenuBox(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        AppOutlinedButton(
            onClick = { expanded = true }
        ) {
            Text(selected)
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun calculate(expression: String): String {
    return try {
        val result = evaluateExpression(expression)
        "$expression = ${String.format("%.6f", result).trimEnd('0').trimEnd('.')}"
    } catch (e: Exception) {
        "计算错误: ${e.message}"
    }
}

fun evaluateExpression(expression: String): Double {
    val expr = expression.replace(" ", "")
    
    return when {
        expr.contains("+") -> {
            val parts = expr.split("+", limit = 2)
            evaluateExpression(parts[0]) + evaluateExpression(parts[1])
        }
        expr.contains("-") -> {
            val parts = expr.split("-", limit = 2)
            evaluateExpression(parts[0]) - evaluateExpression(parts[1])
        }
        expr.contains("*") -> {
            val parts = expr.split("*", limit = 2)
            evaluateExpression(parts[0]) * evaluateExpression(parts[1])
        }
        expr.contains("/") -> {
            val parts = expr.split("/", limit = 2)
            val divisor = evaluateExpression(parts[1])
            if (divisor == 0.0) throw ArithmeticException("Division by zero")
            evaluateExpression(parts[0]) / divisor
        }
        expr.startsWith("sqrt(") && expr.endsWith(")") -> {
            val inner = expr.substring(5, expr.length - 1)
            Math.sqrt(evaluateExpression(inner))
        }
        expr.startsWith("sin(") && expr.endsWith(")") -> {
            val inner = expr.substring(4, expr.length - 1)
            Math.sin(Math.toRadians(evaluateExpression(inner)))
        }
        expr.startsWith("cos(") && expr.endsWith(")") -> {
            val inner = expr.substring(4, expr.length - 1)
            Math.cos(Math.toRadians(evaluateExpression(inner)))
        }
        expr.startsWith("tan(") && expr.endsWith(")") -> {
            val inner = expr.substring(4, expr.length - 1)
            Math.tan(Math.toRadians(evaluateExpression(inner)))
        }
        else -> expr.toDouble()
    }
}

fun unitConvert(value: Double, fromUnit: String, toUnit: String): String {
    val result = when {
        fromUnit == "km" && toUnit == "miles" -> value * 0.621371
        fromUnit == "miles" && toUnit == "km" -> value * 1.60934
        fromUnit == "kg" && toUnit == "lbs" -> value * 2.20462
        fromUnit == "lbs" && toUnit == "kg" -> value * 0.453592
        fromUnit == "celsius" && toUnit == "fahrenheit" -> value * 9/5 + 32
        fromUnit == "fahrenheit" && toUnit == "celsius" -> (value - 32) * 5/9
        else -> return "不支持的单位转换: $fromUnit -> $toUnit"
    }
    
    return "$value $fromUnit = ${String.format("%.2f", result)} $toUnit"
}
