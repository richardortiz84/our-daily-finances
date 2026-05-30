# Plaid Add Account Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an "Add Account" button to the HomeScreen that fetches a Plaid link token from the backend and launches Plaid Link to let the user connect a bank account, then auto-refreshes the accounts list on success.

**Architecture:** An `onPlaidTokenReady: (String) -> Unit` callback threads from `MainActivity` → `App` → `AppNavigation` → `HomeScreen`. The `HomeViewModel` fetches the link token via the existing service/repo/usecase pattern and emits it via a `SharedFlow`. A `PlaidEventBus` Koin singleton lets `MainActivity` signal the ViewModel to reload after Plaid succeeds.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Ktor (HTTP), Koin (DI), Plaid Link Android SDK 5.5.2 (`com.plaid.link:sdk-core`), Kotlin Coroutines / SharedFlow.

---

## File Map

| File | Action | Purpose |
|---|---|---|
| `shared/.../data/network/dto/LinkTokenDto.kt` | **Create** | Serializable response DTO for `/link/token/create` |
| `shared/.../data/network/service/AccountService.kt` | **Modify** | Add `createLinkToken()` POST call |
| `shared/.../domain/repository/AccountRepository.kt` | **Modify** | Add `createLinkToken()` to interface |
| `shared/.../data/repository/AccountRepositoryImpl.kt` | **Modify** | Implement `createLinkToken()` |
| `shared/.../domain/usecase/AccountUseCase.kt` | **Modify** | Add `CreateLinkToken` use case class + property on `AccountUseCase` |
| `shared/.../domain/plaid/PlaidEventBus.kt` | **Create** | Koin singleton; MainActivity emits here after Plaid success; HomeViewModel reloads |
| `shared/.../di/RepositoryModule.kt` | **Modify** | Register `PlaidEventBus` singleton |
| `shared/.../di/UseCaseModule.kt` | **Modify** | Register `CreateLinkToken`; update `AccountUseCase` binding |
| `shared/.../presentation/home/HomeScreenState.kt` | **Modify** | Add `isAddingAccount: Boolean = false` to `Loaded` |
| `shared/.../presentation/home/HomeViewModel.kt` | **Modify** | Add `requestAddAccount()`, `plaidLinkTokenEvent` SharedFlow, PlaidEventBus collection |
| `shared/.../presentation/home/HomeScreen.kt` | **Modify** | Add `onPlaidTokenReady` param, "Add Account" button, LaunchedEffect token collector |
| `shared/.../presentation/navigation/AppNavigation.kt` | **Modify** | Add `onPlaidTokenReady` param; pass to `HomeScreen` |
| `shared/.../App.kt` | **Modify** | Add `onPlaidTokenReady` param; pass to `AppNavigation` |
| `androidApp/.../MainActivity.kt` | **Modify** | Register Plaid result launcher; inject `PlaidEventBus`; pass callback to `App` |

All paths under `shared/` are in `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/` unless otherwise noted. All paths under `androidApp/` are in `androidApp/src/main/kotlin/com/daytimegaming/ourdailyfinances/`.

---

## Task 1: LinkTokenDto

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/LinkTokenDto.kt`

- [ ] **Step 1: Create the DTO file**

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkTokenResponse(
    @SerialName("link_token") val linkToken: String,
)
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/LinkTokenDto.kt
git commit -m "feat: add LinkTokenResponse DTO for /link/token/create"
```

---

## Task 2: AccountService — createLinkToken()

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt`

- [ ] **Step 1: Add `createLinkToken()` to AccountService**

Replace the entire file with:

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.AccountListResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkTokenResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

class AccountService(private val apiClient: ApiClient) {
    suspend fun getAccounts(): AccountListResponse =
        apiClient.http.get("${apiClient.baseUrl}/plaid/balance").body()

    suspend fun createLinkToken(): LinkTokenResponse =
        apiClient.http.post("${apiClient.baseUrl}/link/token/create").body()
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt
git commit -m "feat: add createLinkToken() to AccountService"
```

---

