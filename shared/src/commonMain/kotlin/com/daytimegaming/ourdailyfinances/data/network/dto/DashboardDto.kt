package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DashboardDto(
    @SerialName("dashboard_id") val dashboardId: String,
    @SerialName("name") val name: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("invite_code") val inviteCode: String? = null,
)

@Serializable
data class DashboardListResponse(
    @SerialName("dashboards") val dashboards: List<DashboardDto>,
)

@Serializable
data class DashboardMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String,
)

@Serializable
data class DashboardAccountDto(
    @SerialName("account_id") val accountId: String,
    @SerialName("name") val name: String,
    @SerialName("official_name") val officialName: String? = null,
    @SerialName("type") val type: String,
    @SerialName("subtype") val subtype: String? = null,
    @SerialName("current_balance") val currentBalance: Double? = null,
    @SerialName("available_balance") val availableBalance: Double? = null,
    @SerialName("iso_currency_code") val isoCurrencyCode: String? = null,
    @SerialName("added_by_user_id") val addedByUserId: String,
)

@Serializable
data class DashboardDetailDto(
    @SerialName("dashboard_id") val dashboardId: String,
    @SerialName("name") val name: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("members") val members: List<DashboardMemberDto>,
    @SerialName("accounts") val accounts: List<DashboardAccountDto>,
    @SerialName("transactions") val transactions: List<TransactionDto> = emptyList(),
)

@Serializable
data class CreateDashboardRequest(
    @SerialName("name") val name: String,
)

@Serializable
data class AddDashboardAccountRequest(
    @SerialName("account_id") val accountId: String,
)

@Serializable
data class MessageResponse(
    @SerialName("message") val message: String,
)
