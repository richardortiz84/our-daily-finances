package com.daytimegaming.ourdailyfinances.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import com.daytimegaming.ourdailyfinances.domain.repository.TransactionRepository
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class AccountDetailScreenState {
    data object Loading : AccountDetailScreenState()
    data class Error(val message: String) : AccountDetailScreenState()
    data class Loaded(
        val account: Account,
        val transactions: List<Transaction>,
    ) : AccountDetailScreenState()
}

class AccountDetailViewModel(
    private val accountId: String,
    private val accountUseCase: AccountUseCase,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AccountDetailScreenState>(AccountDetailScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            transactionRepository.refresh()
        }
        viewModelScope.launch {
            combine(
                accountUseCase.GetAccounts(),
                transactionRepository.transactions,
            ) { accountsResponse, transactionsResponse ->
                when {
                    accountsResponse is Response.Error ->
                        AccountDetailScreenState.Error(accountsResponse.message)
                    transactionsResponse is Response.Error ->
                        AccountDetailScreenState.Error(transactionsResponse.message)
                    accountsResponse is Response.Success && transactionsResponse is Response.Success -> {
                        val account = accountsResponse.data.find { it.accountId == accountId }
                            ?: return@combine AccountDetailScreenState.Error("Account not found")
                        val filtered = transactionsResponse.data.filter { it.accountId == accountId }
                        AccountDetailScreenState.Loaded(account, filtered)
                    }
                    else -> AccountDetailScreenState.Loading
                }
            }.collect { _state.value = it }
        }
    }
}
