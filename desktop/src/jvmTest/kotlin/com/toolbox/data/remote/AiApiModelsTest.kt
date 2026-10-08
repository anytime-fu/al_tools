package com.toolbox.data.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertNull

class AiApiModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun openAiErrorBodyWithoutChoicesDoesNotThrow() {
        val parsed = json.decodeFromString<OpenAiResponse>("""{"error":{"message":"Invalid API key"}}""")
        assertNull(parsed.choices)
    }

    @Test
    fun openAiEmptyObjectDoesNotThrow() {
        val parsed = json.decodeFromString<OpenAiResponse>("{}")
        assertNull(parsed.choices)
    }

    @Test
    fun geminiErrorBodyWithoutCandidatesDoesNotThrow() {
        val parsed = json.decodeFromString<GeminiResponse>("""{"error":{"message":"quota exceeded"}}""")
        assertNull(parsed.candidates)
    }

    @Test
    fun claudeErrorBodyWithoutContentDoesNotThrow() {
        val parsed = json.decodeFromString<ClaudeResponse>("""{"type":"error","error":{"message":"boom"}}""")
        assertNull(parsed.content)
    }
}