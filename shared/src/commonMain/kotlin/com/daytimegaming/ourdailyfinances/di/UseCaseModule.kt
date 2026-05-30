package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.AuthUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.CreateLinkToken
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.GetAccounts
import com.daytimegaming.ourdailyfinances.domain.usecase.GetCurrentUser
import com.daytimegaming.ourdailyfinances.domain.usecase.GetDashboardDetail
import com.daytimegaming.ourdailyfinances.domain.usecase.GetDashboards
import com.daytimegaming.ourdailyfinances.domain.usecase.GetIdToken
import com.daytimegaming.ourdailyfinances.domain.usecase.LoginUser
import com.daytimegaming.ourdailyfinances.domain.usecase.RegisterUser
import com.daytimegaming.ourdailyfinances.domain.usecase.SignOutUser
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun useCaseModule() = module {
    singleOf(::GetCurrentUser)
    singleOf(::LoginUser)
    singleOf(::RegisterUser)
    singleOf(::SignOutUser)
    singleOf(::GetIdToken)
    singleOf(::AuthUseCase)

    singleOf(::GetDashboards)
    singleOf(::GetDashboardDetail)
    singleOf(::DashboardUseCase)

    singleOf(::GetAccounts)
    singleOf(::CreateLinkToken)
    singleOf(::AccountUseCase)
}
