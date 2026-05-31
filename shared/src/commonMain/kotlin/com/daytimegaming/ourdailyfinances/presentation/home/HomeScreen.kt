package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlin.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private enum class HomeTab {
    Dashboards,
    Accounts,
    Transactions,
    Settings
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    onDashboardClick: (String) -> Unit,
    onAccountClick: (String) -> Unit,
    onPlaidTokenReady: (String) -> Unit,
    onManageAccounts: () -> Unit,
    onCreateDashboard: () -> Unit,
) {
    // Backstack implementation using enum names to ensure safe bundle serialization
    var tabBackStackNames by rememberSaveable { mutableStateOf(listOf(HomeTab.Dashboards.name)) }
    val tabBackStack = tabBackStackNames.map { HomeTab.valueOf(it) }
    val selectedTab = tabBackStack.lastOrNull() ?: HomeTab.Dashboards

    val selectTab: (HomeTab) -> Unit = { tab ->
        if (tabBackStackNames.lastOrNull() != tab.name) {
            tabBackStackNames = tabBackStackNames + tab.name
        }
    }

    val navState = rememberNavigationEventState(NavigationEventInfo.None)

    // Intercept back presses to navigate to the previously selected navigation item
    NavigationBackHandler(
        state = navState,
        isBackEnabled = tabBackStackNames.size > 1,
        onBackCompleted = {
            if (tabBackStackNames.size > 1) {
                tabBackStackNames = tabBackStackNames.dropLast(1)
            }
        }
    )

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isPhonePortrait = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    if (isPhonePortrait) {
        Scaffold(
            bottomBar = {
                GlassBottomBar(
                    selectedTab = selectedTab,
                    onTabSelect = selectTab
                )
            }
        ) { innerPadding ->
            HomeContent(
                selectedTab = selectedTab,
                onDashboardClick = onDashboardClick,
                onAccountClick = onAccountClick,
                onPlaidTokenReady = onPlaidTokenReady,
                onManageAccounts = onManageAccounts,
                onCreateDashboard = onCreateDashboard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    } else {
        Scaffold { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                GlassNavigationRail(
                    selectedTab = selectedTab,
                    onTabSelect = selectTab
                )
                HomeContent(
                    selectedTab = selectedTab,
                    onDashboardClick = onDashboardClick,
                    onAccountClick = onAccountClick,
                    onPlaidTokenReady = onPlaidTokenReady,
                    onManageAccounts = onManageAccounts,
                    onCreateDashboard = onCreateDashboard,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    selectedTab: HomeTab,
    onDashboardClick: (String) -> Unit,
    onAccountClick: (String) -> Unit,
    onPlaidTokenReady: (String) -> Unit,
    onManageAccounts: () -> Unit,
    onCreateDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (selectedTab) {
            HomeTab.Dashboards -> DashboardsScreen(
                onDashboardClick = onDashboardClick,
                onCreateDashboardClick = onCreateDashboard
            )
            HomeTab.Accounts -> AccountsScreen(
                onAccountClick = onAccountClick,
                onPlaidTokenReady = onPlaidTokenReady,
                onManageAccounts = onManageAccounts
            )
            HomeTab.Transactions -> TransactionsScreen()
            HomeTab.Settings -> SettingsScreen()
        }
    }
}

@Composable
private fun GlassBottomBar(
    selectedTab: HomeTab,
    onTabSelect: (HomeTab) -> Unit
) {
    val tabs = listOf(
        HomeTab.Dashboards to Icons.Default.Dashboard,
        HomeTab.Accounts to Icons.Default.AccountBalance,
        HomeTab.Transactions to Icons.AutoMirrored.Filled.ReceiptLong,
        HomeTab.Settings to Icons.Default.Settings
    )

    NavigationBar(
        containerColor = Color(0x221D2022), // semi-transparent surface-container
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(width = 1.dp, color = Color(0x1AFFFFFF), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        tabs.forEach { (tab, icon) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelect(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.name,
                        tint = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = tab.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0x1F57F1DB) // Low opacity teal glow selection indicator
                )
            )
        }
    }
}

@Composable
private fun GlassNavigationRail(
    selectedTab: HomeTab,
    onTabSelect: (HomeTab) -> Unit
) {
    val tabs = listOf(
        HomeTab.Dashboards to Icons.Default.Dashboard,
        HomeTab.Accounts to Icons.Default.AccountBalance,
        HomeTab.Transactions to Icons.AutoMirrored.Filled.ReceiptLong,
        HomeTab.Settings to Icons.Default.Settings
    )

    NavigationRail(
        containerColor = Color(0x221D2022), // semi-transparent surface-container
        modifier = Modifier
            .fillMaxHeight()
            .border(width = 1.dp, color = Color(0x1AFFFFFF), shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
    ) {
        tabs.forEach { (tab, icon) ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelect(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.name,
                        tint = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = tab.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color(0x1F57F1DB) // Low opacity teal glow selection indicator
                )
            )
        }
    }
}

