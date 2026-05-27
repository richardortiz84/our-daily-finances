package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.DashboardDetailDto
import com.daytimegaming.ourdailyfinances.data.network.dto.DashboardListResponse
import io.ktor.client.call.body
import io.ktor.client.request.get

class DashboardService(private val apiClient: ApiClient) {
    suspend fun getDashboards(): DashboardListResponse =
        apiClient.http.get("${apiClient.baseUrl}/dashboards").body()

    suspend fun getDashboardDetail(dashboardId: String): DashboardDetailDto =
        apiClient.http.get("${apiClient.baseUrl}/dashboards/$dashboardId").body()
}
