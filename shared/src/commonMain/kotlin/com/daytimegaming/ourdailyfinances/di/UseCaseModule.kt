package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.AuthUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.CreateLinkToken
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.ExchangePublicToken
import com.daytimegaming.ourdailyfinances.domain.usecase.GetAccounts
import com.daytimegaming.ourdailyfinances.domain.usecase.GetCurrentUser
import com.daytimegaming.ourdailyfinances.domain.usecase.CreateDashboard
import com.daytimegaming.ourdailyfinances.domain.usecase.AddDashboardAccount
import com.daytimegaming.ourdailyfinances.domain.usecase.CreateInvite
import com.daytimegaming.ourdailyfinances.domain.usecase.RevokeInvite
import com.daytimegaming.ourdailyfinances.domain.usecase.JoinDashboard
import com.daytimegaming.ourdailyfinances.domain.usecase.LeaveDashboard
import com.daytimegaming.ourdailyfinances.domain.usecase.RemoveDashboardAccount
import com.daytimegaming.ourdailyfinances.domain.usecase.GetDashboardDetail
import com.daytimegaming.ourdailyfinances.domain.usecase.GetDashboards
import com.daytimegaming.ourdailyfinances.domain.usecase.GetIdToken
import com.daytimegaming.ourdailyfinances.domain.usecase.GetPlaidItems
import com.daytimegaming.ourdailyfinances.domain.usecase.LoginUser
import com.daytimegaming.ourdailyfinances.domain.usecase.RegisterUser
import com.daytimegaming.ourdailyfinances.domain.usecase.SignOutUser
import com.daytimegaming.ourdailyfinances.domain.usecase.UnlinkPlaidItem
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
    singleOf(::CreateDashboard)
    singleOf(::AddDashboardAccount)
    singleOf(::CreateInvite)
    singleOf(::RevokeInvite)
    singleOf(::JoinDashboard)
    singleOf(::LeaveDashboard)
    singleOf(::RemoveDashboardAccount)
    singleOf(::DashboardUseCase)

    singleOf(::GetAccounts)
    singleOf(::CreateLinkToken)
    singleOf(::ExchangePublicToken)
    singleOf(::GetPlaidItems)
    singleOf(::UnlinkPlaidItem)
    singleOf(::AccountUseCase)
}
