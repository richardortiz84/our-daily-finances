package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem
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

class GetPlaidItems(private val repo: AccountRepository) {
    operator fun invoke(): Flow<Response<List<PlaidItem>>> = repo.getPlaidItems()
}

class UnlinkPlaidItem(private val repo: AccountRepository) {
    suspend operator fun invoke(itemId: String) = repo.unlinkItem(itemId)
}

class AccountUseCase(
    val GetAccounts: GetAccounts,
    val CreateLinkToken: CreateLinkToken,
    val ExchangePublicToken: ExchangePublicToken,
    val GetPlaidItems: GetPlaidItems,
    val UnlinkPlaidItem: UnlinkPlaidItem,
)
