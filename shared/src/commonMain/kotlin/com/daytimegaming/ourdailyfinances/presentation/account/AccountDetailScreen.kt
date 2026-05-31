package com.daytimegaming.ourdailyfinances.presentation.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytimegaming.ourdailyfinances.presentation.theme.GlassCard
import com.daytimegaming.ourdailyfinances.presentation.util.formatAmount
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    viewModel: AccountDetailViewModel = koinViewModel { parametersOf(accountId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = state) {
                is AccountDetailScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AccountDetailScreenState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = s.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is AccountDetailScreenState.Loaded -> {
                    val account = s.account
                    val currencySymbol = account.isoCurrencyCode ?: "$"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Account Hero Card
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = account.subtype?.uppercase() ?: account.type.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            account.officialName?.let {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "$currencySymbol ${account.currentBalance?.formatAmount() ?: "0.00"}",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            account.availableBalance?.let { available ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Available Balance: $currencySymbol ${available.formatAmount()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Account Transactions",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (s.transactions.isEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No recent transactions found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(s.transactions) { tx ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = tx.merchant,
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = tx.date,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0x1FBEC6E0), shape = RoundedCornerShape(999.dp))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = tx.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                    if (tx.isPending) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0x33FFB4AB), shape = RoundedCornerShape(999.dp))
                                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "Pending",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            val formattedAmount = if (tx.amount < 0) {
                                                "-$currencySymbol${kotlin.math.abs(tx.amount).formatAmount()}"
                                            } else {
                                                "+$currencySymbol${tx.amount.formatAmount()}"
                                            }
                                            val color = if (tx.amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            Text(
                                                text = formattedAmount,
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = color
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
