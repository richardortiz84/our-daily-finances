package com.daytimegaming.ourdailyfinances

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.navigation.AppNavigation
import com.daytimegaming.ourdailyfinances.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    darkTheme: Boolean,
    dynamicColor: Boolean
) {
    AppTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
    ) {
        val mainViewModel = koinViewModel<MainViewModel>()
        val currentUser by mainViewModel.currentUser.collectAsStateWithLifecycle()
        AppNavigation(isAuthenticated = currentUser != null)
    }
}
