package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class DashboardsScreenState {
    data object Loading : DashboardsScreenState()
    data class Error(val message: String) : DashboardsScreenState()
    data class Loaded(
        val dashboards: List<Dashboard>,
        val totalBalance: Double
    ) : DashboardsScreenState()
}

class DashboardsViewModel(
    private val dashboardUseCase: DashboardUseCase,
    private val accountUseCase: AccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardsScreenState>(DashboardsScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { DashboardsScreenState.Loading }
            combine(
                dashboardUseCase.GetDashboards(),
                accountUseCase.GetAccounts()
            ) { dashboardsResponse, accountsResponse ->
                when {
                    dashboardsResponse is Response.Error ->
                        DashboardsScreenState.Error(dashboardsResponse.message)
                    accountsResponse is Response.Error ->
                        DashboardsScreenState.Error(accountsResponse.message)
                    dashboardsResponse is Response.Success && accountsResponse is Response.Success -> {
                        val total = accountsResponse.data.sumOf { it.currentBalance ?: 0.0 }
                        DashboardsScreenState.Loaded(
                            dashboards = dashboardsResponse.data,
                            totalBalance = total
                        )
                    }
                    else -> DashboardsScreenState.Loading
                }
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }
}
