package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<Response<List<Account>>>
    suspend fun createLinkToken(): String
    suspend fun exchangePublicToken(publicToken: String, institutionName: String?)
}
