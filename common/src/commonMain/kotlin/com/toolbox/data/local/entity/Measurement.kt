package com.toolbox.data.local.entity

data class Measurement(
    val id: Long = 0,
    val name: String,
    val value: Double,
    val unit: String,
    val type: String,
    val createdAt: Long = System.currentTimeMillis()
)
