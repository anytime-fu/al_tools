package com.toolbox.data.local.entity

data class Setting(
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
