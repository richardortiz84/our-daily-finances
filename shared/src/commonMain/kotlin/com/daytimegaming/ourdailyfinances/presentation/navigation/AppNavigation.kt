package com.daytimegaming.ourdailyfinances.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.navigation3.ui.NavDisplay
import com.daytimegaming.ourdailyfinances.presentation.auth.LoginScreen
import com.daytimegaming.ourdailyfinances.presentation.auth.RegisterScreen
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailScreen
import com.daytimegaming.ourdailyfinances.presentation.home.HomeScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val navSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Login::class, AppRoute.Login.serializer())
            subclass(AppRoute.Register::class, AppRoute.Register.serializer())
            subclass(AppRoute.Home::class, AppRoute.Home.serializer())
            subclass(AppRoute.DashboardDetail::class, AppRoute.DashboardDetail.serializer())
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
                    onPlaidTokenReady = onPlaidTokenReady,
                )
            }
            entry<AppRoute.DashboardDetail> { route ->
                DashboardDetailScreen(
                    dashboardId = route.dashboardId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
