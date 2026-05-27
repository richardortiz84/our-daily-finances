package com.daytimegaming.ourdailyfinances.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.usecase.AuthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(private val authUseCase: AuthUseCase) : ViewModel() {

    private val _state = MutableStateFlow<AuthScreenState>(AuthScreenState.Idle)
    val state = _state.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { AuthScreenState.Loading }
            try {
                authUseCase.LoginUser(email, password)
                _state.update { AuthScreenState.Idle }
            } catch (e: Exception) {
                _state.update { AuthScreenState.Error(e.message ?: "Login failed") }
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.update { AuthScreenState.Loading }
            try {
                authUseCase.RegisterUser(email, password)
                _state.update { AuthScreenState.Idle }
            } catch (e: Exception) {
                _state.update { AuthScreenState.Error(e.message ?: "Registration failed") }
            }
        }
    }

    fun clearError() {
        _state.update { AuthScreenState.Idle }
    }
}
