package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.DashboardService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.domain.model.DashboardAccount
import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail
import com.daytimegaming.ourdailyfinances.domain.model.DashboardMember
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import com.daytimegaming.ourdailyfinances.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DashboardRepositoryImpl(private val service: DashboardService) : DashboardRepository {

    override fun getDashboards(): Flow<Response<List<Dashboard>>> = flow {
        emit(Response.Loading)
        try {
            val response = service.getDashboards()
            val dashboards = response.dashboards.map { dto ->
                Dashboard(
                    dashboardId = dto.dashboardId,
                    name = dto.name,
                    ownerUserId = dto.ownerUserId,
                    inviteCode = dto.inviteCode,
                )
            }
            emit(Response.Success(dashboards))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to load dashboards"))
        }
    }

    override fun getDashboardDetail(dashboardId: String): Flow<Response<DashboardDetail>> = flow {
        emit(Response.Loading)
        try {
            val dto = service.getDashboardDetail(dashboardId)
            val detail = DashboardDetail(
                dashboardId = dto.dashboardId,
                name = dto.name,
                ownerUserId = dto.ownerUserId,
                inviteCode = dto.inviteCode,
                members = dto.members.map { DashboardMember(it.userId, it.joinedAt) },
                accounts = dto.accounts.map { a ->
                    DashboardAccount(
                        accountId = a.accountId,
                        name = a.name,
                        officialName = a.officialName,
                        type = a.type,
                        subtype = a.subtype,
                        currentBalance = a.currentBalance,
                        availableBalance = a.availableBalance,
                        isoCurrencyCode = a.isoCurrencyCode,
                        addedByUserId = a.addedByUserId,
                    )
                },
                transactions = dto.transactions.map { t ->
                    Transaction(
                        transactionId = t.transactionId,
                        accountId = t.accountId,
                        amount = t.amount,
                        date = t.date,
                        name = t.name,
                        merchantName = t.merchantName,
                        category = t.category,
                        pending = t.pending,
                        isoCurrencyCode = t.isoCurrencyCode,
                    )
                }
            )
            emit(Response.Success(detail))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to load dashboard"))
        }
    }

    override suspend fun createDashboard(name: String): Response<Dashboard> {
        return try {
            val dto = service.createDashboard(name)
            val dashboard = Dashboard(
                dashboardId = dto.dashboardId,
                name = dto.name,
                ownerUserId = dto.ownerUserId,
                inviteCode = dto.inviteCode
            )
            Response.Success(dashboard)
        } catch (e: Exception) {
            Response.Error(e.message ?: "Failed to create dashboard")
        }
    }

    override suspend fun addDashboardAccount(dashboardId: String, accountId: String): Response<Unit> {
        return try {
            service.addDashboardAccount(dashboardId, accountId)
            Response.Success(Unit)
        } catch (e: Exception) {
            Response.Error(e.message ?: "Failed to add account to dashboard")
        }
    }
}
