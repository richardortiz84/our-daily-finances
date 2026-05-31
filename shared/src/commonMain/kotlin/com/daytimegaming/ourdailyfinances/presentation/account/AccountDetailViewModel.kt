package com.daytimegaming.ourdailyfinances.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.presentation.home.MockTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AccountDetailScreenState {
    data object Loading : AccountDetailScreenState()
    data class Error(val message: String) : AccountDetailScreenState()
    data class Loaded(
        val account: Account,
        val transactions: List<MockTransaction>
    ) : AccountDetailScreenState()
}

class AccountDetailViewModel(
    private val accountId: String,
    private val accountUseCase: AccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<AccountDetailScreenState>(AccountDetailScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { AccountDetailScreenState.Loading }
            accountUseCase.GetAccounts().collect { response ->
                when (response) {
                    is Response.Error -> _state.update { AccountDetailScreenState.Error(response.message) }
                    Response.Loading -> _state.update { AccountDetailScreenState.Loading }
                    is Response.Success -> {
                        val account = response.data.find { it.accountId == accountId }
                        if (account != null) {
                            val mockTx = getMockTransactionsForAccount(account)
                            _state.update { AccountDetailScreenState.Loaded(account, mockTx) }
                        } else {
                            _state.update { AccountDetailScreenState.Error("Account not found") }
                        }
                    }
                }
            }
        }
    }

    private fun getMockTransactionsForAccount(account: Account): List<MockTransaction> {
        val baseTransactions = listOf(
            MockTransaction("Netflix Subscription", "May 28, 2026", "Entertainment", -15.49),
            MockTransaction("Salary Deposit", "May 25, 2026", "Income", 4250.00),
            MockTransaction("Whole Foods Market", "May 24, 2026", "Groceries", -124.60),
            MockTransaction("Chevron Gasoline", "May 23, 2026", "Transportation", -45.00),
            MockTransaction("Starbucks Coffee", "May 22, 2026", "Dining", -6.75),
            MockTransaction("Transfer to Shared Savings", "May 20, 2026", "Transfer", -500.00)
        )
        return when (account.subtype?.lowercase()) {
            "credit card", "credit" -> baseTransactions.filter { it.amount < 0 && it.category != "Transfer" }
            "checking", "depository" -> baseTransactions
            "savings" -> baseTransactions.filter { it.category == "Transfer" || it.category == "Income" }
            else -> baseTransactions
        }
    }
}
