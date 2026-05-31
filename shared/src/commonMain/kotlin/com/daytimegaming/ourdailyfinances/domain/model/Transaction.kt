package com.daytimegaming.ourdailyfinances.domain.model

data class Transaction(
    val transactionId: String,
    val accountId: String,
    val amount: Double,
    val date: String,
    val name: String,
    val merchantName: String?,
    val category: List<String>,
    val pending: Boolean,
    val isoCurrencyCode: String?,
)
