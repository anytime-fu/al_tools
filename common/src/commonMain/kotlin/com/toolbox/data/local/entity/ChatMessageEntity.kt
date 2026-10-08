package com.toolbox.data.local.entity

data class ChatMessageEntity(
    val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
