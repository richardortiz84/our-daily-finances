package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.model.User
import com.daytimegaming.ourdailyfinances.domain.usecase.AuthUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authUseCase: AuthUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authUseCase.GetCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun signOut() {
        viewModelScope.launch {
            authUseCase.SignOutUser()
        }
    }
}
