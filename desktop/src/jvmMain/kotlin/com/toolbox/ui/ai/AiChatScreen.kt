package com.toolbox.ui.ai

import com.toolbox.ui.components.appClickable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.toolbox.ai.tools.ToolCall
import com.toolbox.ai.tools.ToolExecutor
import com.toolbox.ai.tools.ToolRegistry
import com.toolbox.ai.tools.ToolResult
import com.toolbox.data.local.entity.ChatMessageEntity
import com.toolbox.data.local.entity.ChatSession
import com.toolbox.data.remote.AiApiService
import com.toolbox.data.remote.AiApiServiceImpl
import com.toolbox.data.remote.AiConfig
import com.toolbox.data.remote.AiConfigManager
import com.toolbox.data.repository.ChatRepository
import com.toolbox.ui.components.MarkdownText
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.koin.java.KoinJavaComponent.inject

data class ChatMessage(
    val role: String,
    val content: String,
    val isToolCall: Boolean = false,
    val toolName: String? = null,
    val isToolResult: Boolean = false,
    val isStreaming: Boolean = false
)

private const val MAX_TOOL_ROUNDS = 3

private fun encodeToolCalls(calls: List<ToolCall>): String {
    val array = buildJsonArray {
        calls.forEach { call ->
            add(
                buildJsonObject {
                    put("name", JsonPrimitive(call.function.name))
                    put("arguments", parseArgsToElement(call.function.arguments))
                }
            )
        }
    }
    return array.toString()
}

private fun parseArgsToElement(args: String): JsonElement {
    return runCatching { Json.parseToJsonElement(args) }.getOrDefault(JsonObject(emptyMap()))
}

