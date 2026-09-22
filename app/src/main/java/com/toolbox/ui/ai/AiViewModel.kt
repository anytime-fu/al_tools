package com.toolbox.ui.ai

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.ai.tools.ToolCall
import com.toolbox.ai.tools.ToolExecutor
import com.toolbox.ai.tools.ToolRegistry
import com.toolbox.ai.tools.ToolResult
import com.toolbox.data.local.entity.ChatSession
import com.toolbox.data.local.entity.ChatMessageEntity
import com.toolbox.data.remote.AiApiService
import com.toolbox.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val role: String,
    val content: String,
    val isToolCall: Boolean = false,
    val toolName: String? = null,
    val isToolResult: Boolean = false
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiApiService: AiApiService,
    private val chatRepository: ChatRepository,
    private val toolExecutor: ToolExecutor
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId

    // 工具对话框专用状态
    private val _toolResult = MutableStateFlow<String?>(null)
    val toolResult: StateFlow<String?> = _toolResult
    
    // 是否启用工具调用
    private val _enableTools = MutableStateFlow(true)
    val enableTools: StateFlow<Boolean> = _enableTools
    
    // 当前正在执行的工具
    private val _executingTool = MutableStateFlow<String?>(null)
    val executingTool: StateFlow<String?> = _executingTool

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            chatRepository.getAllSessions().collect { sessionList ->
                _sessions.value = sessionList
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val session = ChatSession(title = "新对话")
            val sessionId = chatRepository.insertSession(session)
            _currentSessionId.value = sessionId
            _messages.value = emptyList()
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _currentSessionId.value = sessionId
            chatRepository.getMessagesBySessionId(sessionId).collect { messageEntities ->
                _messages.value = messageEntities.map { entity ->
                    ChatMessage(role = entity.role, content = entity.content)
                }
            }
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            chatRepository.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 如果没有当前会话，创建一个
            if (_currentSessionId.value == null) {
                val session = ChatSession(
                    title = text.take(20) + if (text.length > 20) "..." else ""
                )
                val sessionId = chatRepository.insertSession(session)
                _currentSessionId.value = sessionId
            }

            val userMessage = ChatMessage(role = "user", content = text)
            _messages.value = _messages.value + userMessage

            // 保存用户消息到数据库
            val currentSessionId = _currentSessionId.value
            if (currentSessionId == null) {
                _error.value = "会话创建失败"
                _isLoading.value = false
                return@launch
            }

            chatRepository.insertMessage(
                ChatMessageEntity(
                    sessionId = currentSessionId,
                    role = "user",
                    content = text
                )
            )

            // 更新会话标题（如果是第一条消息）
            if (_messages.value.size == 1) {
                val session = chatRepository.getSessionById(currentSessionId)
                session?.let {
                    chatRepository.updateSession(it.copy(title = text.take(20) + if (text.length > 20) "..." else ""))
                }
            }

            try {
                val messagePairs = _messages.value.filter { !it.isToolCall && !it.isToolResult }
                    .map { msg -> msg.role to msg.content }
                
                // 判断是否启用工具调用
                if (_enableTools.value) {
                    sendMessageWithTools(messagePairs, currentSessionId)
                } else {
                    sendMessageWithoutTools(messagePairs, currentSessionId)
                }
            } catch (e: Exception) {
                _error.value = "错误: ${e.message}"
                _messages.value = _messages.value.dropLast(1)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 不带工具调用的消息发送
    private suspend fun sendMessageWithoutTools(
        messagePairs: List<Pair<String, String>>,
        currentSessionId: Long
    ) {
        val result = aiApiService.sendAiMessage(messagePairs)
        
        result.fold(
            onSuccess = { aiText ->
                val aiMessage = ChatMessage(role = "assistant", content = aiText)
                _messages.value = _messages.value + aiMessage
                saveAiMessage(currentSessionId, aiText)
            },
            onFailure = { exception ->
                _error.value = exception.message ?: "AI响应失败"
                _messages.value = _messages.value.dropLast(1)
            }
        )
    }
    
    // 带工具调用的消息发送
    private suspend fun sendMessageWithTools(
        messagePairs: List<Pair<String, String>>,
        currentSessionId: Long
    ) {
        val tools = ToolRegistry.allTools
        val result = aiApiService.sendAiMessageWithTools(messagePairs, tools)
        
        result.fold(
            onSuccess = { aiResponse ->
                if (aiResponse.isToolCall && aiResponse.toolCalls != null) {
                    // 显示AI决定调用工具的消息
                    val toolCallMessage = ChatMessage(
                        role = "assistant",
                        content = "正在执行操作...",
                        isToolCall = true,
                        toolName = aiResponse.toolCalls.first().function.name
                    )
                    _messages.value = _messages.value + toolCallMessage
                    
                    // 执行工具调用
                    executeToolCalls(aiResponse.toolCalls, messagePairs, currentSessionId)
                } else {
                    // 普通文本回复
                    val aiText = aiResponse.text ?: "无响应"
                    val aiMessage = ChatMessage(role = "assistant", content = aiText)
                    _messages.value = _messages.value + aiMessage
                    saveAiMessage(currentSessionId, aiText)
                }
            },
            onFailure = { exception ->
                _error.value = exception.message ?: "AI响应失败"
                _messages.value = _messages.value.dropLast(1)
            }
        )
    }
    
    // 执行工具调用
    private suspend fun executeToolCalls(
        toolCalls: List<ToolCall>,
        messagePairs: List<Pair<String, String>>,
        currentSessionId: Long
    ) {
        val toolResults = mutableListOf<ToolResult>()
        
        for (toolCall in toolCalls) {
            _executingTool.value = toolCall.function.name
            android.util.Log.d("AiViewModel", "Executing tool: ${toolCall.function.name}")
            
            // 执行工具
            val result = toolExecutor.executeTool(toolCall)
            toolResults.add(result)
            android.util.Log.d("AiViewModel", "Tool result: ${result.result}")
            
            // 显示工具执行结果
            val toolResultMessage = ChatMessage(
                role = "system",
                content = result.result,
                isToolResult = true
            )
            _messages.value = _messages.value + toolResultMessage
        }
        
        _executingTool.value = null
        
        // 构建包含functionCall的完整消息历史
        val fullMessagePairs = messagePairs.toMutableList()
        
        // 添加assistant的functionCall消息（用JSON表示）
        val toolCallJson = toolCalls.joinToString(",") { tc ->
            """{"name":"${tc.function.name}","arguments":${tc.function.arguments}}"""
        }
        fullMessagePairs.add("assistant" to "[$toolCallJson]")
        android.util.Log.d("AiViewModel", "Sending tool results to AI, message count: ${fullMessagePairs.size}")
        android.util.Log.d("AiViewModel", "Tool results count: ${toolResults.size}")
        
        // 将工具结果发送给AI获取最终回复
        val tools = ToolRegistry.allTools
        try {
            val finalResult = aiApiService.sendToolResults(fullMessagePairs, toolResults, tools)
            
            finalResult.fold(
                onSuccess = { aiResponse ->
                    android.util.Log.d("AiViewModel", "Got AI response, isToolCall: ${aiResponse.isToolCall}")
                    if (aiResponse.isToolCall && aiResponse.toolCalls != null) {
                        // AI还想调用更多工具，递归执行
                        executeToolCalls(aiResponse.toolCalls, fullMessagePairs, currentSessionId)
                    } else {
                        // 移除临时的工具调用消息，保留最终回复
                        val finalMessages = _messages.value.filter { !it.isToolCall }
                        _messages.value = finalMessages
                        
                        val aiText = aiResponse.text ?: "操作完成"
                        val aiMessage = ChatMessage(role = "assistant", content = aiText)
                        _messages.value = _messages.value + aiMessage
                        saveAiMessage(currentSessionId, aiText)
                    }
                },
                onFailure = { exception ->
                    android.util.Log.e("AiViewModel", "Failed to get AI response", exception)
                    // 移除临时的工具调用消息
                    val finalMessages = _messages.value.filter { !it.isToolCall && !it.isToolResult }
                    _messages.value = finalMessages
                    
                    _error.value = "工具执行后获取回复失败: ${exception.message}"
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AiViewModel", "Exception in executeToolCalls", e)
            _error.value = "工具执行异常: ${e.message}"
        }
    }
    
    // 保存AI消息到数据库
    private suspend fun saveAiMessage(sessionId: Long, content: String) {
        chatRepository.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "assistant",
                content = content
            )
        )
        
        val session = chatRepository.getSessionById(sessionId)
        session?.let {
            chatRepository.updateSession(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    // 切换工具调用功能
    fun toggleTools(enable: Boolean) {
        _enableTools.value = enable
    }

    fun sendToolMessage(prompt: String, onResult: (String) -> Unit) {
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _toolResult.value = "处理中..."

            try {
                val result = aiApiService.sendSinglePrompt(prompt)
                
                result.fold(
                    onSuccess = { aiText ->
                        _toolResult.value = aiText
                        onResult(aiText)
                    },
                    onFailure = { exception ->
                        val errorMsg = exception.message ?: "AI处理失败"
                        _toolResult.value = errorMsg
                        onResult(errorMsg)
                    }
                )
            } catch (e: Exception) {
                val errorMsg = "错误: ${e.message}"
                _toolResult.value = errorMsg
                onResult(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearToolResult() {
        _toolResult.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            _currentSessionId.value?.let { sessionId ->
                chatRepository.deleteMessagesBySessionId(sessionId)
            }
            _messages.value = emptyList()
            _error.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }
}
