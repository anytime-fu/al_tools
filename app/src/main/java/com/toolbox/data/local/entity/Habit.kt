package com.toolbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "⭐",
    val color: Long = 0xFF4CAF50,
    val frequency: String = "daily",
    val createdAt: Long = System.currentTimeMillis()
)
