package com.daytimegaming.ourdailyfinances.di

import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(
            networkModule(),
            repositoryModule(),
            useCaseModule(),
            viewModelModule(),
        )
    }
}
