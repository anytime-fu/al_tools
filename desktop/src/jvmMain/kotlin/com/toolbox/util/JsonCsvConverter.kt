package com.toolbox.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.encodeToString

object JsonCsvConverter {
    fun jsonToCsv(text: String): String {
    val element = Json.parseToJsonElement(text.trim())
    val array = when (element) {
        is JsonArray -> element
        is JsonObject -> JsonArray(listOf(element))
        else -> throw IllegalArgumentException("需要 JSON 数组或对象")
    }
    val headers = LinkedHashSet<String>()
    array.forEach { item ->
        if (item is JsonObject) headers.addAll(item.keys)
    }
    if (headers.isEmpty()) throw IllegalArgumentException("JSON 中未找到字段")

    return buildString {
        appendLine(headers.joinToString(",") { escapeCsv(it) })
        array.forEach { item ->
            val obj = item as? JsonObject
            appendLine(headers.joinToString(",") { key ->
                val v = obj?.get(key)
                val content = when {
                    v == null || v is JsonNull -> ""
                    v is JsonPrimitive -> v.content
                    else -> v.toString()
                }
                escapeCsv(content)
            })
        }
    }
}

    fun csvToJson(text: String): String {
    val lines = text.trim().lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) throw IllegalArgumentException("CSV 内容为空")
    val headers = splitCsvLine(lines.first())
    val objects = lines.drop(1).map { line ->
        val values = splitCsvLine(line)
        buildJsonObject {
            headers.forEachIndexed { i, h ->
                put(h, JsonPrimitive(values.getOrNull(i) ?: ""))
            }
        }
    }
    val pretty = Json { prettyPrint = true }
    return pretty.encodeToString(JsonArray.serializer(), JsonArray(objects))
}

private fun escapeCsv(value: String): String {
    return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}

private fun splitCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                sb.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                result.add(sb.toString())
                sb.clear()
            }
            else -> sb.append(c)
        }
        i++
    }
    result.add(sb.toString())
    return result
}
}
