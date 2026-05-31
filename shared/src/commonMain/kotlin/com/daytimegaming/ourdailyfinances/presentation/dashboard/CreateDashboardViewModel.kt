package com.daytimegaming.ourdailyfinances.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateDashboardState(
    val name: String = "",
    val availableAccounts: List<Account> = emptyList(),
    val selectedAccounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdDashboardId: String? = null
)

class CreateDashboardViewModel(
    private val dashboardUseCase: DashboardUseCase,
    private val accountUseCase: AccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateDashboardState())
    val state = _state.asStateFlow()

    init {
        loadAvailableAccounts()
    }

    private fun loadAvailableAccounts() {
        viewModelScope.launch {
            accountUseCase.GetAccounts().collect { response ->
                when (response) {
                    is Response.Success -> {
                        _state.update { it.copy(availableAccounts = response.data) }
                    }
                    is Response.Error -> {
                        _state.update { it.copy(error = response.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _state.update { it.copy(name = newName) }
    }

    fun selectAccount(account: Account) {
        _state.update { state ->
            if (state.selectedAccounts.any { it.accountId == account.accountId }) {
                state
            } else {
                state.copy(selectedAccounts = state.selectedAccounts + account)
            }
        }
    }

    fun removeAccount(account: Account) {
        _state.update { state ->
            state.copy(selectedAccounts = state.selectedAccounts.filter { it.accountId != account.accountId })
        }
    }

    fun createDashboard(onSuccess: (String) -> Unit) {
        val currentState = _state.value
        if (currentState.name.isBlank()) {
            _state.update { it.copy(error = "Dashboard name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val createResponse = dashboardUseCase.CreateDashboard(currentState.name)
            when (createResponse) {
                is Response.Success -> {
                    val dashboardId = createResponse.data.dashboardId
                    var addAccountsSuccess = true
                    var lastError: String? = null

                    // Add each selected account
                    for (account in currentState.selectedAccounts) {
                        val addResponse = dashboardUseCase.AddDashboardAccount(dashboardId, account.accountId)
                        if (addResponse is Response.Error) {
                            addAccountsSuccess = false
                            lastError = addResponse.message
                            break
                        }
                    }

                    if (addAccountsSuccess) {
                        _state.update { it.copy(isLoading = false, createdDashboardId = dashboardId) }
                        onSuccess(dashboardId)
                    } else {
                        _state.update { it.copy(isLoading = false, error = lastError ?: "Failed to link some accounts") }
                    }
                }
                is Response.Error -> {
                    _state.update { it.copy(isLoading = false, error = createResponse.message) }
                }
                else -> {
                    _state.update { it.copy(isLoading = false, error = "Unknown error occurred") }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
