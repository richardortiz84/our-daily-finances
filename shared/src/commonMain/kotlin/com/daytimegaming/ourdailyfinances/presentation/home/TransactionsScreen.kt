package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import com.daytimegaming.ourdailyfinances.presentation.theme.GlassCard
import com.daytimegaming.ourdailyfinances.presentation.util.formatAmount
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            text = "Transactions History",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        when (val s = state) {
            is TransactionsScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is TransactionsScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is TransactionsScreenState.Loaded -> {
                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x11FFFFFF),
                        unfocusedContainerColor = Color(0x11FFFFFF),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Category selection scrollable chips
                val categories = remember(s.transactions) {
                    listOf("All") + s.transactions
                        .flatMap { it.category }
                        .distinct()
                        .take(5)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = Color(0x11FFFFFF),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0x1AFFFFFF),
                                selectedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(999.dp)
                        )
                    }
                }

                val filteredTransactions = remember(s.transactions, searchQuery, selectedCategory) {
                    s.transactions.filter { tx ->
                        val matchesSearch = searchQuery.isEmpty() ||
                            tx.name.contains(searchQuery, ignoreCase = true) ||
                            (tx.merchantName?.contains(searchQuery, ignoreCase = true) ?: false)
                        val matchesCategory = selectedCategory == "All" ||
                            tx.category.contains(selectedCategory)
                        matchesSearch && matchesCategory
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "No transactions found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        items(filteredTransactions, key = { it.transactionId }) { tx ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Custom action if needed */ }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val categoryName = tx.category.firstOrNull() ?: "General"
                                        val isExpense = tx.amount > 0
                                        val icon = when {
                                            categoryName.contains("Groceries", ignoreCase = true) || categoryName.contains("Food", ignoreCase = true) -> Icons.Default.Group
                                            categoryName.contains("Utilities", ignoreCase = true) || categoryName.contains("Electric", ignoreCase = true) -> Icons.Default.Bolt
                                            categoryName.contains("Income", ignoreCase = true) || categoryName.contains("Transfer", ignoreCase = true) -> Icons.Default.AccountBalance
                                            else -> Icons.Default.Bolt
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isExpense) Color(0x11FFFFFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isExpense) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = tx.merchantName ?: tx.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = categoryName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isExpense) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (isExpense) Color(0x11FFFFFF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                                if (tx.pending) {
                                                    Text(
                                                        text = "Pending",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    val isExpense = tx.amount > 0
                                    val formattedAmount = if (isExpense) {
                                        "-$${tx.amount.formatAmount()}"
                                    } else {
                                        "+$${kotlin.math.abs(tx.amount).formatAmount()}"
                                    }
                                    val color = if (isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = formattedAmount,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tx.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
