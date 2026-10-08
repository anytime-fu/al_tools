package com.toolbox.data.local.entity

data class OcrHistory(
    val id: Long = 0,
    val imagePath: String? = null,
    val recognizedText: String,
    val language: String = "zh",
    val createdAt: Long = System.currentTimeMillis()
)
