package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    @SerialName("account_id") val accountId: String,
    @SerialName("name") val name: String,
    @SerialName("official_name") val officialName: String? = null,
    @SerialName("type") val type: String,
    @SerialName("subtype") val subtype: String? = null,
    @SerialName("current_balance") val currentBalance: Double? = null,
    @SerialName("available_balance") val availableBalance: Double? = null,
    @SerialName("iso_currency_code") val isoCurrencyCode: String? = null,
)

@Serializable
data class AccountListResponse(
    @SerialName("accounts") val accounts: List<AccountDto>,
)
