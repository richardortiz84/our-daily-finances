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

class DashboardUseCase(
    val GetDashboards: GetDashboards,
    val GetDashboardDetail: GetDashboardDetail,
)
