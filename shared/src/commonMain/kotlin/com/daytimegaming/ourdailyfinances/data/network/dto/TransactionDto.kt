package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("date") val date: String,
    @SerialName("name") val name: String,
    @SerialName("merchant_name") val merchantName: String? = null,
    @SerialName("category") val category: List<String>,
    @SerialName("pending") val pending: Boolean,
    @SerialName("iso_currency_code") val isoCurrencyCode: String? = null,
)

@Serializable
data class TransactionsResponse(
    @SerialName("transactions") val transactions: List<TransactionDto>,
)
