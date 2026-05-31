package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidItemsEventBus
import com.daytimegaming.ourdailyfinances.domain.repository.TransactionRepository
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class TransactionsScreenState {
    data object Loading : TransactionsScreenState()
    data class Error(val message: String) : TransactionsScreenState()
    data class Loaded(val transactions: List<Transaction>) : TransactionsScreenState()
}

class TransactionsViewModel(
    private val accountUseCase: AccountUseCase,
    private val transactionRepository: TransactionRepository,
    private val plaidItemsEventBus: PlaidItemsEventBus
) : ViewModel() {

    private val _state = MutableStateFlow<TransactionsScreenState>(TransactionsScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            plaidItemsEventBus.events.collect {
                load()
            }
        }
    }

    fun load() {
        _state.value = TransactionsScreenState.Loading
        viewModelScope.launch {
            transactionRepository.refresh()
        }
        viewModelScope.launch {
            combine(
                accountUseCase.GetAccounts(),
                transactionRepository.transactions
            ) { accountsResponse, transactionsResponse ->
                when {
                    accountsResponse is Response.Error ->
                        TransactionsScreenState.Error(accountsResponse.message)
                    transactionsResponse is Response.Error ->
                        TransactionsScreenState.Error(transactionsResponse.message)
                    accountsResponse is Response.Success && transactionsResponse is Response.Success -> {
                        if (accountsResponse.data.isEmpty()) {
                            TransactionsScreenState.Loaded(emptyList())
                        } else {
                            TransactionsScreenState.Loaded(transactionsResponse.data)
                        }
                    }
                    else -> TransactionsScreenState.Loading
                }
            }.collect { _state.value = it }
        }
    }
}
