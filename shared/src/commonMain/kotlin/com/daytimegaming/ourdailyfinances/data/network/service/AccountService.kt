package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.AccountListResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkTokenResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AccountService(private val apiClient: ApiClient) {
    suspend fun getAccounts(): AccountListResponse =
        apiClient.http.get("${apiClient.baseUrl}/plaid/balance").body()

    suspend fun createLinkToken(): LinkTokenResponse =
        apiClient.http.post("${apiClient.baseUrl}/plaid/link/token").body()

    suspend fun exchangePublicToken(publicToken: String, institutionName: String?): LinkExchangeResponse =
        apiClient.http.post("${apiClient.baseUrl}/plaid/link/exchange") {
            contentType(ContentType.Application.Json)
            setBody(LinkExchangeRequest(publicToken, institutionName))
        }.body()
}
