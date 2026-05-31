package com.daytimegaming.ourdailyfinances.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.daytimegaming.ourdailyfinances.presentation.account.AccountDetailScreen
import com.daytimegaming.ourdailyfinances.presentation.auth.LoginScreen
import com.daytimegaming.ourdailyfinances.presentation.auth.RegisterScreen
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailScreen
import com.daytimegaming.ourdailyfinances.presentation.dashboard.CreateDashboardScreen
import com.daytimegaming.ourdailyfinances.presentation.home.HomeScreen
import com.daytimegaming.ourdailyfinances.presentation.manageaccounts.ManageAccountsScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val navSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Login::class, AppRoute.Login.serializer())
            subclass(AppRoute.Register::class, AppRoute.Register.serializer())
            subclass(AppRoute.Home::class, AppRoute.Home.serializer())
            subclass(AppRoute.DashboardDetail::class, AppRoute.DashboardDetail.serializer())
            subclass(AppRoute.AccountDetail::class, AppRoute.AccountDetail.serializer())
            subclass(AppRoute.ManageAccounts::class, AppRoute.ManageAccounts.serializer())
            subclass(AppRoute.CreateDashboard::class, AppRoute.CreateDashboard.serializer())
        }
    }
}

@Composable
fun AppNavigation(
    isAuthenticated: Boolean,
    onPlaidTokenReady: (String) -> Unit,
) {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(navSavedStateConfig, AppRoute.Login)

    LaunchedEffect(isAuthenticated) {
        val top = backStack.lastOrNull()
        if (isAuthenticated && (top is AppRoute.Login || top is AppRoute.Register)) {
            backStack.clear()
            backStack.add(AppRoute.Home)
        } else if (!isAuthenticated && top != null && top !is AppRoute.Login && top !is AppRoute.Register) {
            backStack.clear()
            backStack.add(AppRoute.Login)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AppRoute.Login> {
                LoginScreen(
                    onNavigateToRegister = { backStack.add(AppRoute.Register) },
                )
            }
            entry<AppRoute.Register> {
                RegisterScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.Home> {
                HomeScreen(
                    onDashboardClick = { dashboardId ->
                        backStack.add(AppRoute.DashboardDetail(dashboardId))
                    },
                    onAccountClick = { accountId ->
                        backStack.add(AppRoute.AccountDetail(accountId))
                    },
                    onPlaidTokenReady = onPlaidTokenReady,
                    onManageAccounts = {
                        backStack.add(AppRoute.ManageAccounts)
                    },
                    onCreateDashboard = {
                        backStack.add(AppRoute.CreateDashboard)
                    }
                )
            }
            entry<AppRoute.DashboardDetail> { route ->
                DashboardDetailScreen(
                    dashboardId = route.dashboardId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.AccountDetail> { route ->
                AccountDetailScreen(
                    accountId = route.accountId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.ManageAccounts> {
                ManageAccountsScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.CreateDashboard> {
                CreateDashboardScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onDashboardCreated = { dashboardId ->
                        backStack.removeLastOrNull()
                        backStack.add(AppRoute.DashboardDetail(dashboardId))
                    }
                )
            }
        },
    )
}
