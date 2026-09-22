package com.toolbox.data.remote

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.toolbox.ai.tools.ToolCall
import com.toolbox.ai.tools.ToolDefinition
import com.toolbox.ai.tools.ToolResult
import com.toolbox.di.EncryptedPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

// Google Gemini 格式
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null
)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user"
)
data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any>?
)
data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any>
)
data class GeminiTool(val functionDeclarations: List<GeminiFunctionDeclaration>)
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>?
)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContent?)

// OpenAI 兼容格式 (DeepSeek等)
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val tools: List<OpenAiTool>? = null
)
data class OpenAiMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null
)
data class OpenAiTool(
    val type: String = "function",
    val function: OpenAiToolFunction
)
data class OpenAiToolFunction(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>?
)
data class OpenAiResponse(val choices: List<OpenAiChoice>?)
data class OpenAiChoice(
    val message: OpenAiMessage?,
    val finish_reason: String? = null
)

// Claude 格式
data class ClaudeRequest(
    val model: String,
    val max_tokens: Int = 2048,
    val messages: List<ClaudeMessage>,
    val tools: List<ClaudeTool>? = null
)
data class ClaudeMessage(
    val role: String,
    val content: String? = null,
    val tool_use: List<ClaudeToolUse>? = null,
    val tool_result: List<ClaudeToolResult>? = null
)
data class ClaudeToolUse(
    val id: String,
    val name: String,
    val input: Map<String, Any>
)
data class ClaudeToolResult(
    val tool_use_id: String,
    val content: String,
    val is_error: Boolean = false
)
data class ClaudeTool(
    val name: String,
    val description: String,
    val input_schema: Map<String, Any>?
)
data class ClaudeResponse(
    val content: List<ClaudeContent>?,
    val stop_reason: String? = null
)
data class ClaudeContent(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: Map<String, Any>? = null
)

// 统一的AI响应
data class AiResponse(
    val text: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val isToolCall: Boolean = false
)

data class ExchangeRateResponse(
    @SerializedName("rates") val rates: Map<String, Double>?
)

