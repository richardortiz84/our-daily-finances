package com.daytimegaming.ourdailyfinances.presentation.manageaccounts

import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem

sealed class ManageAccountsScreenState {
    data object Loading : ManageAccountsScreenState()
    data class Error(val message: String) : ManageAccountsScreenState()
    data class Loaded(
        val items: List<PlaidItem>,
        val unlinkingItemId: String? = null,
    ) : ManageAccountsScreenState()
}
