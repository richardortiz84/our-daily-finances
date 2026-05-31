package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail
import com.daytimegaming.ourdailyfinances.domain.model.DashboardInvite
import com.daytimegaming.ourdailyfinances.domain.model.JoinedDashboard
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboards(): Flow<Response<List<Dashboard>>>
    fun getDashboardDetail(dashboardId: String): Flow<Response<DashboardDetail>>
    suspend fun createDashboard(name: String): Response<Dashboard>
    suspend fun addDashboardAccount(dashboardId: String, accountId: String): Response<Unit>
    suspend fun createInvite(dashboardId: String): Response<DashboardInvite>
    suspend fun revokeInvite(dashboardId: String, code: String): Response<Unit>
    suspend fun joinDashboard(inviteCode: String): Response<JoinedDashboard>
    suspend fun leaveDashboard(dashboardId: String): Response<Unit>
    suspend fun removeDashboardAccount(dashboardId: String, accountId: String): Response<Unit>
}
