package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.data.repository.AccountRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.AuthRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.DashboardRepositoryImpl
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import com.daytimegaming.ourdailyfinances.domain.repository.AuthRepository
import com.daytimegaming.ourdailyfinances.domain.repository.DashboardRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.koin.dsl.module

fun repositoryModule() = module {
    single { Firebase.auth }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<DashboardRepository> { DashboardRepositoryImpl(get()) }
    single<AccountRepository> { AccountRepositoryImpl(get()) }
    single { PlaidEventBus() }
}
