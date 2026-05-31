package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import kotlinx.coroutines.flow.StateFlow

interface TransactionRepository {
    val transactions: StateFlow<Response<List<Transaction>>>
    suspend fun refresh()
}
