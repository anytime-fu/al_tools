package com.toolbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val pointsJson: String = "",
    val totalDistanceCm: Double = 0.0,
    val pointCount: Int = 0,
    val unit: String = "cm",
    val createdAt: Long = System.currentTimeMillis()
)
