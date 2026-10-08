package com.toolbox.data.local.entity

data class Schedule(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: Long,
    val time: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
