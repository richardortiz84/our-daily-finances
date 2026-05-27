package com.daytimegaming.ourdailyfinances

import androidx.compose.ui.window.ComposeUIViewController
import com.daytimegaming.ourdailyfinances.di.initKoin
import org.koin.core.context.GlobalContext

fun MainViewController() = run {
    if (GlobalContext.getOrNull() == null) {
        initKoin()
    }
    ComposeUIViewController { App() }
}
