package com.toolbox.data.local.entity

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val folderId: Long? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
