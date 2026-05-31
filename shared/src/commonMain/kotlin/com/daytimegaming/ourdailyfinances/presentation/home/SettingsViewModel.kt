package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.model.User
import com.daytimegaming.ourdailyfinances.domain.usecase.AuthUseCase
import com.daytimegaming.ourdailyfinances.data.settings.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authUseCase: AuthUseCase,
    private val settingsManager: SettingsManager
) : ViewModel() {

    val currentUser: StateFlow<User?> = authUseCase.GetCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _isStaging = MutableStateFlow(settingsManager.isStaging())
    val isStaging: StateFlow<Boolean> = _isStaging.asStateFlow()

    fun setStaging(enabled: Boolean) {
        settingsManager.setStaging(enabled)
        _isStaging.value = enabled
    }

    fun signOut() {
        viewModelScope.launch {
            authUseCase.SignOutUser()
        }
    }
}
