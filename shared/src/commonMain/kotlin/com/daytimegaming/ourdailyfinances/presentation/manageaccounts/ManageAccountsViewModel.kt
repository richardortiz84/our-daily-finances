package com.daytimegaming.ourdailyfinances.presentation.manageaccounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidItemsEventBus
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageAccountsViewModel(
    private val accountUseCase: AccountUseCase,
    private val plaidItemsEventBus: PlaidItemsEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<ManageAccountsScreenState>(ManageAccountsScreenState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { ManageAccountsScreenState.Loading }
            accountUseCase.GetPlaidItems().collect { response ->
                _state.update {
                    when (response) {
                        is Response.Loading -> ManageAccountsScreenState.Loading
                        is Response.Success -> ManageAccountsScreenState.Loaded(response.data)
                        is Response.Error -> ManageAccountsScreenState.Error(response.message)
                    }
                }
            }
        }
    }

    fun confirmUnlink(itemId: String) {
        _state.update { s ->
            if (s is ManageAccountsScreenState.Loaded) s.copy(unlinkingItemId = itemId) else s
        }
    }

    fun dismissUnlink() {
        _state.update { s ->
            if (s is ManageAccountsScreenState.Loaded) s.copy(unlinkingItemId = null) else s
        }
    }

    fun executeUnlink(itemId: String) {
        viewModelScope.launch {
            try {
                accountUseCase.UnlinkPlaidItem(itemId)
                plaidItemsEventBus.itemsChanged()
                load()
            } catch (e: Exception) {
                _state.update { ManageAccountsScreenState.Error(e.message ?: "Failed to unlink account") }
            }
        }
    }
}
