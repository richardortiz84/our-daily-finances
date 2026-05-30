# Plaid Link Exchange Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the Plaid Link SDK returns a `LinkSuccess`, call `POST /plaid/link/exchange` with the public token and institution name to finalize account linking, showing a non-dismissable loading dialog during the exchange.

**Architecture:** `MainActivity` passes `publicToken` and `institutionName` to an evolved `PlaidEventBus` (now `SharedFlow<PlaidLinkResult>`). `HomeViewModel` collects the event, sets `isLinkingAccount = true`, calls the new `ExchangePublicToken` use case, then reloads accounts. The loading dialog is rendered in `HomeScreen` whenever `isLinkingAccount` is `true`.

**Tech Stack:** Kotlin Multiplatform, Ktor (HTTP), Koin (DI), Jetpack Compose, kotlinx.serialization

---

## File Map

| File | Action |
|---|---|
| `shared/src/commonMain/.../data/network/dto/LinkExchangeRequest.kt` | Create |
| `shared/src/commonMain/.../data/network/dto/LinkExchangeResponse.kt` | Create |
| `shared/src/commonMain/.../data/network/service/AccountService.kt` | Modify |
| `shared/src/commonMain/.../domain/repository/AccountRepository.kt` | Modify |
| `shared/src/commonMain/.../data/repository/AccountRepositoryImpl.kt` | Modify |
| `shared/src/commonMain/.../domain/usecase/AccountUseCase.kt` | Modify |
| `shared/src/commonMain/.../di/UseCaseModule.kt` | Modify |
| `shared/src/commonMain/.../domain/plaid/PlaidLinkResult.kt` | Create |
| `shared/src/commonMain/.../domain/plaid/PlaidEventBus.kt` | Modify |
| `androidApp/src/main/.../MainActivity.kt` | Modify |
| `shared/src/commonMain/.../presentation/home/HomeScreenState.kt` | Modify |
| `shared/src/commonMain/.../presentation/home/HomeViewModel.kt` | Modify |
| `shared/src/commonMain/.../presentation/home/HomeScreen.kt` | Modify |

All paths under `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/` and `androidApp/src/main/kotlin/com/daytimegaming/ourdailyfinances/`.

---

### Task 1: Exchange DTOs

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/LinkExchangeRequest.kt`
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/LinkExchangeResponse.kt`

- [ ] **Step 1: Create `LinkExchangeRequest.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkExchangeRequest(
    @SerialName("public_token") val publicToken: String,
    @SerialName("institution_name") val institutionName: String? = null,
)
```

- [ ] **Step 2: Create `LinkExchangeResponse.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkExchangeResponse(
    @SerialName("item_id") val itemId: String,
    @SerialName("institution_name") val institutionName: String,
)
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/LinkExchangeRequest.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/LinkExchangeResponse.kt
git commit -m "feat: add LinkExchangeRequest and LinkExchangeResponse DTOs"
```

---

### Task 2: `AccountService.exchangePublicToken()`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt`

- [ ] **Step 1: Add `exchangePublicToken()` to `AccountService`**

Replace the entire file with:

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.AccountListResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkTokenResponse
import io.ktor.client.call.body
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
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt
git commit -m "feat: add exchangePublicToken() to AccountService"
```

---

### Task 3: `AccountRepository` interface and impl

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/AccountRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/AccountRepositoryImpl.kt`

- [ ] **Step 1: Add `exchangePublicToken()` to the repository interface**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<Response<List<Account>>>
    suspend fun createLinkToken(): String
    suspend fun exchangePublicToken(publicToken: String, institutionName: String?)
}
```

- [ ] **Step 2: Implement `exchangePublicToken()` in `AccountRepositoryImpl`**

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

    override suspend fun exchangePublicToken(publicToken: String, institutionName: String?) {
        service.exchangePublicToken(publicToken, institutionName)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/AccountRepository.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/AccountRepositoryImpl.kt
git commit -m "feat: add exchangePublicToken() to AccountRepository"
```

---

### Task 4: `ExchangePublicToken` use case and DI wiring

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/usecase/AccountUseCase.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/UseCaseModule.kt`

- [ ] **Step 1: Add `ExchangePublicToken` use case to `AccountUseCase.kt`**

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

class ExchangePublicToken(private val repo: AccountRepository) {
    suspend operator fun invoke(publicToken: String, institutionName: String?) =
        repo.exchangePublicToken(publicToken, institutionName)
}

class AccountUseCase(
    val GetAccounts: GetAccounts,
    val CreateLinkToken: CreateLinkToken,
    val ExchangePublicToken: ExchangePublicToken,
)
```

- [ ] **Step 2: Wire `ExchangePublicToken` in `UseCaseModule.kt`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.AuthUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.CreateLinkToken
import com.daytimegaming.ourdailyfinances.domain.usecase.DashboardUseCase
import com.daytimegaming.ourdailyfinances.domain.usecase.ExchangePublicToken
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
    singleOf(::ExchangePublicToken)
    singleOf(::AccountUseCase)
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/usecase/AccountUseCase.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/UseCaseModule.kt
git commit -m "feat: add ExchangePublicToken use case and wire in DI"
```

---

### Task 5: `PlaidLinkResult` and evolved `PlaidEventBus`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidLinkResult.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidEventBus.kt`

