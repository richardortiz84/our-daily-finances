package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual fun viewModelModule(): Module = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
}
