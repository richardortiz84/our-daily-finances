package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.AccountListResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkTokenResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.PlaidItemsResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
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

    suspend fun getPlaidItems(): PlaidItemsResponse =
        apiClient.http.get("${apiClient.baseUrl}/plaid/items").body()

    suspend fun unlinkItem(itemId: String) {
        apiClient.http.delete("${apiClient.baseUrl}/plaid/items/$itemId")
    }
}
