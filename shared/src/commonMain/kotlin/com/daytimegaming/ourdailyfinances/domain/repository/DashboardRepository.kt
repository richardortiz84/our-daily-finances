package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboards(): Flow<Response<List<Dashboard>>>
    fun getDashboardDetail(dashboardId: String): Flow<Response<DashboardDetail>>
    suspend fun createDashboard(name: String): Response<Dashboard>
    suspend fun addDashboardAccount(dashboardId: String, accountId: String): Response<Unit>
}