private fun appendToolNarrative(
    history: List<Pair<String, String>>,
    calls: List<ToolCall>,
    results: List<ToolResult>
): List<Pair<String, String>> {
    val narrative = buildString {
        append("[工具调用] ")
        append(calls.joinToString(", ") { it.function.name })
        results.forEach { result ->
            append("\n[工具结果] ${result.functionName}: ${result.result}")
        }
    }
    
    val last = history.lastOrNull()
    return if (last != null && last.first == "user") {
        history.dropLast(1) + (last.first to (last.second + "\n\n" + narrative))
    } else {
        history + ("user" to narrative)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(onBack: (() -> Unit)? = null) {
    val chatRepository: ChatRepository by inject(ChatRepository::class.java)
    val toolExecutor: ToolExecutor by inject(ToolExecutor::class.java)
    val coroutineScope = rememberCoroutineScope()
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentSessionId by remember { mutableStateOf<Long?>(null) }
    var toolsEnabled by remember { mutableStateOf(true) }
    
    val listState = rememberLazyListState()
    
    // Get config from manager
    val config by AiConfigManager.config.collectAsState()
    
    // Get sessions from repository
    val sessions by chatRepository.getAllSessions().collectAsState(initial = emptyList())
    
    // Create AI service
    val aiService = remember {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
        AiApiServiceImpl(client, config)
    }
    
    // Update config when it changes
    LaunchedEffect(config) {
        aiService.updateConfig(config)
    }
    
    // Function to load session messages
    fun loadSessionMessages(sessionId: Long) {
        coroutineScope.launch {
            try {
                val messageEntities = withContext(Dispatchers.IO) {
                    chatRepository.getMessagesBySessionIdSync(sessionId)
                }
                messages = messageEntities.map { entity ->
                    ChatMessage(role = entity.role, content = entity.content)
                }
            } catch (e: Exception) {
                messages = emptyList()
            }
        }
    }
    
    // Function to create new session
    fun createNewSession() {
        coroutineScope.launch {
            val session = ChatSession(
                title = "新对话",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val sessionId = chatRepository.insertSession(session)
            currentSessionId = sessionId
            messages = emptyList()
        }
    }
    
    // Function to load session
    fun loadSession(sessionId: Long) {
        currentSessionId = sessionId
        loadSessionMessages(sessionId)
    }
    
    // Function to send message
    fun sendMessage() {
        if (inputText.isBlank() || isLoading) return
        
        val currentInput = inputText
        inputText = ""
        isLoading = true
        error = null
        
        // Add user message to UI immediately
        messages = messages + ChatMessage(role = "user", content = currentInput)
        
        coroutineScope.launch {
            try {
                // Create session if needed
                var sessionId = currentSessionId
                if (sessionId == null) {
                    val session = ChatSession(
                        title = currentInput.take(20) + if (currentInput.length > 20) "..." else "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    sessionId = chatRepository.insertSession(session)
                    currentSessionId = sessionId
                }
                
                // Save user message
                chatRepository.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "user",
                        content = currentInput,
                        createdAt = System.currentTimeMillis()
                    )
                )
                
                if (toolsEnabled) {
                    var history = messages
                        .filter { !it.isToolCall && !it.isToolResult && !it.isStreaming }
                        .map { it.role to it.content }
                    
                    var round = 0
                    var response = aiService.sendAiMessageWithTools(history, ToolRegistry.allTools)
                    
                    while (true) {
                        val aiResponse = response.getOrThrow()
                        val toolCalls = aiResponse.toolCalls
                        
                        if (aiResponse.isToolCall && !toolCalls.isNullOrEmpty() && round < MAX_TOOL_ROUNDS) {
                            round++
                            val calls = toolCalls
                            val toolNames = calls.joinToString(", ") { it.function.name }
                            
                            messages = messages + ChatMessage(
                                role = "assistant",
                                content = toolNames,
                                isToolCall = true,
                                toolName = toolNames
                            )
                            chatRepository.insertMessage(
                                ChatMessageEntity(
                                    sessionId = sessionId,
                                    role = "assistant",
                                    content = "🔧 调用工具: $toolNames",
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            
                            val callsJson = encodeToolCalls(calls)
                            val results = calls.map { toolExecutor.executeTool(it) }
                            
                            val resultText = results.joinToString("\n") { result ->
                                val marker = if (result.isError) "✗" else "✓"
                                "$marker ${result.functionName}: ${result.result}"
                            }
                            messages = messages + ChatMessage(
                                role = "assistant",
                                content = resultText,
                                isToolResult = true
                            )
                            chatRepository.insertMessage(
                                ChatMessageEntity(
                                    sessionId = sessionId,
                                    role = "assistant",
                                    content = "🔧 工具结果:\n$resultText",
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            
                            history = appendToolNarrative(history, calls, results)
                            response = aiService.sendToolResults(
                                history + ("assistant" to callsJson),
                                results,
                                ToolRegistry.allTools
                            )
                        } else {
                            val text = aiResponse.text ?: "无响应"
                            messages = messages + ChatMessage(role = "assistant", content = text)
                            chatRepository.insertMessage(
                                ChatMessageEntity(
                                    sessionId = sessionId,
                                    role = "assistant",
                                    content = text,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            break
                        }
                    }
                } else {
                    messages = messages + ChatMessage(role = "assistant", content = "", isStreaming = true)
                    
                    val messagePairs = messages
                        .filter { !it.isToolCall && !it.isToolResult && !it.isStreaming }
                        .map { it.role to it.content }
                    
                    val fullContent = StringBuilder()
                    
                    aiService.sendAiMessageStream(messagePairs).collect { chunk ->
                        fullContent.append(chunk)
                        messages = messages.dropLast(1) + ChatMessage(
                            role = "assistant",
                            content = fullContent.toString(),
                            isStreaming = true
                        )
                    }
                    
                    messages = messages.dropLast(1) + ChatMessage(
                        role = "assistant",
                        content = fullContent.toString(),
                        isStreaming = false
                    )
                    
                    chatRepository.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = fullContent.toString(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                
                chatRepository.getSessionById(sessionId)?.let { session ->
                    chatRepository.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                // Remove streaming message on error
                if (messages.lastOrNull()?.isStreaming == true) {
                    messages = messages.dropLast(1)
                }
                error = "错误: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = { Text("AI 对话") },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            actions = {
                // New chat button
                IconButton(onClick = { createNewSession() }) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "新对话",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Delete current session button
                IconButton(onClick = { 
                    currentSessionId?.let { sessionId ->
                        coroutineScope.launch {
                            chatRepository.deleteMessagesBySessionId(sessionId)
                            chatRepository.deleteSession(ChatSession(id = sessionId, title = "", createdAt = 0, updatedAt = 0))
                        }
                    }
                    messages = emptyList()
                    currentSessionId = null
                }) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "删除当前会话",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
        
        // Session list (horizontal scrollable chips)
        if (sessions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    val isSelected = session.id == currentSessionId
                    Surface(
                        onClick = { loadSession(session.id) },
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.title.take(15) + if (session.title.length > 15) "..." else "",
                                maxLines = 1,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "删除",
                                modifier = Modifier
                                    .size(14.dp)
                                    .appClickable {
                                        coroutineScope.launch {
                                            chatRepository.deleteMessagesBySessionId(session.id)
                                            chatRepository.deleteSession(session)
                                            if (currentSessionId == session.id) {
                                                currentSessionId = null
                                                messages = emptyList()
                                            }
                                        }
                                    },
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Error message
        error?.let { errorMsg ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    IconButton(onClick = { error = null }) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            state = listState
        ) {
            itemsIndexed(messages) { index, message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (isLoading && messages.lastOrNull()?.isStreaming != true) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI 思考中...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Input area
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when {
                                    event.key == Key.Enter && !event.isCtrlPressed -> {
                                        sendMessage()
                                        true
                                    }
                                    event.key == Key.Enter && event.isCtrlPressed -> {
                                        inputText += "\n"
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        },
                    placeholder = { Text("输入消息... (Enter发送, Ctrl+Enter换行)") },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = { toolsEnabled = !toolsEnabled },
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = if (toolsEnabled) "关闭工具调用" else "开启工具调用",
                        tint = if (toolsEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Button(
                    onClick = { sendMessage() },
                    enabled = !isLoading && inputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 700.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else if (message.isToolCall || message.isToolResult) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isToolCall) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "执行工具: ${message.toolName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                if (isUser) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    // Render as Markdown for assistant messages
                    MarkdownText(
                        markdown = message.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Show streaming indicator
                    if (message.isStreaming) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}