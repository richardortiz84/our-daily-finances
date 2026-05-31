package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.DashboardDetailDto
import com.daytimegaming.ourdailyfinances.data.network.dto.DashboardListResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.CreateDashboardRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.AddDashboardAccountRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.DashboardDto
import com.daytimegaming.ourdailyfinances.data.network.dto.DashboardInviteDto
import com.daytimegaming.ourdailyfinances.data.network.dto.JoinDashboardRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.JoinDashboardResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.MessageResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class DashboardService(private val apiClient: ApiClient) {
    suspend fun getDashboards(): DashboardListResponse =
        apiClient.http.get("${apiClient.baseUrl}/dashboards").body()

    suspend fun getDashboardDetail(dashboardId: String): DashboardDetailDto =
        apiClient.http.get("${apiClient.baseUrl}/dashboards/$dashboardId").body()

    suspend fun createDashboard(name: String): DashboardDto =
        apiClient.http.post("${apiClient.baseUrl}/dashboards") {
            contentType(ContentType.Application.Json)
            setBody(CreateDashboardRequest(name))
        }.body()

    suspend fun addDashboardAccount(dashboardId: String, accountId: String): MessageResponse =
        apiClient.http.post("${apiClient.baseUrl}/dashboards/$dashboardId/accounts") {
            contentType(ContentType.Application.Json)
            setBody(AddDashboardAccountRequest(accountId))
        }.body()

    suspend fun createInvite(dashboardId: String): DashboardInviteDto =
        apiClient.http.post("${apiClient.baseUrl}/dashboards/$dashboardId/invites").body()

    suspend fun revokeInvite(dashboardId: String, code: String): MessageResponse =
        apiClient.http.delete("${apiClient.baseUrl}/dashboards/$dashboardId/invites/$code").body()

    suspend fun joinDashboard(inviteCode: String): JoinDashboardResponse =
        apiClient.http.post("${apiClient.baseUrl}/dashboards/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinDashboardRequest(inviteCode))
        }.body()

    suspend fun leaveDashboard(dashboardId: String): MessageResponse =
        apiClient.http.delete("${apiClient.baseUrl}/dashboards/$dashboardId/leave").body()

    suspend fun removeDashboardAccount(dashboardId: String, accountId: String): MessageResponse =
        apiClient.http.delete("${apiClient.baseUrl}/dashboards/$dashboardId/accounts/$accountId").body()
}
