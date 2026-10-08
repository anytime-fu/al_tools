package com.toolbox.data.local.entity

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
