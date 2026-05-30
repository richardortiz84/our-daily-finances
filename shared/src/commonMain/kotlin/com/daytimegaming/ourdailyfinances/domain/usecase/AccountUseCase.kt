package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccounts(private val repo: AccountRepository) {
    operator fun invoke(): Flow<Response<List<Account>>> = repo.getAccounts()
}

class CreateLinkToken(private val repo: AccountRepository) {
    suspend operator fun invoke(): String = repo.createLinkToken()
}

class ExchangePublicToken(private val repo: AccountRepository) {
    suspend operator fun invoke(publicToken: String, institutionName: String?) =
        repo.exchangePublicToken(publicToken, institutionName)
}

class AccountUseCase(
    val GetAccounts: GetAccounts,
    val CreateLinkToken: CreateLinkToken,
    val ExchangePublicToken: ExchangePublicToken,
)
