package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.AccountService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
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
}
