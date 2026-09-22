package com.toolbox.ui.life.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.*

data class Language(
    val code: String,
    val name: String
)

private val languages = listOf(
    Language("zh", "中文"),
    Language("en", "英语"),
    Language("ja", "日语"),
    Language("ko", "韩语")
)

private val zhToEn = mapOf(
    "你好" to "Hello",
    "谢谢" to "Thank you",
    "再见" to "Goodbye",
    "早上好" to "Good morning",
    "晚安" to "Good night",
    "对不起" to "Sorry",
    "没关系" to "It's okay",
    "请" to "Please",
    "是" to "Yes",
    "不是" to "No",
    "我" to "I",
    "你" to "You",
    "他" to "He",
    "她" to "She",
    "我们" to "We",
    "什么" to "What",
    "谁" to "Who",
    "在哪里" to "Where",
    "什么时候" to "When",
    "为什么" to "Why",
    "怎么样" to "How",
    "好" to "Good",
    "坏" to "Bad",
    "大" to "Big",
    "小" to "Small",
    "多" to "Many",
    "少" to "Few",
    "快" to "Fast",
    "慢" to "Slow",
    "热" to "Hot",
    "冷" to "Cold",
    "水" to "Water",
    "食物" to "Food",
    "钱" to "Money",
    "时间" to "Time",
    "今天" to "Today",
    "明天" to "Tomorrow",
    "昨天" to "Yesterday",
    "朋友" to "Friend",
    "家人" to "Family",
    "工作" to "Work",
    "学校" to "School",
    "医院" to "Hospital",
    "餐厅" to "Restaurant",
    "酒店" to "Hotel",
    "机场" to "Airport",
    "火车站" to "Train station",
    "多少钱" to "How much",
    "便宜一点" to "Cheaper please",
    "太贵了" to "Too expensive",
    "我要这个" to "I want this",
    "厕所在哪里" to "Where is the restroom",
    "我不明白" to "I don't understand",
    "请说慢一点" to "Please speak slowly",
    "你会说英语吗" to "Do you speak English",
    "救命" to "Help",
    "警察" to "Police",
    "医生" to "Doctor",
    "我爱你" to "I love you",
    "生日快乐" to "Happy birthday",
    "新年快乐" to "Happy new year"
)

private val enToZh = zhToEn.entries.associate { (k, v) -> v.lowercase() to k }

private val zhToJa = mapOf(
    "你好" to "こんにちは",
    "谢谢" to "ありがとう",
    "再见" to "さようなら",
    "早上好" to "おはようございます",
    "晚安" to "おやすみなさい",
    "对不起" to "すみません",
    "是" to "はい",
    "不是" to "いいえ",
    "我" to "私",
    "你" to "あなた",
    "好" to "良い",
    "水" to "水",
    "食物" to "食べ物",
    "朋友" to "友達",
    "我爱你" to "愛してる"
)

private val zhToKo = mapOf(
    "你好" to "안녕하세요",
    "谢谢" to "감사합니다",
    "再见" to "안녕히 가세요",
    "对不起" to "죄송합니다",
    "是" to "네",
    "不是" to "아니요",
    "我" to "나",
    "你" to "당신",
    "好" to "좋다",
    "水" to "물",
    "食物" to "음식",
    "朋友" to "친구",
    "我爱你" to "사랑해요"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    onBack: () -> Unit
) {
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var sourceLang by remember { mutableStateOf(languages[0]) }
    var targetLang by remember { mutableStateOf(languages[1]) }
    var showSourceLangMenu by remember { mutableStateOf(false) }
    var showTargetLangMenu by remember { mutableStateOf(false) }
    var translationResult by remember { mutableStateOf("") }

    fun translate() {
        if (sourceText.isBlank()) {
            translatedText = ""
            return
        }

        val input = sourceText.trim()
        translatedText = when {
            sourceLang.code == "zh" && targetLang.code == "en" -> {
                // Try exact match first, then partial match
                zhToEn[input]
                    ?: zhToEn.entries.firstOrNull { input.contains(it.key) }?.value
                    ?: "未找到翻译，请尝试常用词语"
            }
            sourceLang.code == "en" && targetLang.code == "zh" -> {
                enToZh[input.lowercase()]
                    ?: enToZh.entries.firstOrNull { input.lowercase().contains(it.key) }?.value
                    ?: "Translation not found, try common words"
            }
            sourceLang.code == "zh" && targetLang.code == "ja" -> {
                zhToJa[input]
                    ?: zhToJa.entries.firstOrNull { input.contains(it.key) }?.value
                    ?: "翻訳が見つかりません"
            }
            sourceLang.code == "zh" && targetLang.code == "ko" -> {
                zhToKo[input]
                    ?: zhToKo.entries.firstOrNull { input.contains(it.key) }?.value
                    ?: "번역을 찾을 수 없습니다"
            }
            sourceLang.code == "ja" && targetLang.code == "zh" -> {
                zhToJa.entries.find { it.value == input }?.key
                    ?: "未找到翻译"
            }
            sourceLang.code == "ko" && targetLang.code == "zh" -> {
                zhToKo.entries.find { it.value == input }?.key
                    ?: "未找到翻译"
            }
            else -> "该语言对暂不支持，请选择中文↔英语/日语/韩语"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("翻译") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            // Language selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    OutlinedButton(onClick = { showSourceLangMenu = true }) {
                        Text(sourceLang.name)
                    }
                    DropdownMenu(
                        expanded = showSourceLangMenu,
                        onDismissRequest = { showSourceLangMenu = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.name) },
                                onClick = {
                                    sourceLang = lang
                                    showSourceLangMenu = false
                                    translate()
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = {
                    val temp = sourceLang
                    sourceLang = targetLang
                    targetLang = temp
                    sourceText = translatedText
                    translatedText = ""
                    translate()
                }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "交换")
                }

                Box {
                    OutlinedButton(onClick = { showTargetLangMenu = true }) {
                        Text(targetLang.name)
                    }
                    DropdownMenu(
                        expanded = showTargetLangMenu,
                        onDismissRequest = { showTargetLangMenu = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.name) },
                                onClick = {
                                    targetLang = lang
                                    showTargetLangMenu = false
                                    translate()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Source text
            OutlinedTextField(
                value = sourceText,
                onValueChange = {
                    sourceText = it
                    translate()
                },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                label = { Text("输入文本") },
                trailingIcon = {
                    IconButton(onClick = {
                        sourceText = ""
                        translatedText = ""
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Translate button
            Button(
                onClick = { translate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = sourceText.isNotBlank()
            ) {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("翻译")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Translated text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("翻译结果", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (translatedText.isNotBlank()) translatedText else "翻译结果将显示在这里",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick phrases
            Text("常用短语", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            val quickPhrases = listOf("你好", "谢谢", "再见", "多少钱", "厕所在哪里", "我爱你")
            quickPhrases.forEach { phrase ->
                TextButton(
                    onClick = {
                        sourceText = phrase
                        translate()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(phrase, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
