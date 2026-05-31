package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

private enum class HomeTab {
    Dashboards,
    Accounts,
    Transactions,
    Settings
}

@Composable
fun HomeScreen(
    onDashboardClick: (String) -> Unit,
    onPlaidTokenReady: (String) -> Unit,
) {
    val tabHistory: SnapshotStateList<HomeTab> = rememberSaveable(
        saver = listSaver(
            save = { list -> list.map { it.ordinal } },
            restore = { list -> list.map { HomeTab.entries[it] }.toMutableStateList() },
        )
    ) { mutableListOf(HomeTab.Dashboards).toMutableStateList() }

    val selectedTab = tabHistory.last()

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled =  tabHistory.size > 1,
    ) {
        tabHistory.removeLastOrNull()
    }

    Scaffold(
        bottomBar = {
            GlassBottomBar(
                selectedTab = selectedTab,
                onTabSelect = { tab ->
                    if (tab != selectedTab) tabHistory.add(tab)
                },
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                HomeTab.Dashboards -> DashboardsScreen(
                    onDashboardClick = onDashboardClick
                )
                HomeTab.Accounts -> AccountsScreen(
                    onPlaidTokenReady = onPlaidTokenReady
                )
                HomeTab.Transactions -> TransactionsScreen()
                HomeTab.Settings -> SettingsScreen()
            }
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
        HomeTab.Transactions to Icons.Default.ReceiptLong,
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
