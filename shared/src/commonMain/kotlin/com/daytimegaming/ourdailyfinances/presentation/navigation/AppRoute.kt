package com.daytimegaming.ourdailyfinances.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class AppRoute : NavKey {
    @Serializable data object Login : AppRoute()
    @Serializable data object Register : AppRoute()
    @Serializable data object Home : AppRoute()
    @Serializable data class DashboardDetail(val dashboardId: String) : AppRoute()
}