- [ ] **Step 1: Create `PlaidLinkResult.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.domain.plaid

data class PlaidLinkResult(
    val publicToken: String,
    val institutionName: String?,
)
```

- [ ] **Step 2: Evolve `PlaidEventBus` to carry `PlaidLinkResult`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.domain.plaid

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PlaidEventBus {
    private val _events = MutableSharedFlow<PlaidLinkResult>()
    val events: SharedFlow<PlaidLinkResult> = _events.asSharedFlow()

    suspend fun accountLinked(publicToken: String, institutionName: String?) =
        _events.emit(PlaidLinkResult(publicToken, institutionName))
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidLinkResult.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidEventBus.kt
git commit -m "feat: evolve PlaidEventBus to carry PlaidLinkResult with publicToken and institutionName"
```

---

### Task 6: `MainActivity` — pass token data on `LinkSuccess`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/daytimegaming/ourdailyfinances/MainActivity.kt`

- [ ] **Step 1: Update `LinkSuccess` handler to pass token data**

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
import com.plaid.link.OpenPlaidLink
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val plaidEventBus: PlaidEventBus by inject()

    private val plaidLauncher = registerForActivityResult(OpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> lifecycleScope.launch {
                plaidEventBus.accountLinked(
                    publicToken = result.publicToken,
                    institutionName = result.metadata.institution?.name,
                )
            }
            is LinkExit -> Unit
            else -> Unit
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
                    plaidLauncher.launch(config)
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

- [ ] **Step 2: Commit**

```bash
git add androidApp/src/main/kotlin/com/daytimegaming/ourdailyfinances/MainActivity.kt
git commit -m "feat: pass publicToken and institutionName to PlaidEventBus on LinkSuccess"
```

---

### Task 7: `HomeScreenState` — add `isLinkingAccount`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreenState.kt`

- [ ] **Step 1: Add `isLinkingAccount` field to `Loaded`**

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
        val isLinkingAccount: Boolean = false,
    ) : HomeScreenState()
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreenState.kt
git commit -m "feat: add isLinkingAccount to HomeScreenState.Loaded"
```

---

### Task 8: `HomeViewModel` — drive exchange flow from event

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeViewModel.kt`

- [ ] **Step 1: Replace the `plaidEventBus` collector with the exchange flow**

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
            plaidEventBus.events.collect { result ->
                _state.update { s ->
                    if (s is HomeScreenState.Loaded) s.copy(isLinkingAccount = true) else s
                }
                try {
                    accountUseCase.ExchangePublicToken(result.publicToken, result.institutionName)
                    load()
                } catch (e: Exception) {
                    _state.update { HomeScreenState.Error(e.message ?: "Failed to link account") }
                }
            }
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
            _state.update { s -> if (s is HomeScreenState.Loaded) s.copy(isAddingAccount = true) else s }
            try {
                val token = accountUseCase.CreateLinkToken()
                _state.update { s -> if (s is HomeScreenState.Loaded) s.copy(isAddingAccount = false) else s }
                _plaidLinkTokenEvent.emit(token)
            } catch (e: Exception) {
                _state.update { HomeScreenState.Error(e.message ?: "Failed to start account linking") }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeViewModel.kt
git commit -m "feat: drive exchange flow from PlaidEventBus in HomeViewModel"
```

---

### Task 9: `HomeScreen` — non-dismissable loading dialog

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreen.kt`

- [ ] **Step 1: Add the dialog import and conditional dialog to the `Loaded` branch**

Add `AlertDialog` to the imports block and add the dialog inside the `is HomeScreenState.Loaded` branch. Replace the entire file:

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
import androidx.compose.material3.AlertDialog
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
                if (state.isLinkingAccount) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Linking your account...") },
                        text = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        },
                        confirmButton = {},
                    )
                }
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

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreen.kt
git commit -m "feat: add non-dismissable linking dialog to HomeScreen"
```

---

### Task 10: Compile and verify

- [ ] **Step 1: Build the Android app**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. No compilation errors.

- [ ] **Step 2: Verify the complete commit log**

```bash
git log --oneline -8
```

Expected output (most recent first):
```
<hash> feat: add non-dismissable linking dialog to HomeScreen
<hash> feat: drive exchange flow from PlaidEventBus in HomeViewModel
<hash> feat: add isLinkingAccount to HomeScreenState.Loaded
<hash> feat: pass publicToken and institutionName to PlaidEventBus on LinkSuccess
<hash> feat: evolve PlaidEventBus to carry PlaidLinkResult with publicToken and institutionName
<hash> feat: add ExchangePublicToken use case and wire in DI
<hash> feat: add exchangePublicToken() to AccountRepository
<hash> feat: add exchangePublicToken() to AccountService
```
