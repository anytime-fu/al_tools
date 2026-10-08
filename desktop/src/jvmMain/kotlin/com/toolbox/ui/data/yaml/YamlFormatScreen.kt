package com.toolbox.ui.data.yaml

import com.toolbox.ui.components.AppFilterChip

import com.toolbox.ui.components.AppOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.serialization.json.*
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

private enum class YamlMode(val label: String) {
    FORMAT("格式化"),
    VALIDATE("验证"),
    YAML_TO_JSON("YAML→JSON"),
    JSON_TO_YAML("JSON→YAML")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YamlFormatScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(YamlMode.FORMAT) }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YAML 格式化") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                YamlMode.values().forEach { m ->
                    AppFilterChip(
                        selected = mode == m,
                        onClick = { mode = m },
                        label = { Text(m.label) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        message = ""
                        try {
                            when (mode) {
                                YamlMode.FORMAT -> {
                                    output = dumpYaml(loadYaml(input))
                                    message = "格式化成功"
                                }
                                YamlMode.VALIDATE -> {
                                    loadYaml(input)
                                    output = "YAML 格式有效"
                                    message = "验证通过"
                                }
                                YamlMode.YAML_TO_JSON -> {
                                    output = toJson(loadYaml(input))
                                    message = "转换成功"
                                }
                                YamlMode.JSON_TO_YAML -> {
                                    val element = Json.parseToJsonElement(input.trim())
                                    output = dumpYaml(fromJson(element))
                                    message = "转换成功"
                                }
                            }
                        } catch (e: Exception) {
                            output = ""
                            message = "处理失败：${e.message}"
                        }
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("执行")
                }

                AppOutlinedButton(
                    onClick = {
                        if (output.isNotEmpty()) {
                            Toolkit.getDefaultToolkit().systemClipboard
                                .setContents(StringSelection(output), null)
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }

                AppOutlinedButton(
                    onClick = {
                        input = ""
                        output = ""
                        message = ""
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.startsWith("处理失败")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("输入", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = {
                            Text(
                                when (mode) {
                                    YamlMode.JSON_TO_YAML -> "{\"key\": \"value\"}"
                                    else -> "key: value\nlist:\n  - a\n  - b"
                                }
                            )
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("结果", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = output,
                        onValueChange = {},
                        modifier = Modifier.fillMaxSize(),
                        readOnly = true,
                        placeholder = { Text("结果...") }
                    )
                }
            }
        }
    }
}

private fun loadYaml(text: String): Any? = Yaml().load<Any>(text)

private fun dumpYaml(data: Any?): String {
    val options = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
    }
    return Yaml(options).dump(data ?: "")
}

private fun toJson(data: Any?): String {
    val element = toJsonElement(data)
    return Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), element)
}

private fun toJsonElement(data: Any?): JsonElement = when (data) {
    null -> JsonNull
    is Map<*, *> -> buildJsonObject {
        data.forEach { (k, v) -> put(k.toString(), toJsonElement(v)) }
    }
    is List<*> -> JsonArray(data.map { toJsonElement(it) })
    is Boolean -> JsonPrimitive(data)
    is Number -> JsonPrimitive(data)
    else -> JsonPrimitive(data.toString())
}

private fun fromJson(element: JsonElement): Any? = when (element) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        element.isString -> element.content
        element.content == "true" -> true
        element.content == "false" -> false
        else -> element.content.toLongOrNull()
            ?: element.content.toDoubleOrNull()
            ?: element.content
    }
    is JsonObject -> element.entries.associate { (k, v) -> k to fromJson(v) }
    is JsonArray -> element.map { fromJson(it) }
}
