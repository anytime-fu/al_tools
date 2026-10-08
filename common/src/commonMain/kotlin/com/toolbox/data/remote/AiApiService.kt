package com.toolbox.data.remote

import com.toolbox.ai.tools.ToolCall
import com.toolbox.ai.tools.ToolCallFunction
import com.toolbox.ai.tools.ToolDefinition
import com.toolbox.ai.tools.ToolResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// Request/Response models
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
    val role: String = "user"
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)

@Serializable
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, JsonElement>? = null
)

@Serializable
data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, JsonElement>
)

@Serializable
data class GeminiTool(val functionDeclarations: List<GeminiFunctionDeclaration>)

@Serializable
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, JsonElement>? = null
)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null)

// OpenAI compatible format
@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val tools: List<OpenAiTool>? = null,
    val stream: Boolean = false
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null
)

@Serializable
data class OpenAiTool(
    val type: String = "function",
    val function: OpenAiToolFunction
)

@Serializable
data class OpenAiToolFunction(
    val name: String,
    val description: String,
    val parameters: Map<String, JsonElement>? = null
)

@Serializable
data class OpenAiResponse(val choices: List<OpenAiChoice>? = null)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage? = null,
    val delta: OpenAiDelta? = null,
    val finish_reason: String? = null
)

@Serializable
data class OpenAiDelta(
    val content: String? = null
)

// Claude format
@Serializable
data class ClaudeRequest(
    val model: String,
    val max_tokens: Int = 2048,
    val messages: List<ClaudeMessage>,
    val tools: List<ClaudeTool>? = null,
    val stream: Boolean = false
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String? = null,
    val tool_use: List<ClaudeToolUse>? = null,
    val tool_result: List<ClaudeToolResult>? = null
)

@Serializable
data class ClaudeToolUse(
    val id: String,
    val name: String,
    val input: Map<String, JsonElement>
)

@Serializable
data class ClaudeToolResult(
    val tool_use_id: String,
    val content: String,
    val is_error: Boolean = false
)

@Serializable
data class ClaudeTool(
    val name: String,
    val description: String,
    val input_schema: Map<String, JsonElement>? = null
)

@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContent>? = null,
    val stop_reason: String? = null
)

@Serializable
data class ClaudeContent(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: Map<String, JsonElement>? = null
)

// Unified AI response
@Serializable
data class AiResponse(
    val text: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val isToolCall: Boolean = false
)

@Serializable
data class ExchangeRateResponse(
    val rates: Map<String, Double>? = null
)

interface AiApiService {
    suspend fun sendAiMessage(messages: List<Pair<String, String>>): Result<String>
    suspend fun sendSinglePrompt(prompt: String): Result<String>
    suspend fun sendAiMessageWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>
    ): Result<AiResponse>
    suspend fun sendToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>
    ): Result<AiResponse>
    suspend fun getExchangeRates(): Result<Map<String, Double>>
    fun sendAiMessageStream(messages: List<Pair<String, String>>): Flow<String>
}

