package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.account.AccountDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.AccountsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.DashboardsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.SettingsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.TransactionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual fun viewModelModule(): Module = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::DashboardsViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::TransactionsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
    viewModel { params -> AccountDetailViewModel(params.get(), get(), get()) }
}
