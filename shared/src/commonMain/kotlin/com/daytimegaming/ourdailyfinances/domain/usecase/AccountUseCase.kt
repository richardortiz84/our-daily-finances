package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccounts(private val repo: AccountRepository) {
    operator fun invoke(): Flow<Response<List<Account>>> = repo.getAccounts()
}

class AccountUseCase(
    val GetAccounts: GetAccounts,
)
