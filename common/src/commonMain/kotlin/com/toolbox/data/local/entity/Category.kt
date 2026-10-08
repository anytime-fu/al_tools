package com.toolbox.data.local.entity

data class Category(
    val id: Long = 0,
    val name: String,
    val type: String,
    val icon: String = "💰",
    val color: Long = 0xFF4CAF50,
    val createdAt: Long = System.currentTimeMillis()
)
