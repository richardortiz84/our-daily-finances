package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.AccountListResponse
import io.ktor.client.call.body
import io.ktor.client.request.get

class AccountService(private val apiClient: ApiClient) {
    suspend fun getAccounts(): AccountListResponse =
        apiClient.http.get("${apiClient.baseUrl}/plaid/balance").body()
}
