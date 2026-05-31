package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.AccountService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import com.daytimegaming.ourdailyfinances.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransactionRepositoryImpl(private val service: AccountService) : TransactionRepository {

    private val _transactions = MutableStateFlow<Response<List<Transaction>>>(Response.Loading)
    override val transactions: StateFlow<Response<List<Transaction>>> = _transactions.asStateFlow()

    override suspend fun refresh() {
        _transactions.value = Response.Loading
        try {
            val list = service.getTransactions().transactions.map { dto ->
                Transaction(
                    transactionId = dto.transactionId,
                    accountId = dto.accountId,
                    amount = dto.amount,
                    date = dto.date,
                    name = dto.name,
                    merchantName = dto.merchantName,
                    category = dto.category,
                    pending = dto.pending,
                    isoCurrencyCode = dto.isoCurrencyCode,
                )
            }
            _transactions.value = Response.Success(list)
        } catch (e: Exception) {
            _transactions.value = Response.Error(e.message ?: "Failed to load transactions")
        }
    }
}
