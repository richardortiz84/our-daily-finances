package com.daytimegaming.ourdailyfinances.di

import org.koin.core.context.startKoin

fun initKoin(enableHttpLogging: Boolean = false) {
    startKoin {
        modules(
            networkModule(enableHttpLogging),
            repositoryModule(),
            useCaseModule(),
            viewModelModule(),
        )
    }
}
