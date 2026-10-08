package com.toolbox.data.local.entity

data class HabitRecord(
    val id: Long = 0,
    val habitId: Long,
    val date: Long,
    val isCompleted: Boolean = false
)
