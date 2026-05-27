package com.daytimegaming.ourdailyfinances.presentation.dashboard

import com.daytimegaming.ourdailyfinances.domain.model.DashboardDetail

sealed class DashboardDetailScreenState {
    data object Loading : DashboardDetailScreenState()
    data class Error(val message: String) : DashboardDetailScreenState()
    data class Loaded(val detail: DashboardDetail) : DashboardDetailScreenState()
}
