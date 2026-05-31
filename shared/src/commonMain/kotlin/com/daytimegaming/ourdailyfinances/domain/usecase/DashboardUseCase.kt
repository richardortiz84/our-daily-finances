package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail
import com.daytimegaming.ourdailyfinances.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

class GetDashboards(private val repo: DashboardRepository) {
    operator fun invoke(): Flow<Response<List<Dashboard>>> = repo.getDashboards()
}

class GetDashboardDetail(private val repo: DashboardRepository) {
    operator fun invoke(dashboardId: String): Flow<Response<DashboardDetail>> =
        repo.getDashboardDetail(dashboardId)
}

class CreateDashboard(private val repo: DashboardRepository) {
    suspend operator fun invoke(name: String): Response<Dashboard> = repo.createDashboard(name)
}

class AddDashboardAccount(private val repo: DashboardRepository) {
    suspend operator fun invoke(dashboardId: String, accountId: String): Response<Unit> =
        repo.addDashboardAccount(dashboardId, accountId)
}

class DashboardUseCase(
    val GetDashboards: GetDashboards,
    val GetDashboardDetail: GetDashboardDetail,
    val CreateDashboard: CreateDashboard,
    val AddDashboardAccount: AddDashboardAccount,
)
