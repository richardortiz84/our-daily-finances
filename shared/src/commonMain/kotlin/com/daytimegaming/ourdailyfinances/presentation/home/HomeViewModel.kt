package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dashboardUseCase: DashboardUseCase,
    private val accountUseCase: AccountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
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
            }.collect { screenstate ->
                _state.update { screenstate }
            }
        }
    }
}
