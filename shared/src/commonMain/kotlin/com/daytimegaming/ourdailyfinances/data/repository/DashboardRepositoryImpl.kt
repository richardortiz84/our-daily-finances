package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.DashboardService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.domain.model.DashboardAccount
import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail
import com.daytimegaming.ourdailyfinances.domain.model.DashboardMember
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
            )
            emit(Response.Success(detail))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to load dashboard"))
        }
    }
}
