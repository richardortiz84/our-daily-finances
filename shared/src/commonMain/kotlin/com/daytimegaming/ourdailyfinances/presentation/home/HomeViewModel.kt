package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dashboardUseCase: DashboardUseCase,
    private val accountUseCase: AccountUseCase,
    private val plaidEventBus: PlaidEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val state = _state.asStateFlow()

    private val _plaidLinkTokenEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val plaidLinkTokenEvent: SharedFlow<String> = _plaidLinkTokenEvent.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            plaidEventBus.events.collect { load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { HomeScreenState.Loading }
            combine(
                dashboardUseCase.GetDashboards(),
                accountUseCase.GetAccounts(),
            ) { dashboardsResponse, accountsResponse ->
                when {
                    dashboardsResponse is Response.Error ->
                        HomeScreenState.Error(dashboardsResponse.message)
                    accountsResponse is Response.Error ->
                        HomeScreenState.Error(accountsResponse.message)
                    dashboardsResponse is Response.Success && accountsResponse is Response.Success ->
                        HomeScreenState.Loaded(
                            dashboards = dashboardsResponse.data,
                            accounts = accountsResponse.data,
                        )
                    else -> HomeScreenState.Loading
                }
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }

    fun requestAddAccount() {
        viewModelScope.launch {
            val current = _state.value
            if (current is HomeScreenState.Loaded) {
                _state.update { current.copy(isAddingAccount = true) }
            }
            try {
                val token = accountUseCase.CreateLinkToken()
                val loaded = _state.value
                if (loaded is HomeScreenState.Loaded) {
                    _state.update { loaded.copy(isAddingAccount = false) }
                }
                _plaidLinkTokenEvent.emit(token)
            } catch (e: Exception) {
                _state.update { HomeScreenState.Error(e.message ?: "Failed to start account linking") }
            }
        }
    }
}
