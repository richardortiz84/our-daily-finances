package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail
import com.daytimegaming.ourdailyfinances.domain.model.DashboardInvite
import com.daytimegaming.ourdailyfinances.domain.model.JoinedDashboard
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

class CreateInvite(private val repo: DashboardRepository) {
    suspend operator fun invoke(dashboardId: String): Response<DashboardInvite> =
        repo.createInvite(dashboardId)
}

class RevokeInvite(private val repo: DashboardRepository) {
    suspend operator fun invoke(dashboardId: String, code: String): Response<Unit> =
        repo.revokeInvite(dashboardId, code)
}

class JoinDashboard(private val repo: DashboardRepository) {
    suspend operator fun invoke(inviteCode: String): Response<JoinedDashboard> =
        repo.joinDashboard(inviteCode)
}

class LeaveDashboard(private val repo: DashboardRepository) {
    suspend operator fun invoke(dashboardId: String): Response<Unit> =
        repo.leaveDashboard(dashboardId)
}

class RemoveDashboardAccount(private val repo: DashboardRepository) {
    suspend operator fun invoke(dashboardId: String, accountId: String): Response<Unit> =
        repo.removeDashboardAccount(dashboardId, accountId)
}

class DashboardUseCase(
    val GetDashboards: GetDashboards,
    val GetDashboardDetail: GetDashboardDetail,
    val CreateDashboard: CreateDashboard,
    val AddDashboardAccount: AddDashboardAccount,
    val CreateInvite: CreateInvite,
    val RevokeInvite: RevokeInvite,
    val JoinDashboard: JoinDashboard,
    val LeaveDashboard: LeaveDashboard,
    val RemoveDashboardAccount: RemoveDashboardAccount,
)
