package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.data.settings.JvmSettingsManager
import com.daytimegaming.ourdailyfinances.data.settings.SettingsManager
import org.koin.dsl.module

actual fun platformModule() = module {
    single<SettingsManager> { JvmSettingsManager() }
}
