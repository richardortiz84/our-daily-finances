package com.daytimegaming.ourdailyfinances.presentation.home

import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard

sealed class HomeScreenState {
    data object Loading : HomeScreenState()
    data class Error(val message: String) : HomeScreenState()
    data class Loaded(
        val dashboards: List<Dashboard>,
        val accounts: List<Account>,
        val isAddingAccount: Boolean = false,
    ) : HomeScreenState()
}
