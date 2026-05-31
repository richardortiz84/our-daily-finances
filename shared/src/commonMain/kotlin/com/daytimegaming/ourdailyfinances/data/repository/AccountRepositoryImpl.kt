package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.AccountService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AccountRepositoryImpl(private val service: AccountService) : AccountRepository {

    override fun getAccounts(): Flow<Response<List<Account>>> = flow {
        emit(Response.Loading)
        try {
            val response = service.getAccounts()
            val accounts = response.accounts.map { dto ->
                Account(
                    accountId = dto.accountId,
                    name = dto.name,
                    officialName = dto.officialName,
                    type = dto.type,
                    subtype = dto.subtype,
                    currentBalance = dto.currentBalance,
                    availableBalance = dto.availableBalance,
                    isoCurrencyCode = dto.isoCurrencyCode,
                )
            }
            emit(Response.Success(accounts))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to load accounts"))
        }
    }

    override suspend fun createLinkToken(): String =
        service.createLinkToken().linkToken

    override suspend fun exchangePublicToken(publicToken: String, institutionName: String?) {
        service.exchangePublicToken(publicToken, institutionName)
    }

    override fun getPlaidItems(): Flow<Response<List<PlaidItem>>> = flow {
        emit(Response.Loading)
        try {
            val items = service.getPlaidItems().items.map { dto ->
                PlaidItem(
                    itemId = dto.itemId,
                    institutionName = dto.institutionName,
                    createdAt = dto.createdAt,
                )
            }
            emit(Response.Success(items))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to load linked accounts"))
        }
    }

    override suspend fun unlinkItem(itemId: String) {
        service.unlinkItem(itemId)
    }
}
