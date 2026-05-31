# Account Detail Real Transactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace mock transaction data in `AccountDetailScreen` with real data from `GET /plaid/transactions`, using a singleton `TransactionRepository` cache shared across ViewModels.

**Architecture:** `TransactionRepositoryImpl` is a Koin singleton holding a `MutableStateFlow<Response<List<Transaction>>>` as its cache. `AccountDetailViewModel` injects it, calls `refresh()` on load, then `combine()`s the accounts flow with the transactions StateFlow and filters by `accountId`. The screen UI field mappings and amount sign convention are updated to match the real Plaid API.

**Tech Stack:** Kotlin Multiplatform, Ktor (HTTP), Koin (DI), Jetpack Compose, kotlinx.serialization, kotlinx.coroutines

---

## File Map

| File | Action |
|---|---|
| `shared/.../domain/model/Transaction.kt` | Create |
| `shared/.../data/network/dto/TransactionDto.kt` | Create |
| `shared/.../data/network/service/AccountService.kt` | Add `getTransactions()` |
| `shared/.../domain/repository/TransactionRepository.kt` | Create |
| `shared/.../data/repository/TransactionRepositoryImpl.kt` | Create |
| `shared/.../di/RepositoryModule.kt` | Register `TransactionRepository` singleton |
| `shared/.../presentation/account/AccountDetailViewModel.kt` | Replace mock logic with real combine flow |
| `shared/.../di/ViewModelModule.android.kt` | Add third `get()` to AccountDetailViewModel |
| `shared/.../di/ViewModelModule.ios.kt` | Add third `get()` to AccountDetailViewModel |
| `shared/.../di/ViewModelModule.jvm.kt` | Add third `get()` to AccountDetailViewModel |
| `shared/.../presentation/account/AccountDetailScreen.kt` | Field mappings + amount sign fix |

All `shared/...` paths are under `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/`. The ViewModelModule files are in their respective source sets (`androidMain`, `iosMain`, `jvmMain`).

---

### Task 1: `Transaction` domain model and DTOs

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/model/Transaction.kt`
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/TransactionDto.kt`

- [ ] **Step 1: Create `Transaction.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.domain.model

data class Transaction(
    val transactionId: String,
    val accountId: String,
    val amount: Double,
    val date: String,
    val name: String,
    val merchantName: String?,
    val category: List<String>,
    val pending: Boolean,
    val isoCurrencyCode: String?,
)
```

- [ ] **Step 2: Create `TransactionDto.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("date") val date: String,
    @SerialName("name") val name: String,
    @SerialName("merchant_name") val merchantName: String? = null,
    @SerialName("category") val category: List<String>,
    @SerialName("pending") val pending: Boolean,
    @SerialName("iso_currency_code") val isoCurrencyCode: String? = null,
)

@Serializable
data class TransactionsResponse(
    @SerialName("transactions") val transactions: List<TransactionDto>,
)
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/model/Transaction.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/TransactionDto.kt
git commit -m "feat: add Transaction domain model and DTOs"
```

---

### Task 2: `AccountService.getTransactions()`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt`

