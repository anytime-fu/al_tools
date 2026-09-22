package com.toolbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val icon: String = "💰",
    val color: Long = 0xFF4CAF50,
    val createdAt: Long = System.currentTimeMillis()
)
