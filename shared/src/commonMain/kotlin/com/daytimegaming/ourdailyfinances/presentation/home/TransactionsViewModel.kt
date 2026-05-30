package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MockTransaction(
    val merchant: String,
    val date: String,
    val category: String,
    val amount: Double,
    val isPending: Boolean = false
)

sealed class TransactionsScreenState {
    data class Loaded(val transactions: List<MockTransaction>) : TransactionsScreenState()
}

class TransactionsViewModel : ViewModel() {
    private val _state = MutableStateFlow<TransactionsScreenState>(
        TransactionsScreenState.Loaded(
            listOf(
                MockTransaction("Netflix Subscription", "May 28, 2026", "Entertainment", -15.49),
                MockTransaction("Salary Deposit", "May 25, 2026", "Income", 4250.00),
                MockTransaction("Whole Foods Market", "May 24, 2026", "Groceries", -124.60),
                MockTransaction("Chevron Gasoline", "May 23, 2026", "Transportation", -45.00),
                MockTransaction("Starbucks Coffee", "May 22, 2026", "Dining", -6.75),
                MockTransaction("Transfer to Shared Savings", "May 20, 2026", "Transfer", -500.00)
            )
        )
    )
    val state = _state.asStateFlow()
}
