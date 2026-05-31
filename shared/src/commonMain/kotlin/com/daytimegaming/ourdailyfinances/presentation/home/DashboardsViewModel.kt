package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidItemsEventBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class DashboardsScreenState {
    data object Loading : DashboardsScreenState()
    data class Error(val message: String) : DashboardsScreenState()
    data class Loaded(
        val dashboards: List<DashboardDetail>,
        val totalBalance: Double,
        val hasLinkedAccounts: Boolean
    ) : DashboardsScreenState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardsViewModel(
    private val dashboardUseCase: DashboardUseCase,
    private val accountUseCase: AccountUseCase,
    private val plaidEventBus: PlaidEventBus,
    private val plaidItemsEventBus: PlaidItemsEventBus
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardsScreenState>(DashboardsScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            plaidEventBus.events.collect { load() }
        }
        viewModelScope.launch {
            plaidItemsEventBus.events.collect { load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { DashboardsScreenState.Loading }
            
            val dashboardsDetailsFlow = dashboardUseCase.GetDashboards().flatMapLatest { dashboardsResponse ->
                when (dashboardsResponse) {
                    is Response.Error -> flowOf(Response.Error(dashboardsResponse.message))
                    Response.Loading -> flowOf(Response.Loading)
                    is Response.Success -> {
                        val dashboards = dashboardsResponse.data
                        if (dashboards.isEmpty()) {
                            flowOf(Response.Success(emptyList<DashboardDetail>()))
                        } else {
                            val detailFlows = dashboards.map { dashboard ->
                                dashboardUseCase.GetDashboardDetail(dashboard.dashboardId)
                            }
                            combine(detailFlows) { detailsResponses ->
                                val error = detailsResponses.filterIsInstance<Response.Error>().firstOrNull()
                                if (error != null) {
                                    Response.Error(error.message)
                                } else if (detailsResponses.any { it is Response.Loading }) {
                                    Response.Loading
                                } else {
                                    val details = detailsResponses.filterIsInstance<Response.Success<DashboardDetail>>().map { it.data }
                                    Response.Success(details)
                                }
                            }
                        }
                    }
                }
            }

            combine(
                dashboardsDetailsFlow,
                accountUseCase.GetAccounts()
            ) { detailsResponse, accountsResponse ->
                when {
                    detailsResponse is Response.Error ->
                        DashboardsScreenState.Error(detailsResponse.message)
                    accountsResponse is Response.Error ->
                        DashboardsScreenState.Error(accountsResponse.message)
                    detailsResponse is Response.Success && accountsResponse is Response.Success -> {
                        val total = accountsResponse.data.sumOf { it.currentBalance ?: 0.0 }
                        DashboardsScreenState.Loaded(
                            dashboards = detailsResponse.data,
                            totalBalance = total,
                            hasLinkedAccounts = accountsResponse.data.isNotEmpty()
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
