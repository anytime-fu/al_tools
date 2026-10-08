package com.toolbox.data.local.entity

data class Habit(
    val id: Long = 0,
    val name: String,
    val icon: String = "⭐",
    val color: Long = 0xFF4CAF50,
    val frequency: String = "daily",
    val createdAt: Long = System.currentTimeMillis()
)