class AiApiServiceImpl(
    private val httpClient: HttpClient,
    private var config: AiConfig
) : AiApiService {
    
    companion object {
        private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com"
        private const val CLAUDE_BASE_URL = "https://api.anthropic.com"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com"
    }
    
    fun updateConfig(newConfig: AiConfig) {
        config = newConfig
    }
    
    private fun isClaudeModel(model: String): Boolean {
        return model.startsWith("claude") || config.customApiUrl?.contains("anthropic") == true
    }
    
    private fun isGeminiModel(model: String): Boolean {
        if (model.startsWith("gemini")) return true
        val url = config.customApiUrl ?: return false
        return url.contains("googleapis") || url.contains("generativelanguage") || url.contains("gemini")
    }
    
    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        throw Exception("API错误 ${response.status.value}: ${extractApiErrorMessage(body)}")
    }
    
    private fun extractApiErrorMessage(body: String): String {
        val message = try {
            val json = Json.parseToJsonElement(body) as? JsonObject
            val error = json?.get("error") as? JsonObject
            (error?.get("message") as? JsonPrimitive)?.contentOrNull
        } catch (e: Exception) {
            null
        }
        return message ?: body.trim().take(200).ifEmpty { "无错误详情" }
    }
    
    override suspend fun sendAiMessage(messages: List<Pair<String, String>>): Result<String> {
        val apiKey = config.apiKey
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置API Key"))
        }
        
        val model = config.model
        
        return when {
            isClaudeModel(model) -> sendClaudeRequest(messages, model, apiKey)
            isGeminiModel(model) -> sendGeminiRequest(messages, model, apiKey)
            else -> sendOpenAiCompatible(messages, model, apiKey)
        }
    }
    
    override suspend fun sendSinglePrompt(prompt: String): Result<String> {
        return sendAiMessage(listOf("user" to prompt))
    }
    
    override suspend fun sendAiMessageWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>
    ): Result<AiResponse> {
        val apiKey = config.apiKey
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置API Key"))
        }
        
        val model = config.model
        
        return when {
            isClaudeModel(model) -> sendClaudeRequestWithTools(messages, tools, model, apiKey)
            isGeminiModel(model) -> sendGeminiRequestWithTools(messages, tools, model, apiKey)
            else -> sendOpenAiCompatibleWithTools(messages, tools, model, apiKey)
        }
    }
    
    override suspend fun sendToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>
    ): Result<AiResponse> {
        val apiKey = config.apiKey
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置API Key"))
        }
        
        val model = config.model
        
        return when {
            isClaudeModel(model) -> sendClaudeToolResults(messages, toolResults, tools, model, apiKey)
            isGeminiModel(model) -> sendGeminiToolResults(messages, toolResults, tools, model, apiKey)
            else -> sendOpenAiToolResults(messages, toolResults, tools, model, apiKey)
        }
    }
    
    override suspend fun getExchangeRates(): Result<Map<String, Double>> {
        return withContext(Dispatchers.Default) {
            try {
                val response = httpClient.get("https://api.exchangerate-api.com/v4/latest/CNY")
                val rateResponse = response.body<ExchangeRateResponse>()
                Result.success(rateResponse.rates ?: emptyMap())
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }
    
    override fun sendAiMessageStream(messages: List<Pair<String, String>>): Flow<String> = flow {
        val apiKey = config.apiKey
        if (apiKey.isBlank()) {
            throw Exception("请先在设置中配置API Key")
        }
        
        val model = config.model
        
        when {
            isClaudeModel(model) -> {
                sendClaudeStream(messages, model, apiKey).collect { emit(it) }
            }
            isGeminiModel(model) -> {
                sendGeminiStream(messages, model, apiKey).collect { emit(it) }
            }
            else -> {
                sendOpenAiCompatibleStream(messages, model, apiKey).collect { emit(it) }
            }
        }
    }.flowOn(Dispatchers.Default)
    
    private fun sendOpenAiCompatibleStream(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Flow<String> = flow {
        val baseUrl = config.customApiUrl?.trimEnd('/') ?: DEEPSEEK_BASE_URL
        
        val openAiMessages = messages.map { (role, content) ->
            OpenAiMessage(role = role, content = content)
        }
        
        val request = OpenAiRequest(model = model, messages = openAiMessages, stream = true)
        
        val response = httpClient.preparePost("$baseUrl/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.execute()
        ensureSuccess(response)
        
        val channel = response.bodyAsChannel()
        
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: continue
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                
                try {
                    val json = Json.parseToJsonElement(data) as? JsonObject ?: continue
                    val choices = json["choices"] as? JsonArray ?: continue
                    if (choices.isEmpty()) continue
                    
                    val choice = choices[0] as? JsonObject ?: continue
                    val delta = choice["delta"] as? JsonObject ?: continue
                    val content = delta["content"] as? JsonPrimitive ?: continue
                    val text = content.contentOrNull ?: continue
                    
                    if (text.isNotEmpty()) {
                        emit(text)
                    }
                } catch (e: Exception) {
                    // Skip parsing errors
                }
            }
        }
    }
    
    private fun sendGeminiStream(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Flow<String> = flow {
        val baseUrl = config.customApiUrl?.trimEnd('/') ?: GEMINI_BASE_URL
        
        val geminiContents = messages.map { (role, content) ->
            GeminiContent(
                parts = listOf(GeminiPart(text = content)),
                role = if (role == "assistant") "model" else role
            )
        }
        
        val request = GeminiRequest(contents = geminiContents)
        
        val response = httpClient.preparePost("$baseUrl/v1beta/models/$model:streamGenerateContent") {
            parameter("alt", "sse")
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.execute()
        ensureSuccess(response)
        
        val channel = response.bodyAsChannel()
        
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: continue
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ").trim()
            if (data.isEmpty() || data == "[DONE]") continue
            
            try {
                val json = Json.parseToJsonElement(data) as? JsonObject ?: continue
                val candidates = json["candidates"] as? JsonArray ?: continue
                if (candidates.isEmpty()) continue
                val candidate = candidates[0] as? JsonObject ?: continue
                val content = candidate["content"] as? JsonObject ?: continue
                val parts = content["parts"] as? JsonArray ?: continue
                
                for (part in parts) {
                    val partObj = part as? JsonObject ?: continue
                    val text = (partObj["text"] as? JsonPrimitive)?.contentOrNull ?: continue
                    if (text.isNotEmpty()) {
                        emit(text)
                    }
                }
            } catch (e: Exception) {
                // Skip parsing errors
            }
        }
    }
    
    private fun sendClaudeStream(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Flow<String> = flow {
        val baseUrl = config.customApiUrl?.trimEnd('/') ?: CLAUDE_BASE_URL
        
        val claudeMessages = messages.filter { it.first == "user" || it.first == "assistant" }
            .map { (role, content) ->
                ClaudeMessage(role = role, content = content)
            }
        
        val request = ClaudeRequest(model = model, messages = claudeMessages, stream = true)
        
        val response = httpClient.preparePost("$baseUrl/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.execute()
        ensureSuccess(response)
        
        val channel = response.bodyAsChannel()
        
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: continue
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ").trim()
            if (data.isEmpty()) continue
            
            try {
                val json = Json.parseToJsonElement(data) as? JsonObject ?: continue
                when ((json["type"] as? JsonPrimitive)?.contentOrNull) {
                    "content_block_delta" -> {
                        val delta = json["delta"] as? JsonObject ?: continue
                        val text = (delta["text"] as? JsonPrimitive)?.contentOrNull ?: continue
                        if (text.isNotEmpty()) {
                            emit(text)
                        }
                    }
                    "message_stop" -> break
                    else -> Unit
                }
            } catch (e: Exception) {
                // Skip parsing errors
            }
        }
    }
    
    private suspend fun sendGeminiRequest(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: "https://generativelanguage.googleapis.com"
                
                val geminiContents = messages.map { (role, content) ->
                    GeminiContent(parts = listOf(GeminiPart(text = content)))
                }
                
                val request = GeminiRequest(contents = geminiContents)
                
                val response = httpClient.post("$baseUrl/v1beta/models/$model:generateContent") {
                    parameter("key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<GeminiResponse>()
                val aiText = aiResponse.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text ?: "无响应"
                Result.success(aiText)
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private suspend fun sendOpenAiCompatible(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: DEEPSEEK_BASE_URL
                
                val openAiMessages = messages.map { (role, content) ->
                    OpenAiMessage(role = role, content = content)
                }
                
                val request = OpenAiRequest(model = model, messages = openAiMessages)
                
                val response = httpClient.post("$baseUrl/v1/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<OpenAiResponse>()
                val aiText = aiResponse.choices?.firstOrNull()?.message?.content ?: "无响应"
                Result.success(aiText)
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private suspend fun sendClaudeRequest(
        messages: List<Pair<String, String>>,
        model: String,
        apiKey: String
    ): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: CLAUDE_BASE_URL
                
                val claudeMessages = messages.filter { it.first == "user" || it.first == "assistant" }
                    .map { (role, content) ->
                        ClaudeMessage(role = role, content = content)
                    }
                
                val request = ClaudeRequest(model = model, messages = claudeMessages)
                
                val response = httpClient.post("$baseUrl/v1/messages") {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<ClaudeResponse>()
                val aiText = aiResponse.content?.firstOrNull()?.text ?: "无响应"
                Result.success(aiText)
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private suspend fun sendGeminiRequestWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: "https://generativelanguage.googleapis.com"
                
                val geminiContents = messages.map { (role, content) ->
                    GeminiContent(
                        parts = listOf(GeminiPart(text = content)),
                        role = if (role == "assistant") "model" else role
                    )
                }
                
                val geminiTools = convertToolsToGeminiFormat(tools)
                val request = GeminiRequest(contents = geminiContents, tools = geminiTools)
                
                val response = httpClient.post("$baseUrl/v1beta/models/$model:generateContent") {
                    parameter("key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<GeminiResponse>()
                val candidate = aiResponse.candidates?.firstOrNull()
                val parts = candidate?.content?.parts ?: emptyList()
                
                val functionCalls = parts.filter { it.functionCall != null }.mapIndexed { index, part ->
                    val fc = part.functionCall!!
                    ToolCall(
                        id = "call_${System.currentTimeMillis()}_$index",
                        function = ToolCallFunction(
                            name = fc.name,
                            arguments = JsonObject(fc.args ?: emptyMap()).toString()
                        )
                    )
                }
                
                if (functionCalls.isNotEmpty()) {
                    Result.success(AiResponse(toolCalls = functionCalls, isToolCall = true))
                } else {
                    val text = parts.firstOrNull()?.text ?: "无响应"
                    Result.success(AiResponse(text = text))
                }
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private suspend fun sendOpenAiCompatibleWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: DEEPSEEK_BASE_URL
                
                val openAiMessages = messages.map { (role, content) ->
                    OpenAiMessage(role = role, content = content)
                }
                
                val openAiTools = convertToolsToOpenAiFormat(tools)
                val request = OpenAiRequest(model = model, messages = openAiMessages, tools = openAiTools)
                
                val response = httpClient.post("$baseUrl/v1/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<OpenAiResponse>()
                val choice = aiResponse.choices?.firstOrNull()
                val message = choice?.message
                
                if (message?.tool_calls != null && message.tool_calls.isNotEmpty()) {
                    Result.success(AiResponse(toolCalls = message.tool_calls, isToolCall = true))
                } else {
                    val text = message?.content ?: "无响应"
                    Result.success(AiResponse(text = text))
                }
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private suspend fun sendClaudeRequestWithTools(
        messages: List<Pair<String, String>>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: CLAUDE_BASE_URL
                
                val claudeMessages = messages.filter { it.first == "user" || it.first == "assistant" }
                    .map { (role, content) ->
                        ClaudeMessage(role = role, content = content)
                    }
                
                val claudeTools = convertToolsToClaudeFormat(tools)
                val request = ClaudeRequest(model = model, messages = claudeMessages, tools = claudeTools)
                
                val response = httpClient.post("$baseUrl/v1/messages") {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<ClaudeResponse>()
                
                val toolUseBlocks = aiResponse.content?.filter { it.type == "tool_use" } ?: emptyList()
                
                if (toolUseBlocks.isNotEmpty()) {
                    val toolCalls = toolUseBlocks.map { block ->
                        ToolCall(
                            id = block.id ?: "call_${System.currentTimeMillis()}",
                            function = ToolCallFunction(
                                name = block.name ?: "",
                                arguments = JsonObject(block.input ?: emptyMap()).toString()
                            )
                        )
                    }
                    Result.success(AiResponse(toolCalls = toolCalls, isToolCall = true))
                } else {
                    val text = aiResponse.content?.firstOrNull { it.type == "text" }?.text ?: "无响应"
                    Result.success(AiResponse(text = text))
                }
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private fun parseToolCallsJson(json: String?, toolResults: List<ToolResult>): List<ToolCall> {
        val fallback = toolResults.map { result ->
            ToolCall(
                id = result.toolCallId,
                function = ToolCallFunction(name = result.functionName, arguments = "{}")
            )
        }
        if (json.isNullOrBlank()) return fallback
        return try {
            val array = Json.parseToJsonElement(json) as? JsonArray ?: return fallback
            val parsed = array.mapIndexedNotNull { index, element ->
                val obj = element as? JsonObject ?: return@mapIndexedNotNull null
                val name = (obj["name"] as? JsonPrimitive)?.contentOrNull ?: return@mapIndexedNotNull null
                val argsElement = obj["arguments"]
                val argsString = when (argsElement) {
                    is JsonObject -> argsElement.toString()
                    is JsonPrimitive -> argsElement.contentOrNull ?: "{}"
                    else -> "{}"
                }
                ToolCall(
                    id = toolResults.getOrNull(index)?.toolCallId ?: "call_${System.currentTimeMillis()}_$index",
                    function = ToolCallFunction(name = name, arguments = argsString)
                )
            }
            if (parsed.isEmpty()) fallback else parsed
        } catch (e: Exception) {
            fallback
        }
    }
    
    private fun parseArgsObject(json: String?): JsonObject {
        if (json.isNullOrBlank()) return JsonObject(emptyMap())
        return try {
            Json.parseToJsonElement(json) as? JsonObject ?: JsonObject(emptyMap())
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
    }
    
    private suspend fun sendGeminiToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: GEMINI_BASE_URL
                
                val geminiContents = mutableListOf<GeminiContent>()
                
                messages.dropLast(1).forEach { (role, content) ->
                    geminiContents.add(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = content)),
                            role = if (role == "assistant") "model" else role
                        )
                    )
                }
                
                val toolCalls = parseToolCallsJson(messages.lastOrNull()?.second, toolResults)
                geminiContents.add(
                    GeminiContent(
                        parts = toolCalls.map { call ->
                            GeminiPart(
                                functionCall = GeminiFunctionCall(
                                    name = call.function.name,
                                    args = parseArgsObject(call.function.arguments)
                                )
                            )
                        },
                        role = "model"
                    )
                )
                
                toolResults.forEach { result ->
                    geminiContents.add(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(
                                    functionResponse = GeminiFunctionResponse(
                                        name = result.functionName,
                                        response = mapOf("result" to JsonPrimitive(result.result))
                                    )
                                )
                            ),
                            role = "user"
                        )
                    )
                }
                
                val request = GeminiRequest(
                    contents = geminiContents,
                    tools = convertToolsToGeminiFormat(tools)
                )
                
                val response = httpClient.post("$baseUrl/v1beta/models/$model:generateContent") {
                    parameter("key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<GeminiResponse>()
                val candidate = aiResponse.candidates?.firstOrNull()
                val parts = candidate?.content?.parts ?: emptyList()
                
                val functionCalls = parts.filter { it.functionCall != null }.mapIndexed { index, part ->
                    val fc = part.functionCall!!
                    ToolCall(
                        id = "call_${System.currentTimeMillis()}_$index",
                        function = ToolCallFunction(
                            name = fc.name,
                            arguments = JsonObject(fc.args ?: emptyMap()).toString()
                        )
                    )
                }
                
                if (functionCalls.isNotEmpty()) {
                    Result.success(AiResponse(toolCalls = functionCalls, isToolCall = true))
                } else {
                    val text = parts.firstOrNull()?.text ?: "无响应"
                    Result.success(AiResponse(text = text))
                }
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private suspend fun sendOpenAiToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: DEEPSEEK_BASE_URL
                
                val openAiMessages = mutableListOf<OpenAiMessage>()
                
                messages.dropLast(1).forEach { (role, content) ->
                    openAiMessages.add(OpenAiMessage(role = role, content = content))
                }
                
                val toolCalls = parseToolCallsJson(messages.lastOrNull()?.second, toolResults)
                openAiMessages.add(
                    OpenAiMessage(
                        role = "assistant",
                        content = null,
                        tool_calls = toolCalls
                    )
                )
                
                toolResults.forEach { result ->
                    openAiMessages.add(
                        OpenAiMessage(
                            role = "tool",
                            content = result.result,
                            tool_call_id = result.toolCallId
                        )
                    )
                }
                
                val request = OpenAiRequest(
                    model = model,
                    messages = openAiMessages,
                    tools = convertToolsToOpenAiFormat(tools)
                )
                
                val response = httpClient.post("$baseUrl/v1/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<OpenAiResponse>()
                val message = aiResponse.choices?.firstOrNull()?.message
                
                if (message?.tool_calls != null && message.tool_calls.isNotEmpty()) {
                    Result.success(AiResponse(toolCalls = message.tool_calls, isToolCall = true))
                } else {
                    val text = message?.content ?: "无响应"
                    Result.success(AiResponse(text = text))
                }
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private suspend fun sendClaudeToolResults(
        messages: List<Pair<String, String>>,
        toolResults: List<ToolResult>,
        tools: List<ToolDefinition>,
        model: String,
        apiKey: String
    ): Result<AiResponse> {
        return withContext(Dispatchers.Default) {
            try {
                val baseUrl = config.customApiUrl?.trimEnd('/') ?: CLAUDE_BASE_URL
                
                val claudeMessages = mutableListOf<JsonObject>()
                
                messages.dropLast(1).filter { it.first == "user" || it.first == "assistant" }
                    .forEach { (role, content) ->
                        claudeMessages.add(
                            buildJsonObject {
                                put("role", JsonPrimitive(role))
                                put("content", JsonPrimitive(content))
                            }
                        )
                    }
                
                val toolCalls = parseToolCallsJson(messages.lastOrNull()?.second, toolResults)
                claudeMessages.add(
                    buildJsonObject {
                        put("role", JsonPrimitive("assistant"))
                        put(
                            "content",
                            JsonArray(
                                toolCalls.map { call ->
                                    buildJsonObject {
                                        put("type", JsonPrimitive("tool_use"))
                                        put("id", JsonPrimitive(call.id))
                                        put("name", JsonPrimitive(call.function.name))
                                        put("input", parseArgsObject(call.function.arguments))
                                    }
                                }
                            )
                        )
                    }
                )
                
                claudeMessages.add(
                    buildJsonObject {
                        put("role", JsonPrimitive("user"))
                        put(
                            "content",
                            JsonArray(
                                toolResults.map { result ->
                                    buildJsonObject {
                                        put("type", JsonPrimitive("tool_result"))
                                        put("tool_use_id", JsonPrimitive(result.toolCallId))
                                        put("content", JsonPrimitive(result.result))
                                        put("is_error", JsonPrimitive(result.isError))
                                    }
                                }
                            )
                        )
                    }
                )
                
                val requestBody = buildJsonObject {
                    put("model", JsonPrimitive(model))
                    put("max_tokens", JsonPrimitive(2048))
                    put("messages", JsonArray(claudeMessages))
                    put("tools", Json.encodeToJsonElement(convertToolsToClaudeFormat(tools)))
                }
                
                val response = httpClient.post("$baseUrl/v1/messages") {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(TextContent(requestBody.toString(), ContentType.Application.Json))
                }
                
                ensureSuccess(response)
                val aiResponse = response.body<ClaudeResponse>()
                val toolUseBlocks = aiResponse.content?.filter { it.type == "tool_use" } ?: emptyList()
                
                if (toolUseBlocks.isNotEmpty()) {
                    val toolCallsFromResponse = toolUseBlocks.map { block ->
                        ToolCall(
                            id = block.id ?: "call_${System.currentTimeMillis()}",
                            function = ToolCallFunction(
                                name = block.name ?: "",
                                arguments = JsonObject(block.input ?: emptyMap()).toString()
                            )
                        )
                    }
                    Result.success(AiResponse(toolCalls = toolCallsFromResponse, isToolCall = true))
                } else {
                    val text = aiResponse.content?.firstOrNull { it.type == "text" }?.text ?: "无响应"
                    Result.success(AiResponse(text = text))
                }
            } catch (e: Exception) {
                Result.failure(Exception("错误: ${e.message}"))
            }
        }
    }
    
    private fun convertToolsToGeminiFormat(tools: List<ToolDefinition>): List<GeminiTool> {
        val declarations = tools.map { tool ->
            GeminiFunctionDeclaration(
                name = tool.name,
                description = tool.description,
                parameters = buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {
                        tool.parameters.properties.forEach { (name, prop) ->
                            put(name, buildJsonObject {
                                put("type", JsonPrimitive(prop.type))
                                put("description", JsonPrimitive(prop.description))
                                prop.enum?.let { enum ->
                                    put("enum", JsonArray(enum.map { JsonPrimitive(it) }))
                                }
                            })
                        }
                    })
                    put("required", JsonArray(tool.parameters.required.map { JsonPrimitive(it) }))
                }
            )
        }
        return listOf(GeminiTool(functionDeclarations = declarations))
    }
    
    private fun convertToolsToOpenAiFormat(tools: List<ToolDefinition>): List<OpenAiTool> {
        return tools.map { tool ->
            OpenAiTool(
                function = OpenAiToolFunction(
                    name = tool.name,
                    description = tool.description,
                    parameters = buildJsonObject {
                        put("type", JsonPrimitive("object"))
                        put("properties", buildJsonObject {
                            tool.parameters.properties.forEach { (name, prop) ->
                                put(name, buildJsonObject {
                                    put("type", JsonPrimitive(prop.type))
                                    put("description", JsonPrimitive(prop.description))
                                    prop.enum?.let { enum ->
                                        put("enum", JsonArray(enum.map { JsonPrimitive(it) }))
                                    }
                                })
                            }
                        })
                        put("required", JsonArray(tool.parameters.required.map { JsonPrimitive(it) }))
                    }
                )
            )
        }
    }
    
    private fun convertToolsToClaudeFormat(tools: List<ToolDefinition>): List<ClaudeTool> {
        return tools.map { tool ->
            ClaudeTool(
                name = tool.name,
                description = tool.description,
                input_schema = buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {
                        tool.parameters.properties.forEach { (name, prop) ->
                            put(name, buildJsonObject {
                                put("type", JsonPrimitive(prop.type))
                                put("description", JsonPrimitive(prop.description))
                                prop.enum?.let { enum ->
                                    put("enum", JsonArray(enum.map { JsonPrimitive(it) }))
                                }
                            })
                        }
                    })
                    put("required", JsonArray(tool.parameters.required.map { JsonPrimitive(it) }))
                }
            )
        }
    }
}

data class AiConfig(
    val apiKey: String = "",
    val model: String = "gemini-2.0-flash",
    val customApiUrl: String? = null
)
