package com.toolbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ocr_history")
data class OcrHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val recognizedText: String,
    val createdAt: Long = System.currentTimeMillis()
)
