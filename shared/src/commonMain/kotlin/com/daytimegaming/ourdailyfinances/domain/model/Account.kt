package com.daytimegaming.ourdailyfinances.domain.model

data class Account(
    val accountId: String,
    val name: String,
    val officialName: String?,
    val type: String,
    val subtype: String?,
    val currentBalance: Double?,
    val availableBalance: Double?,
    val isoCurrencyCode: String?,
)