## Task 3: AccountRepository interface + AccountRepositoryImpl

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/AccountRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/AccountRepositoryImpl.kt`

- [ ] **Step 1: Add `createLinkToken()` to the interface**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<Response<List<Account>>>
    suspend fun createLinkToken(): String
}
```

- [ ] **Step 2: Implement `createLinkToken()` in AccountRepositoryImpl**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.AccountService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AccountRepositoryImpl(private val service: AccountService) : AccountRepository {

    override fun getAccounts(): Flow<Response<List<Account>>> = flow {
        emit(Response.Loading)
        try {
            val response = service.getAccounts()
            val accounts = response.accounts.map { dto ->
                Account(
                    accountId = dto.accountId,
                    name = dto.name,
                    officialName = dto.officialName,
                    type = dto.type,
                    subtype = dto.subtype,
                    currentBalance = dto.currentBalance,
                    availableBalance = dto.availableBalance,
                    isoCurrencyCode = dto.isoCurrencyCode,
                )
            }
            emit(Response.Success(accounts))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to load accounts"))
        }
    }

    override suspend fun createLinkToken(): String {
        return service.createLinkToken().linkToken
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/AccountRepository.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/AccountRepositoryImpl.kt
git commit -m "feat: add createLinkToken() to AccountRepository and AccountRepositoryImpl"
```

---

## Task 4: CreateLinkToken use case

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/usecase/AccountUseCase.kt`

- [ ] **Step 1: Add `CreateLinkToken` class and update `AccountUseCase`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccounts(private val repo: AccountRepository) {
    operator fun invoke(): Flow<Response<List<Account>>> = repo.getAccounts()
}

class CreateLinkToken(private val repo: AccountRepository) {
    suspend operator fun invoke(): String = repo.createLinkToken()
}

class AccountUseCase(
    val GetAccounts: GetAccounts,
    val CreateLinkToken: CreateLinkToken,
)
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/usecase/AccountUseCase.kt
git commit -m "feat: add CreateLinkToken use case"
```

---

## Task 5: PlaidEventBus

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidEventBus.kt`

- [ ] **Step 1: Create PlaidEventBus**

```kotlin
package com.daytimegaming.ourdailyfinances.domain.plaid

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PlaidEventBus {
    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    suspend fun accountLinked() = _events.emit(Unit)
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidEventBus.kt
git commit -m "feat: add PlaidEventBus singleton for post-link reload signalling"
```

---

## Task 6: DI wiring

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/RepositoryModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/UseCaseModule.kt`

- [ ] **Step 1: Register PlaidEventBus in RepositoryModule**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.data.repository.AccountRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.AuthRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.DashboardRepositoryImpl
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.repository.AccountRepository
import com.daytimegaming.ourdailyfinances.domain.repository.AuthRepository
import com.daytimegaming.ourdailyfinances.domain.repository.DashboardRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.koin.dsl.module

fun repositoryModule() = module {
    single { Firebase.auth }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<DashboardRepository> { DashboardRepositoryImpl(get()) }
    single<AccountRepository> { AccountRepositoryImpl(get()) }
    single { PlaidEventBus() }
}
```

- [ ] **Step 2: Register CreateLinkToken in UseCaseModule**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.AuthUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.CreateLinkToken
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.GetAccounts
import com.daytimegaming.ourdailyfinances.domain.usecase.GetCurrentUser
import com.daytimegaming.ourdailyfinances.domain.usecase.GetDashboardDetail
import com.daytimegaming.ourdailyfinances.domain.usecase.GetDashboards
import com.daytimegaming.ourdailyfinances.domain.usecase.GetIdToken
import com.daytimegaming.ourdailyfinances.domain.usecase.LoginUser
import com.daytimegaming.ourdailyfinances.domain.usecase.RegisterUser
import com.daytimegaming.ourdailyfinances.domain.usecase.SignOutUser
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
    singleOf(::DashboardUseCase)

    singleOf(::GetAccounts)
    singleOf(::CreateLinkToken)
    singleOf(::AccountUseCase)
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/RepositoryModule.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/UseCaseModule.kt
git commit -m "feat: wire PlaidEventBus and CreateLinkToken into Koin DI"
```

---

## Task 7: HomeScreenState

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreenState.kt`

- [ ] **Step 1: Add `isAddingAccount` to `Loaded`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.home

import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard

sealed class HomeScreenState {
    data object Loading : HomeScreenState()
    data class Error(val message: String) : HomeScreenState()
    data class Loaded(
        val dashboards: List<Dashboard>,
        val accounts: List<Account>,
        val isAddingAccount: Boolean = false,
    ) : HomeScreenState()
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreenState.kt
git commit -m "feat: add isAddingAccount flag to HomeScreenState.Loaded"
```

---

## Task 8: HomeViewModel

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeViewModel.kt`

- [ ] **Step 1: Update HomeViewModel with Plaid token fetch and event bus**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dashboardUseCase: DashboardUseCase,
    private val accountUseCase: AccountUseCase,
    private val plaidEventBus: PlaidEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val state = _state.asStateFlow()

    private val _plaidLinkTokenEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val plaidLinkTokenEvent: SharedFlow<String> = _plaidLinkTokenEvent.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            plaidEventBus.events.collect { load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { HomeScreenState.Loading }
            combine(
                dashboardUseCase.GetDashboards(),
                accountUseCase.GetAccounts(),
            ) { dashboardsResponse, accountsResponse ->
                when {
                    dashboardsResponse is Response.Error ->
                        HomeScreenState.Error(dashboardsResponse.message)
                    accountsResponse is Response.Error ->
                        HomeScreenState.Error(accountsResponse.message)
                    dashboardsResponse is Response.Success && accountsResponse is Response.Success ->
                        HomeScreenState.Loaded(
                            dashboards = dashboardsResponse.data,
                            accounts = accountsResponse.data,
                        )
                    else -> HomeScreenState.Loading
                }
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }

    fun requestAddAccount() {
        viewModelScope.launch {
            val current = _state.value
            if (current is HomeScreenState.Loaded) {
                _state.update { current.copy(isAddingAccount = true) }
            }
            try {
                val token = accountUseCase.CreateLinkToken()
                val loaded = _state.value
                if (loaded is HomeScreenState.Loaded) {
                    _state.update { loaded.copy(isAddingAccount = false) }
                }
                _plaidLinkTokenEvent.emit(token)
            } catch (e: Exception) {
                _state.update { HomeScreenState.Error(e.message ?: "Failed to start account linking") }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeViewModel.kt
git commit -m "feat: add requestAddAccount() and Plaid event bus reload to HomeViewModel"
```

---

## Task 9: HomeScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreen.kt`

- [ ] **Step 1: Add Add Account button and token collection**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.Dashboard
import com.daytimegaming.ourdailyfinances.presentation.util.formatAmount
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDashboardClick: (String) -> Unit,
    onPlaidTokenReady: (String) -> Unit,
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.plaidLinkTokenEvent.collect { token ->
            onPlaidTokenReady(token)
        }
    }

    ScreenContent(
        state = state,
        onDashboardClick = onDashboardClick,
        onAddAccountClick = { viewModel.requestAddAccount() },
    )
}

@Composable
private fun ScreenContent(
    state: HomeScreenState,
    onDashboardClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        },
    ) { innerPadding ->
        when (state) {
            is HomeScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is HomeScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            is HomeScreenState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .safeContentPadding(),
                ) {
                    item {
                        Text(
                            text = "Dashboards",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    if (state.dashboards.isEmpty()) {
                        item {
                            Text(
                                text = "No dashboards yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        items(state.dashboards, key = { it.dashboardId }) { dashboard ->
                            DashboardItem(dashboard = dashboard, onClick = { onDashboardClick(dashboard.dashboardId) })
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                    item {
                        AccountsSectionHeader(
                            isAddingAccount = state.isAddingAccount,
                            onAddAccountClick = onAddAccountClick,
                        )
                    }
                    if (state.accounts.isEmpty()) {
                        item {
                            Text(
                                text = "No linked accounts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        items(state.accounts, key = { it.accountId }) { account ->
                            AccountItem(account = account)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsSectionHeader(
    isAddingAccount: Boolean,
    onAddAccountClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (isAddingAccount) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            IconButton(onClick = onAddAccountClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account",
                )
            }
        }
    }
}

@Composable
private fun DashboardItem(dashboard: Dashboard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = dashboard.name, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun AccountItem(account: Account) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleSmall)
                account.subtype?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            account.currentBalance?.let { balance ->
                Text(
                    text = "${account.isoCurrencyCode ?: ""} ${balance.formatAmount()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreen.kt
git commit -m "feat: add Add Account button and Plaid token event collection to HomeScreen"
```

---

## Task 10: AppNavigation + App — thread the callback

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/navigation/AppNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/App.kt`

- [ ] **Step 1: Update AppNavigation to accept and pass onPlaidTokenReady**

Replace the entire file:

```kotlin
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
```

- [ ] **Step 2: Update App.kt to accept and pass onPlaidTokenReady**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.navigation.AppNavigation
import com.daytimegaming.ourdailyfinances.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    onPlaidTokenReady: (String) -> Unit = {},
) {
    AppTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
    ) {
        val mainViewModel = koinViewModel<MainViewModel>()
        val currentUser by mainViewModel.currentUser.collectAsStateWithLifecycle()
        AppNavigation(
            isAuthenticated = currentUser != null,
            onPlaidTokenReady = onPlaidTokenReady,
        )
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :shared:compileKotlinAndroid --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/navigation/AppNavigation.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/App.kt
git commit -m "feat: thread onPlaidTokenReady callback through App and AppNavigation"
```

---

## Task 11: MainActivity — Plaid integration

**Files:**
- Modify: `androidApp/src/main/kotlin/com/daytimegaming/ourdailyfinances/MainActivity.kt`

- [ ] **Step 1: Update MainActivity with Plaid launcher, event bus injection, and App callback**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.plaid.link.Plaid
import com.plaid.link.PlaidActivityResultContract
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkError
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val plaidEventBus: PlaidEventBus by inject()

    private val plaidLauncher = registerForActivityResult(PlaidActivityResultContract()) { result ->
        when (result) {
            is LinkSuccess -> lifecycleScope.launch { plaidEventBus.accountLinked() }
            is LinkExit -> Unit
            is LinkError -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = false,
                onPlaidTokenReady = { linkToken ->
                    val config = LinkTokenConfiguration.Builder()
                        .token(linkToken)
                        .build()
                    Plaid.create(application, config).submit(plaidLauncher)
                },
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        darkTheme = false,
        dynamicColor = false,
    )
}
```

> **Note on Plaid SDK API:** If `Plaid.create(application, config).submit(plaidLauncher)` does not compile, check the Plaid 5.5.2 SDK for the exact `PlaidHandler` API. The alternative is `Plaid.create(application, config, callback)` with an async `LinkTokenCallback`. Check the SDK's `PlaidHandler` and `Plaid` class for the exact method signatures.

- [ ] **Step 2: Build the Android debug APK to verify the full integration**

```bash
./gradlew :androidApp:assembleDebug --quiet
```

Expected: BUILD SUCCESSFUL with APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/daytimegaming/ourdailyfinances/MainActivity.kt
git commit -m "feat: integrate Plaid Link SDK in MainActivity with event bus reload"
```

---

## Verification Checklist

After all tasks complete, manually test on an Android device or emulator:

- [ ] App builds and installs: `./gradlew :androidApp:installDebug`
- [ ] HomeScreen shows "Accounts" section with a `+` icon button on the right of the header
- [ ] Tapping `+` shows a loading spinner in the header while the link token is fetched
- [ ] Plaid Link UI opens after the token is fetched
- [ ] Completing Plaid Link (using Plaid sandbox credentials) refreshes the accounts list
- [ ] Cancelling Plaid Link returns to HomeScreen with button re-enabled and no state change
- [ ] Network error during token fetch shows the error state on HomeScreen
