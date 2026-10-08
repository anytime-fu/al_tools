package com.toolbox.data.local.entity

data class PasswordEntry(
    val id: Long = 0,
    val appName: String,
    val account: String,
    val password: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
