package com.daytimegaming.ourdailyfinances.domain.model

data class DashboardInvite(
    val code: String,
    val createdAt: String,
)

data class JoinedDashboard(
    val dashboardId: String,
    val name: String,
)

data class Dashboard(
    val dashboardId: String,
    val name: String,
    val ownerUserId: String,
    val invites: List<DashboardInvite>?,
)

data class DashboardDetail(
    val dashboardId: String,
    val name: String,
    val ownerUserId: String,
    val invites: List<DashboardInvite>?,
    val members: List<DashboardMember>,
    val accounts: List<DashboardAccount>,
    val transactions: List<Transaction> = emptyList(),
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
