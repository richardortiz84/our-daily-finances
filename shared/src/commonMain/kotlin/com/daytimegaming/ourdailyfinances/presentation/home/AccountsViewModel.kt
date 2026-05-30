package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AccountsScreenState {
    data object Loading : AccountsScreenState()
    data class Error(val message: String) : AccountsScreenState()
    data class Loaded(
        val accounts: List<Account>,
        val isAddingAccount: Boolean = false,
        val isLinkingAccount: Boolean = false
    ) : AccountsScreenState()
}

class AccountsViewModel(
    private val accountUseCase: AccountUseCase,
    private val plaidEventBus: PlaidEventBus
) : ViewModel() {

    private val _state = MutableStateFlow<AccountsScreenState>(AccountsScreenState.Loading)
    val state = _state.asStateFlow()

    private val _plaidLinkTokenEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val plaidLinkTokenEvent: SharedFlow<String> = _plaidLinkTokenEvent.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            plaidEventBus.events.collect { result ->
                _state.update { s ->
                    if (s is AccountsScreenState.Loaded) s.copy(isLinkingAccount = true) else s
                }
                try {
                    accountUseCase.ExchangePublicToken(result.publicToken, result.institutionName)
                    load()
                } catch (e: Exception) {
                    _state.update { AccountsScreenState.Error(e.message ?: "Failed to link account") }
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { AccountsScreenState.Loading }
            accountUseCase.GetAccounts().collect { response ->
                when (response) {
                    is Response.Error -> _state.update { AccountsScreenState.Error(response.message) }
                    is Response.Success -> _state.update {
                        AccountsScreenState.Loaded(accounts = response.data)
                    }
                    Response.Loading -> _state.update { AccountsScreenState.Loading }
                }
            }
        }
    }

    fun requestAddAccount() {
        viewModelScope.launch {
            _state.update { s -> if (s is AccountsScreenState.Loaded) s.copy(isAddingAccount = true) else s }
            try {
                val token = accountUseCase.CreateLinkToken()
                _state.update { s -> if (s is AccountsScreenState.Loaded) s.copy(isAddingAccount = false) else s }
                _plaidLinkTokenEvent.emit(token)
            } catch (e: Exception) {
                _state.update { AccountsScreenState.Error(e.message ?: "Failed to start account linking") }
            }
        }
    }
}
