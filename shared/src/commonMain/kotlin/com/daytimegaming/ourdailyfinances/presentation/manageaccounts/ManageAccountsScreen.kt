package com.daytimegaming.ourdailyfinances.presentation.manageaccounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem
import com.daytimegaming.ourdailyfinances.presentation.theme.GlassCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageAccountsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(state) {
        val loaded = state as? ManageAccountsScreenState.Loaded
        if (loaded != null && loaded.items.isEmpty()) {
            onNavigateBack()
        }
    }

    val loaded = state as? ManageAccountsScreenState.Loaded
    if (loaded?.unlinkingItemId != null) {
        val item = loaded.items.find { it.itemId == loaded.unlinkingItemId }
        if (item != null) {
            UnlinkConfirmationDialog(
                item = item,
                onDismiss = { viewModel.dismissUnlink() },
                onConfirm = { viewModel.executeUnlink(item.itemId) },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Accounts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val s = state) {
            is ManageAccountsScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is ManageAccountsScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = s.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ManageAccountsScreenState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    items(s.items, key = { it.itemId }) { item ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.institutionName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedButton(
                                    onClick = { viewModel.confirmUnlink(item.itemId) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text("Unlink")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlinkConfirmationDialog(
    item: PlaidItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlink ${item.institutionName}?") },
        text = { Text("This will remove all accounts and transactions linked through this connection.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Unlink")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
