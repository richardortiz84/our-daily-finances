package com.daytimegaming.ourdailyfinances.presentation.auth

sealed class AuthScreenState {
    data object Idle : AuthScreenState()
    data object Loading : AuthScreenState()
    data class Error(val message: String) : AuthScreenState()
}
