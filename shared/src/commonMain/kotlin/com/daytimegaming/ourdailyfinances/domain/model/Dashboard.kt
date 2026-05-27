package com.daytimegaming.ourdailyfinances.domain.model

data class Dashboard(
    val dashboardId: String,
    val name: String,
    val ownerUserId: String,
    val inviteCode: String?,
)

data class DashboardDetail(
    val dashboardId: String,
    val name: String,
    val ownerUserId: String,
    val inviteCode: String?,
    val members: List<DashboardMember>,
    val accounts: List<DashboardAccount>,
)

data class DashboardMember(
    val userId: String,
    val joinedAt: String,
)

data class DashboardAccount(
    val accountId: String,
    val name: String,
    val officialName: String?,
    val type: String,
    val subtype: String?,
    val currentBalance: Double?,
    val availableBalance: Double?,
    val isoCurrencyCode: String?,
    val addedByUserId: String,
)
