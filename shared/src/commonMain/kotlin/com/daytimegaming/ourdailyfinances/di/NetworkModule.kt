package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.service.AccountService
import com.daytimegaming.ourdailyfinances.data.network.service.DashboardService
import org.koin.dsl.module

fun networkModule(enableHttpLogging: Boolean = false) = module {
    single { httpClientEngine() }
    single { ApiClient(get(), get(), enableHttpLogging) }
    single { AccountService(get()) }
    single { DashboardService(get()) }
}
