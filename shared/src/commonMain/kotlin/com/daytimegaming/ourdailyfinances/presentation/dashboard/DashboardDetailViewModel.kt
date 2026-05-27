package com.daytimegaming.ourdailyfinances.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardDetailViewModel(
    private val dashboardId: String,
    private val dashboardUseCase: DashboardUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardDetailScreenState>(DashboardDetailScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { DashboardDetailScreenState.Loading }
            dashboardUseCase.GetDashboardDetail(dashboardId).collect { response ->
                _state.update {
                    when (response) {
                        is Response.Loading -> DashboardDetailScreenState.Loading
                        is Response.Success -> DashboardDetailScreenState.Loaded(response.data)
                        is Response.Error -> DashboardDetailScreenState.Error(response.message)
                    }
                }
            }
        }
    }
}
