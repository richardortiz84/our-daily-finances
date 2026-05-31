package com.daytimegaming.ourdailyfinances

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.ComposeUIViewController
import com.daytimegaming.ourdailyfinances.di.initKoin

fun MainViewController() = run {
    try {
        initKoin()
    } catch (e: Exception) {
        // Koin already started
    }
    ComposeUIViewController {
        App(
            darkTheme = isSystemInDarkTheme(),
            dynamicColor = false,
        )
    }
}