- [ ] **Step 1: Add `getTransactions()` and import**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.AccountListResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkTokenResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.PlaidItemsResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.TransactionsResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AccountService(private val apiClient: ApiClient) {
    suspend fun getAccounts(): AccountListResponse =
        apiClient.http.get("${apiClient.baseUrl}/plaid/balance").body()

    suspend fun createLinkToken(): LinkTokenResponse =
        apiClient.http.post("${apiClient.baseUrl}/plaid/link/token").body()

    suspend fun exchangePublicToken(publicToken: String, institutionName: String?): LinkExchangeResponse =
        apiClient.http.post("${apiClient.baseUrl}/plaid/link/exchange") {
            contentType(ContentType.Application.Json)
            setBody(LinkExchangeRequest(publicToken, institutionName))
        }.body()

    suspend fun getPlaidItems(): PlaidItemsResponse =
        apiClient.http.get("${apiClient.baseUrl}/plaid/items").body()

    suspend fun unlinkItem(itemId: String) {
        apiClient.http.delete("${apiClient.baseUrl}/plaid/items/$itemId")
    }

    suspend fun getTransactions(): TransactionsResponse =
        apiClient.http.get("${apiClient.baseUrl}/plaid/transactions").body()
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt
git commit -m "feat: add getTransactions() to AccountService"
```

---

### Task 3: `TransactionRepository` interface and impl

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/TransactionRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/TransactionRepositoryImpl.kt`

- [ ] **Step 1: Create `TransactionRepository.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import kotlinx.coroutines.flow.StateFlow

interface TransactionRepository {
    val transactions: StateFlow<Response<List<Transaction>>>
    suspend fun refresh()
}
```

- [ ] **Step 2: Create `TransactionRepositoryImpl.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.AccountService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import com.daytimegaming.ourdailyfinances.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransactionRepositoryImpl(private val service: AccountService) : TransactionRepository {

    private val _transactions = MutableStateFlow<Response<List<Transaction>>>(Response.Loading)
    override val transactions: StateFlow<Response<List<Transaction>>> = _transactions.asStateFlow()

    override suspend fun refresh() {
        _transactions.value = Response.Loading
        try {
            val list = service.getTransactions().transactions.map { dto ->
                Transaction(
                    transactionId = dto.transactionId,
                    accountId = dto.accountId,
                    amount = dto.amount,
                    date = dto.date,
                    name = dto.name,
                    merchantName = dto.merchantName,
                    category = dto.category,
                    pending = dto.pending,
                    isoCurrencyCode = dto.isoCurrencyCode,
                )
            }
            _transactions.value = Response.Success(list)
        } catch (e: Exception) {
            _transactions.value = Response.Error(e.message ?: "Failed to load transactions")
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/TransactionRepository.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/TransactionRepositoryImpl.kt
git commit -m "feat: add TransactionRepository interface and impl with StateFlow cache"
```

---

### Task 4: Register `TransactionRepository` in `RepositoryModule`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/RepositoryModule.kt`

- [ ] **Step 1: Add singleton registration**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.data.repository.AccountRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.AuthRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.DashboardRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.TransactionRepositoryImpl
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidItemsEventBus
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import com.daytimegaming.ourdailyfinances.domain.repository.AuthRepository
import com.daytimegaming.ourdailyfinances.domain.repository.DashboardRepository
import com.daytimegaming.ourdailyfinances.domain.repository.TransactionRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.koin.dsl.module

fun repositoryModule() = module {
    single { Firebase.auth }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<DashboardRepository> { DashboardRepositoryImpl(get()) }
    single<AccountRepository> { AccountRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single { PlaidEventBus() }
    single { PlaidItemsEventBus() }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/RepositoryModule.kt
git commit -m "feat: register TransactionRepository singleton in RepositoryModule"
```

---

### Task 5: `AccountDetailViewModel` — replace mock data with real combine flow

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/account/AccountDetailViewModel.kt`

- [ ] **Step 1: Replace the entire file**

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
import com.daytimegaming.ourdailyfinances.domain.repository.TransactionRepository
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class AccountDetailScreenState {
    data object Loading : AccountDetailScreenState()
    data class Error(val message: String) : AccountDetailScreenState()
    data class Loaded(
        val account: Account,
        val transactions: List<Transaction>,
    ) : AccountDetailScreenState()
}

class AccountDetailViewModel(
    private val accountId: String,
    private val accountUseCase: AccountUseCase,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AccountDetailScreenState>(AccountDetailScreenState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            transactionRepository.refresh()
        }
        viewModelScope.launch {
            combine(
                accountUseCase.GetAccounts(),
                transactionRepository.transactions,
            ) { accountsResponse, transactionsResponse ->
                when {
                    accountsResponse is Response.Error ->
                        AccountDetailScreenState.Error(accountsResponse.message)
                    transactionsResponse is Response.Error ->
                        AccountDetailScreenState.Error(transactionsResponse.message)
                    accountsResponse is Response.Success && transactionsResponse is Response.Success -> {
                        val account = accountsResponse.data.find { it.accountId == accountId }
                            ?: return@combine AccountDetailScreenState.Error("Account not found")
                        val filtered = transactionsResponse.data.filter { it.accountId == accountId }
                        AccountDetailScreenState.Loaded(account, filtered)
                    }
                    else -> AccountDetailScreenState.Loading
                }
            }.collect { _state.value = it }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/account/AccountDetailViewModel.kt
git commit -m "feat: replace mock transactions with real TransactionRepository in AccountDetailViewModel"
```

---

### Task 6: Update `ViewModelModule` — add `TransactionRepository` injection

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.android.kt`
- Modify: `shared/src/iosMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.ios.kt`
- Modify: `shared/src/jvmMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.jvm.kt`

The current registration is:
```kotlin
viewModel { params -> AccountDetailViewModel(params.get(), get()) }
```
`params.get()` = `accountId: String`, `get()` = `AccountUseCase`.

It needs a third `get()` for `TransactionRepository`:
```kotlin
viewModel { params -> AccountDetailViewModel(params.get(), get(), get()) }
```
Koin resolves the two `get()` calls by type: `AccountUseCase` and `TransactionRepository` — they are different types so there is no ambiguity.

- [ ] **Step 1: Update Android module**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.account.AccountDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.AccountsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.DashboardsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.SettingsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.TransactionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun viewModelModule(): Module = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::DashboardsViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::TransactionsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
    viewModel { params -> AccountDetailViewModel(params.get(), get(), get()) }
}
```

- [ ] **Step 2: Update iOS module**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.account.AccountDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.AccountsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.DashboardsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.SettingsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.TransactionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun viewModelModule(): Module = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::DashboardsViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::TransactionsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
    viewModel { params -> AccountDetailViewModel(params.get(), get(), get()) }
}
```

- [ ] **Step 3: Update JVM module**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.account.AccountDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.AccountsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.DashboardsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.SettingsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.TransactionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun viewModelModule(): Module = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::DashboardsViewModel)
    viewModelOf(::AccountsViewModel)
    viewModelOf(::TransactionsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
    viewModel { params -> AccountDetailViewModel(params.get(), get(), get()) }
}
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/androidMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.android.kt \
        shared/src/iosMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.ios.kt \
        shared/src/jvmMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.jvm.kt
git commit -m "feat: inject TransactionRepository into AccountDetailViewModel in all platform modules"
```

---

### Task 7: `AccountDetailScreen` — field mappings and amount sign fix

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/account/AccountDetailScreen.kt`

The screen currently references `MockTransaction` fields. Three field name changes and one sign-convention inversion are needed inside the transaction row composable. Everything else (scaffold, hero card, LazyColumn structure) stays unchanged.

- [ ] **Step 1: Update the import — remove MockTransaction, add Transaction**

Find and remove the import:
```kotlin
import com.daytimegaming.ourdailyfinances.presentation.home.MockTransaction
```

Add in its place:
```kotlin
import com.daytimegaming.ourdailyfinances.domain.model.Transaction
```

- [ ] **Step 2: Fix field mappings and amount sign in the transaction row**

Locate the transaction row block inside `items(s.transactions) { tx -> ... }`. Replace it entirely:

```kotlin
items(s.transactions) { tx ->
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tx.merchantName ?: tx.name,
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
                    if (tx.category.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0x1FBEC6E0), shape = RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tx.category.first(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (tx.pending) {
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
            // Plaid API: positive = debit (money out), negative = credit (money in)
            val formattedAmount = if (tx.amount > 0) {
                "-$currencySymbol${tx.amount.formatAmount()}"
            } else {
                "+$currencySymbol${kotlin.math.abs(tx.amount).formatAmount()}"
            }
            val color = if (tx.amount < 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/account/AccountDetailScreen.kt
git commit -m "feat: update AccountDetailScreen to use real Transaction fields and fix amount sign"
```

---

### Task 8: Compile and verify

- [ ] **Step 1: Build the Android app**

```bash
JAVA_HOME=/home/richard/.local/share/JetBrains/Toolbox/apps/android-studio/jbr ./gradlew :androidApp:assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Verify the commit log**

```bash
git log --oneline -8
```

Expected (most recent first):
```
<hash> feat: update AccountDetailScreen to use real Transaction fields and fix amount sign
<hash> feat: inject TransactionRepository into AccountDetailViewModel in all platform modules
<hash> feat: replace mock transactions with real TransactionRepository in AccountDetailViewModel
<hash> feat: register TransactionRepository singleton in RepositoryModule
<hash> feat: add TransactionRepository interface and impl with StateFlow cache
<hash> feat: add getTransactions() to AccountService
<hash> feat: add Transaction domain model and DTOs
```
