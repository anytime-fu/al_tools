package com.toolbox.data.local.entity

data class Folder(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