@Singleton
class AiApiService @Inject constructor(
    @EncryptedPrefs private val prefs: SharedPreferences,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    companion object {
        // DeepSeek 模型列表
        private val DEEPSEEK_MODELS = listOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner")
        // Claude 模型列表
        private val CLAUDE_MODELS = listOf("claude-3-sonnet", "claude-3-opus", "claude-3-haiku")
        
        // DeepSeek API 地址
        private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com"
        // Claude API 地址
        private const val CLAUDE_BASE_URL = "https://api.anthropic.com"
    }

    private fun getApiKey(): String {
        return prefs.getString("api_key", "") ?: ""
    }

    private fun getModel(): String {
        return prefs.getString("selected_model", "gemini-pro") ?: "gemini-pro"
    }

    private fun getCustomApiUrl(): String? {
        return prefs.getString("custom_api_url", "")?.takeIf { it.isNotBlank() }
    }

    private fun isDeepSeekModel(model: String): Boolean {
        return model in DEEPSEEK_MODELS || getCustomApiUrl()?.contains("deepseek") == true
    }

    private fun isClaudeModel(model: String): Boolean {
        return model in CLAUDE_MODELS || getCustomApiUrl()?.contains("anthropic") == true
    }

    suspend fun sendAiMessage(messages: List<Pair<String, String>>): Result<String> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置API Key"))
        }

        val model = getModel()
        
        return when {
            isDeepSeekModel(model) -> sendOpenAiCompatible(messages, model, apiKey)
            isClaudeModel(model) -> sendClaudeRequest(messages, model, apiKey)
            else -> sendGeminiRequest(messages, model, apiKey)
        }
    }

    suspend fun sendSinglePrompt(prompt: String): Result<String> {
        return sendAiMessage(listOf("user" to prompt))
    }

    // 带工具调用的AI消息
    suspend fun sendAiMessageWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>
    ): Result<AiResponse> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置API Key"))
        }

        val model = getModel()
        
        return when {
            isDeepSeekModel(model) -> sendOpenAiCompatibleWithTools(messages, tools, model, apiKey)
            isClaudeModel(model) -> sendClaudeRequestWithTools(messages, tools, model, apiKey)
            else -> sendGeminiRequestWithTools(messages, tools, model, apiKey)
        }
    }

    // 发送工具执行结果
    suspend fun sendToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>
    ): Result<AiResponse> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置API Key"))
        }

        val model = getModel()
        android.util.Log.d("AiApiService", "sendToolResults called with model: $model")
        android.util.Log.d("AiApiService", "Messages count: ${messages.size}, Tool results count: ${toolResults.size}")
        
        return when {
            isDeepSeekModel(model) -> {
                android.util.Log.d("AiApiService", "Using OpenAI compatible format for DeepSeek")
                sendOpenAiToolResults(messages, toolResults, tools, model, apiKey)
            }
            isClaudeModel(model) -> {
                android.util.Log.d("AiApiService", "Using Claude format")
                sendClaudeToolResults(messages, toolResults, tools, model, apiKey)
            }
            else -> {
                android.util.Log.d("AiApiService", "Using Gemini format")
                sendGeminiToolResults(messages, toolResults, tools, model, apiKey)
            }
        }
    }

    // OpenAI 兼容格式 (DeepSeek)
    private suspend fun sendOpenAiCompatible(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: DEEPSEEK_BASE_URL
                
                val openAiMessages = messages.map { (role, content) ->
                    OpenAiMessage(role = role, content = content)
                }
                
                val request = OpenAiRequest(model = model, messages = openAiMessages)
                val jsonBody = gson.toJson(request)
                android.util.Log.d("AiApiService", "Request URL: $baseUrl/v1/chat/completions")
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = gson.fromJson(responseBody, OpenAiResponse::class.java)
                    val aiText = aiResponse.choices?.firstOrNull()?.message?.content ?: "无响应"
                    Result.success(aiText)
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "API error: ${response.code}")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("AiApiService", "DNS resolution failed", e)
                Result.failure(Exception("无法解析服务器地址，请检查网络连接"))
            } catch (e: java.net.ConnectException) {
                android.util.Log.e("AiApiService", "Connection failed", e)
                Result.failure(Exception("无法连接到服务器，请检查网络连接"))
            } catch (e: javax.net.ssl.SSLException) {
                android.util.Log.e("AiApiService", "SSL error", e)
                Result.failure(Exception("SSL连接错误: ${e.message}"))
            } catch (e: java.io.IOException) {
                android.util.Log.e("AiApiService", "IO error", e)
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "Unknown error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // Claude 格式
    private suspend fun sendClaudeRequest(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: CLAUDE_BASE_URL
                
                val claudeMessages = messages.filter { it.first == "user" || it.first == "assistant" }
                    .map { (role, content) ->
                        ClaudeMessage(role = role, content = content)
                    }
                
                val request = ClaudeRequest(model = model, messages = claudeMessages)
                val jsonBody = gson.toJson(request)
                android.util.Log.d("AiApiService", "Claude Request URL: $baseUrl/v1/messages")
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
                    val aiText = aiResponse.content?.firstOrNull()?.text ?: "无响应"
                    Result.success(aiText)
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "Claude API error: ${response.code}")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("AiApiService", "Claude DNS resolution failed", e)
                Result.failure(Exception("无法解析服务器地址，请检查网络连接"))
            } catch (e: java.net.ConnectException) {
                android.util.Log.e("AiApiService", "Claude Connection failed", e)
                Result.failure(Exception("无法连接到服务器，请检查网络连接"))
            } catch (e: javax.net.ssl.SSLException) {
                android.util.Log.e("AiApiService", "Claude SSL error", e)
                Result.failure(Exception("SSL连接错误: ${e.message}"))
            } catch (e: java.io.IOException) {
                android.util.Log.e("AiApiService", "Claude IO error", e)
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "Claude Unknown error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // Google Gemini 格式
    private suspend fun sendGeminiRequest(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: "https://generativelanguage.googleapis.com"
                
                val geminiContents = messages.map { (role, content) ->
                    GeminiContent(parts = listOf(GeminiPart(text = content)))
                }
                
                val request = GeminiRequest(contents = geminiContents)
                val jsonBody = gson.toJson(request)
                android.util.Log.d("AiApiService", "Gemini Request URL: $baseUrl/v1beta/models/$model:generateContent")
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1beta/models/$model:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                    val aiText = aiResponse.candidates?.firstOrNull()
                        ?.content?.parts?.firstOrNull()?.text ?: "无响应"
                    Result.success(aiText)
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "Gemini API error: ${response.code}")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("AiApiService", "Gemini DNS resolution failed", e)
                Result.failure(Exception("无法解析服务器地址，请检查网络连接"))
            } catch (e: java.net.ConnectException) {
                android.util.Log.e("AiApiService", "Gemini Connection failed", e)
                Result.failure(Exception("无法连接到服务器，请检查网络连接"))
            } catch (e: javax.net.ssl.SSLException) {
                android.util.Log.e("AiApiService", "Gemini SSL error", e)
                Result.failure(Exception("SSL连接错误: ${e.message}"))
            } catch (e: java.io.IOException) {
                android.util.Log.e("AiApiService", "Gemini IO error", e)
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "Gemini Unknown error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    suspend fun getExchangeRates(): Result<Map<String, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.exchangerate-api.com/v4/latest/CNY")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val rateResponse = gson.fromJson(responseBody, ExchangeRateResponse::class.java)
                    val rates = rateResponse.rates ?: emptyMap()
                    Result.success(rates)
                } else {
                    Result.failure(Exception("获取汇率失败: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // ============ Function Calling 实现 ============

    // 将工具定义转换为Gemini格式
    private fun convertToolsToGeminiFormat(tools: List<ToolDefinition>): List<GeminiTool> {
        val declarations = tools.map { tool ->
            GeminiFunctionDeclaration(
                name = tool.name,
                description = tool.description,
                parameters = mapOf(
                    "type" to "object",
                    "properties" to tool.parameters.properties.mapValues { (_, prop) ->
                        mapOf(
                            "type" to prop.type,
                            "description" to prop.description
                        ).let { base ->
                            if (prop.enum != null) base + ("enum" to prop.enum) else base
                        }
                    },
                    "required" to tool.parameters.required
                )
            )
        }
        return listOf(GeminiTool(functionDeclarations = declarations))
    }

    // 将工具定义转换为OpenAI格式
    private fun convertToolsToOpenAiFormat(tools: List<ToolDefinition>): List<OpenAiTool> {
        return tools.map { tool ->
            OpenAiTool(
                function = OpenAiToolFunction(
                    name = tool.name,
                    description = tool.description,
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to tool.parameters.properties.mapValues { (_, prop) ->
                            mapOf(
                                "type" to prop.type,
                                "description" to prop.description
                            ).let { base ->
                                if (prop.enum != null) base + ("enum" to prop.enum) else base
                            }
                        },
                        "required" to tool.parameters.required
                    )
                )
            )
        }
    }

    // 将工具定义转换为Claude格式
    private fun convertToolsToClaudeFormat(tools: List<ToolDefinition>): List<ClaudeTool> {
        return tools.map { tool ->
            ClaudeTool(
                name = tool.name,
                description = tool.description,
                input_schema = mapOf(
                    "type" to "object",
                    "properties" to tool.parameters.properties.mapValues { (_, prop) ->
                        mapOf(
                            "type" to prop.type,
                            "description" to prop.description
                        ).let { base ->
                            if (prop.enum != null) base + ("enum" to prop.enum) else base
                        }
                    },
                    "required" to tool.parameters.required
                )
            )
        }
    }

    // Gemini Function Calling
    private suspend fun sendGeminiRequestWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: "https://generativelanguage.googleapis.com"
                
                val geminiContents = messages.map { (role, content) ->
                    GeminiContent(
                        parts = listOf(GeminiPart(text = content)),
                        role = if (role == "assistant") "model" else role
                    )
                }
                
                val geminiTools = convertToolsToGeminiFormat(tools)
                val request = GeminiRequest(contents = geminiContents, tools = geminiTools)
                val jsonBody = gson.toJson(request)
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1beta/models/$model:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                    val candidate = aiResponse.candidates?.firstOrNull()
                    val parts = candidate?.content?.parts ?: emptyList()
                    
                    // 检查是否有函数调用
                    val functionCalls = parts.filter { it.functionCall != null }.map { part ->
                        val fc = part.functionCall!!
                        ToolCall(
                            id = "call_${System.currentTimeMillis()}",
                            function = com.toolbox.ai.tools.ToolCallFunction(
                                name = fc.name,
                                arguments = gson.toJson(fc.args ?: emptyMap<String, Any>())
                            )
                        )
                    }
                    
                    if (functionCalls.isNotEmpty()) {
                        Result.success(AiResponse(toolCalls = functionCalls, isToolCall = true))
                    } else {
                        val text = parts.firstOrNull()?.text ?: "无响应"
                        Result.success(AiResponse(text = text))
                    }
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "Gemini API error: ${response.code}")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "Gemini Function Calling error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // OpenAI兼容格式 Function Calling (DeepSeek)
    private suspend fun sendOpenAiCompatibleWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: DEEPSEEK_BASE_URL
                
                val openAiMessages = messages.map { (role, content) ->
                    OpenAiMessage(role = role, content = content)
                }
                
                val openAiTools = convertToolsToOpenAiFormat(tools)
                val request = OpenAiRequest(
                    model = model,
                    messages = openAiMessages,
                    tools = openAiTools
                )
                val jsonBody = gson.toJson(request)
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = gson.fromJson(responseBody, OpenAiResponse::class.java)
                    val choice = aiResponse.choices?.firstOrNull()
                    val message = choice?.message
                    
                    if (message?.tool_calls != null && message.tool_calls.isNotEmpty()) {
                        Result.success(AiResponse(toolCalls = message.tool_calls, isToolCall = true))
                    } else {
                        val text = message?.content ?: "无响应"
                        Result.success(AiResponse(text = text))
                    }
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "OpenAI API error: ${response.code}")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "OpenAI Function Calling error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // Claude Function Calling
    private suspend fun sendClaudeRequestWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: CLAUDE_BASE_URL
                
                val claudeMessages = messages.filter { it.first == "user" || it.first == "assistant" }
                    .map { (role, content) ->
                        ClaudeMessage(role = role, content = content)
                    }
                
                val claudeTools = convertToolsToClaudeFormat(tools)
                val request = ClaudeRequest(
                    model = model,
                    messages = claudeMessages,
                    tools = claudeTools
                )
                val jsonBody = gson.toJson(request)
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
                    
                    // 检查是否有工具调用
                    val toolUseBlocks = aiResponse.content?.filter { it.type == "tool_use" } ?: emptyList()
                    
                    if (toolUseBlocks.isNotEmpty()) {
                        val toolCalls = toolUseBlocks.map { block ->
                            ToolCall(
                                id = block.id ?: "call_${System.currentTimeMillis()}",
                                function = com.toolbox.ai.tools.ToolCallFunction(
                                    name = block.name ?: "",
                                    arguments = gson.toJson(block.input ?: emptyMap<String, Any>())
                                )
                            )
                        }
                        Result.success(AiResponse(toolCalls = toolCalls, isToolCall = true))
                    } else {
                        val text = aiResponse.content?.firstOrNull { it.type == "text" }?.text ?: "无响应"
                        Result.success(AiResponse(text = text))
                    }
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "Claude API error: ${response.code}")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "Claude Function Calling error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // 发送工具执行结果给Gemini
    private suspend fun sendGeminiToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: "https://generativelanguage.googleapis.com"
                
                // 构建包含工具结果的消息
                val geminiContents = mutableListOf<GeminiContent>()
                
                // 添加历史消息（排除最后一条assistant的functionCall消息）
                val historyMessages = messages.dropLast(1)
                historyMessages.forEach { (role, content) ->
                    geminiContents.add(GeminiContent(
                        parts = listOf(GeminiPart(text = content)),
                        role = if (role == "assistant") "model" else role
                    ))
                }
                
                // 添加assistant的functionCall消息
                val lastAssistantMessage = messages.lastOrNull()?.second
                if (lastAssistantMessage != null) {
                    try {
                        // 尝试解析functionCall
                        val toolCallType = object : TypeToken<List<Map<String, Any>>>() {}.type
                        val toolCallList: List<Map<String, Any>> = gson.fromJson(lastAssistantMessage, toolCallType)
                        
                        val functionCallParts = toolCallList.map { tc ->
                            val name = tc["name"] as? String ?: ""
                            val args = tc["arguments"] as? Map<String, Any> ?: emptyMap()
                            GeminiPart(functionCall = GeminiFunctionCall(name = name, args = args))
                        }
                        geminiContents.add(GeminiContent(parts = functionCallParts, role = "model"))
                    } catch (e: Exception) {
                        // 如果解析失败，作为普通文本处理
                        geminiContents.add(GeminiContent(
                            parts = listOf(GeminiPart(text = lastAssistantMessage)),
                            role = "model"
                        ))
                    }
                }
                
                // 添加工具执行结果
                toolResults.forEach { result ->
                    geminiContents.add(GeminiContent(
                        parts = listOf(GeminiPart(
                            functionResponse = GeminiFunctionResponse(
                                name = result.toolCallId,
                                response = mapOf("result" to result.result)
                            )
                        )),
                        role = "user"
                    ))
                }
                
                val geminiTools = convertToolsToGeminiFormat(tools)
                val request = GeminiRequest(contents = geminiContents, tools = geminiTools)
                val jsonBody = gson.toJson(request)
                
                android.util.Log.d("AiApiService", "Gemini Tool Results Request: $jsonBody")
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1beta/models/$model:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    android.util.Log.d("AiApiService", "Gemini Tool Results Response: $responseBody")
                    val aiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                    val candidate = aiResponse.candidates?.firstOrNull()
                    val parts = candidate?.content?.parts ?: emptyList()
                    
                    val functionCalls = parts.filter { it.functionCall != null }.map { part ->
                        val fc = part.functionCall!!
                        ToolCall(
                            id = "call_${System.currentTimeMillis()}",
                            function = com.toolbox.ai.tools.ToolCallFunction(
                                name = fc.name,
                                arguments = gson.toJson(fc.args ?: emptyMap<String, Any>())
                            )
                        )
                    }
                    
                    if (functionCalls.isNotEmpty()) {
                        Result.success(AiResponse(toolCalls = functionCalls, isToolCall = true))
                    } else {
                        val text = parts.firstOrNull()?.text ?: "无响应"
                        Result.success(AiResponse(text = text))
                    }
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "Gemini Tool Results error: ${response.code}, body: $errorBody")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "Gemini Tool Results error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // 发送工具执行结果给OpenAI兼容格式
    private suspend fun sendOpenAiToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: DEEPSEEK_BASE_URL
                
                val openAiMessages = mutableListOf<OpenAiMessage>()
                
                // 添加历史消息（排除最后一条assistant的functionCall消息）
                val historyMessages = messages.dropLast(1)
                historyMessages.forEach { (role, content) ->
                    openAiMessages.add(OpenAiMessage(role = role, content = content))
                }
                
                // 添加assistant的tool_calls消息
                val lastAssistantMessage = messages.lastOrNull()?.second
                if (lastAssistantMessage != null) {
                    try {
                        // 解析functionCall JSON
                        val toolCallType = object : TypeToken<List<Map<String, Any>>>() {}.type
                        val toolCallList: List<Map<String, Any>> = gson.fromJson(lastAssistantMessage, toolCallType)
                        
                        val toolCalls = toolCallList.mapIndexed { index, tc ->
                            val name = tc["name"] as? String ?: ""
                            val arguments = tc["arguments"]?.let { gson.toJson(it) } ?: "{}"
                            ToolCall(
                                id = toolResults.getOrNull(index)?.toolCallId ?: "call_${System.currentTimeMillis()}",
                                function = com.toolbox.ai.tools.ToolCallFunction(name = name, arguments = arguments)
                            )
                        }
                        
                        // 添加assistant消息，包含tool_calls
                        openAiMessages.add(OpenAiMessage(
                            role = "assistant",
                            content = null,
                            tool_calls = toolCalls
                        ))
                    } catch (e: Exception) {
                        android.util.Log.e("AiApiService", "Failed to parse tool calls", e)
                        // 如果解析失败，作为普通文本处理
                        openAiMessages.add(OpenAiMessage(role = "assistant", content = lastAssistantMessage))
                    }
                }
                
                // 添加工具执行结果
                toolResults.forEach { result ->
                    openAiMessages.add(OpenAiMessage(
                        role = "tool",
                        content = result.result,
                        tool_call_id = result.toolCallId
                    ))
                }
                
                val openAiTools = convertToolsToOpenAiFormat(tools)
                val request = OpenAiRequest(
                    model = model,
                    messages = openAiMessages,
                    tools = openAiTools
                )
                val jsonBody = gson.toJson(request)
                
                android.util.Log.d("AiApiService", "OpenAI Tool Results Request: $jsonBody")
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    android.util.Log.d("AiApiService", "OpenAI Tool Results Response: $responseBody")
                    val aiResponse = gson.fromJson(responseBody, OpenAiResponse::class.java)
                    val choice = aiResponse.choices?.firstOrNull()
                    val message = choice?.message
                    
                    if (message?.tool_calls != null && message.tool_calls.isNotEmpty()) {
                        Result.success(AiResponse(toolCalls = message.tool_calls, isToolCall = true))
                    } else {
                        val text = message?.content ?: "无响应"
                        Result.success(AiResponse(text = text))
                    }
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("AiApiService", "OpenAI Tool Results error: ${response.code}, body: $errorBody")
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AiApiService", "OpenAI Tool Results error", e)
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }

    // 发送工具执行结果给Claude
    private suspend fun sendClaudeToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val customUrl = getCustomApiUrl()
                val baseUrl = customUrl?.trimEnd('/') ?: CLAUDE_BASE_URL
                
                val claudeMessages = mutableListOf<ClaudeMessage>()
                
                // 添加历史消息
                messages.forEach { (role, content) ->
                    claudeMessages.add(ClaudeMessage(role = role, content = content))
                }
                
                // 添加工具执行结果
                val toolResultList = toolResults.map { result ->
                    ClaudeToolResult(
                        tool_use_id = result.toolCallId,
                        content = result.result,
                        is_error = result.isError
                    )
                }
                claudeMessages.add(ClaudeMessage(
                    role = "user",
                    tool_result = toolResultList
                ))
                
                val claudeTools = convertToolsToClaudeFormat(tools)
                val request = ClaudeRequest(
                    model = model,
                    messages = claudeMessages,
                    tools = claudeTools
                )
                val jsonBody = gson.toJson(request)
                
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val apiRequest = Request.Builder()
                    .url("$baseUrl/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(apiRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
                    
                    val toolUseBlocks = aiResponse.content?.filter { it.type == "tool_use" } ?: emptyList()
                    
                    if (toolUseBlocks.isNotEmpty()) {
                        val toolCalls = toolUseBlocks.map { block ->
                            ToolCall(
                                id = block.id ?: "call_${System.currentTimeMillis()}",
                                function = com.toolbox.ai.tools.ToolCallFunction(
                                    name = block.name ?: "",
                                    arguments = gson.toJson(block.input ?: emptyMap<String, Any>())
                                )
                            )
                        }
                        Result.success(AiResponse(toolCalls = toolCalls, isToolCall = true))
                    } else {
                        val text = aiResponse.content?.firstOrNull { it.type == "text" }?.text ?: "无响应"
                        Result.success(AiResponse(text = text))
                    }
                } else {
                    Result.failure(Exception("API错误: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
    }
}
