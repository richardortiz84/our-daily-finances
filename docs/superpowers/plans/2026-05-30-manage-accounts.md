# Manage Accounts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Manage Accounts screen reachable from the Accounts tab that lists linked Plaid items and allows unlinking them with a confirmation dialog, refreshing the Accounts tab automatically on success.

**Architecture:** A new `AppRoute.ManageAccounts` full-screen push follows the same `NavBackStack` pattern as `DashboardDetail`. A new `PlaidItemsEventBus` singleton carries `Unit` reload signals from `ManageAccountsViewModel` (post-unlink) to `AccountsViewModel` (which calls `load()`). The configure button in `AccountsScreen` is only rendered when at least one account is linked.

**Tech Stack:** Kotlin Multiplatform, Ktor (HTTP), Koin (DI), Jetpack Compose, Navigation3 (`NavBackStack`/`NavDisplay`), kotlinx.serialization

---

## File Map

| File | Action |
|---|---|
| `shared/.../domain/model/PlaidItem.kt` | Create |
| `shared/.../data/network/dto/PlaidItemDto.kt` | Create |
| `shared/.../data/network/service/AccountService.kt` | Modify |
| `shared/.../domain/repository/AccountRepository.kt` | Modify |
| `shared/.../data/repository/AccountRepositoryImpl.kt` | Modify |
| `shared/.../domain/usecase/AccountUseCase.kt` | Modify |
| `shared/.../di/UseCaseModule.kt` | Modify |
| `shared/.../domain/plaid/PlaidItemsEventBus.kt` | Create |
| `shared/.../di/RepositoryModule.kt` | Modify |
| `shared/.../presentation/home/AccountsViewModel.kt` | Modify |
| `shared/.../presentation/navigation/AppRoute.kt` | Modify |
| `shared/.../presentation/navigation/AppNavigation.kt` | Modify |
| `shared/.../presentation/home/HomeScreen.kt` | Modify |
| `shared/.../presentation/home/AccountsScreen.kt` | Modify |
| `shared/.../presentation/manageaccounts/ManageAccountsScreenState.kt` | Create |
| `shared/.../presentation/manageaccounts/ManageAccountsViewModel.kt` | Create |
| `shared/.../presentation/manageaccounts/ManageAccountsScreen.kt` | Create |
| `shared/.../di/ViewModelModule.android.kt` | Modify |
| `shared/.../di/ViewModelModule.ios.kt` | Modify |
| `shared/.../di/ViewModelModule.jvm.kt` | Modify |

All `shared/...` paths are under `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/` except the platform-specific ViewModelModule files which live in their respective source sets (`androidMain`, `iosMain`, `jvmMain`).

---

### Task 1: `PlaidItem` domain model and DTOs

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/model/PlaidItem.kt`
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/PlaidItemDto.kt`

- [ ] **Step 1: Create `PlaidItem.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.domain.model

data class PlaidItem(
    val itemId: String,
    val institutionName: String,
    val createdAt: String,
)
```

- [ ] **Step 2: Create `PlaidItemDto.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaidItemDto(
    @SerialName("item_id") val itemId: String,
    @SerialName("institution_name") val institutionName: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class PlaidItemsResponse(
    @SerialName("items") val items: List<PlaidItemDto>,
)
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/model/PlaidItem.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/dto/PlaidItemDto.kt
git commit -m "feat: add PlaidItem domain model and DTOs"
```

---

### Task 2: `AccountService` — `getPlaidItems()` and `unlinkItem()`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt`

- [ ] **Step 1: Add the two new methods**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.data.network.service

import com.daytimegaming.ourdailyfinances.data.network.ApiClient
import com.daytimegaming.ourdailyfinances.data.network.dto.AccountListResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeRequest
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkExchangeResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.LinkTokenResponse
import com.daytimegaming.ourdailyfinances.data.network.dto.PlaidItemsResponse
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
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/network/service/AccountService.kt
git commit -m "feat: add getPlaidItems() and unlinkItem() to AccountService"
```

---

### Task 3: `AccountRepository` interface and impl

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/AccountRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/AccountRepositoryImpl.kt`

- [ ] **Step 1: Add `getPlaidItems()` and `unlinkItem()` to the interface**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.domain.repository

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<Response<List<Account>>>
    suspend fun createLinkToken(): String
    suspend fun exchangePublicToken(publicToken: String, institutionName: String?)
    fun getPlaidItems(): Flow<Response<List<PlaidItem>>>
    suspend fun unlinkItem(itemId: String)
}
```

- [ ] **Step 2: Implement both methods in `AccountRepositoryImpl`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.data.repository

import com.daytimegaming.ourdailyfinances.data.network.service.AccountService
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem
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

    override suspend fun createLinkToken(): String =
        service.createLinkToken().linkToken

    override suspend fun exchangePublicToken(publicToken: String, institutionName: String?) {
        service.exchangePublicToken(publicToken, institutionName)
    }

    override fun getPlaidItems(): Flow<Response<List<PlaidItem>>> = flow {
        emit(Response.Loading)
        try {
            val items = service.getPlaidItems().items.map { dto ->
                PlaidItem(
                    itemId = dto.itemId,
                    institutionName = dto.institutionName,
                    createdAt = dto.createdAt,
                )
            }
            emit(Response.Success(items))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to load linked accounts"))
        }
    }

    override suspend fun unlinkItem(itemId: String) {
        service.unlinkItem(itemId)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/repository/AccountRepository.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/data/repository/AccountRepositoryImpl.kt
git commit -m "feat: add getPlaidItems() and unlinkItem() to AccountRepository"
```

---

### Task 4: `GetPlaidItems` + `UnlinkPlaidItem` use cases and DI

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/usecase/AccountUseCase.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/UseCaseModule.kt`

- [ ] **Step 1: Add both use cases to `AccountUseCase.kt`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.domain.usecase

import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem
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

class GetPlaidItems(private val repo: AccountRepository) {
    operator fun invoke(): Flow<Response<List<PlaidItem>>> = repo.getPlaidItems()
}

class UnlinkPlaidItem(private val repo: AccountRepository) {
    suspend operator fun invoke(itemId: String) = repo.unlinkItem(itemId)
}

class AccountUseCase(
    val GetAccounts: GetAccounts,
    val CreateLinkToken: CreateLinkToken,
    val ExchangePublicToken: ExchangePublicToken,
    val GetPlaidItems: GetPlaidItems,
    val UnlinkPlaidItem: UnlinkPlaidItem,
)
```

- [ ] **Step 2: Wire both use cases in `UseCaseModule.kt`**

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
import com.daytimegaming.ourdailyfinances.domain.usecase.GetPlaidItems
import com.daytimegaming.ourdailyfinances.domain.usecase.LoginUser
import com.daytimegaming.ourdailyfinances.domain.usecase.RegisterUser
import com.daytimegaming.ourdailyfinances.domain.usecase.SignOutUser
import com.daytimegaming.ourdailyfinances.domain.usecase.UnlinkPlaidItem
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
    singleOf(::GetPlaidItems)
    singleOf(::UnlinkPlaidItem)
    singleOf(::AccountUseCase)
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/usecase/AccountUseCase.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/UseCaseModule.kt
git commit -m "feat: add GetPlaidItems and UnlinkPlaidItem use cases"
```

---

### Task 5: `PlaidItemsEventBus` and `RepositoryModule`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidItemsEventBus.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/RepositoryModule.kt`

- [ ] **Step 1: Create `PlaidItemsEventBus.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.domain.plaid

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PlaidItemsEventBus {
    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    suspend fun itemsChanged() = _events.emit(Unit)
}
```

- [ ] **Step 2: Register `PlaidItemsEventBus` in `RepositoryModule.kt`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.data.repository.AccountRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.AuthRepositoryImpl
import com.daytimegaming.ourdailyfinances.data.repository.DashboardRepositoryImpl
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidItemsEventBus
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
    single { PlaidItemsEventBus() }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/domain/plaid/PlaidItemsEventBus.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/di/RepositoryModule.kt
git commit -m "feat: add PlaidItemsEventBus and register in RepositoryModule"
```

---

### Task 6: `AccountsViewModel` — subscribe to `PlaidItemsEventBus`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/AccountsViewModel.kt`

- [ ] **Step 1: Inject `PlaidItemsEventBus` and add reload subscription**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.model.Account
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidEventBus
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidItemsEventBus
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AccountsScreenState {
    data object Loading : AccountsScreenState()
    data class Error(val message: String) : AccountsScreenState()
    data class Loaded(
        val accounts: List<Account>,
        val isAddingAccount: Boolean = false,
        val isLinkingAccount: Boolean = false,
    ) : AccountsScreenState()
}

class AccountsViewModel(
    private val accountUseCase: AccountUseCase,
    private val plaidEventBus: PlaidEventBus,
    private val plaidItemsEventBus: PlaidItemsEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<AccountsScreenState>(AccountsScreenState.Loading)
    val state = _state.asStateFlow()

    private val _plaidLinkTokenEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val plaidLinkTokenEvent: SharedFlow<String> = _plaidLinkTokenEvent.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            plaidEventBus.events.collect { result ->
                _state.update { s ->
                    if (s is AccountsScreenState.Loaded) s.copy(isLinkingAccount = true) else s
                }
                try {
                    accountUseCase.ExchangePublicToken(result.publicToken, result.institutionName)
                    load()
                } catch (e: Exception) {
                    _state.update { AccountsScreenState.Error(e.message ?: "Failed to link account") }
                }
            }
        }
        viewModelScope.launch {
            plaidItemsEventBus.events.collect { load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { AccountsScreenState.Loading }
            accountUseCase.GetAccounts().collect { response ->
                when (response) {
                    is Response.Error -> _state.update { AccountsScreenState.Error(response.message) }
                    is Response.Success -> _state.update {
                        AccountsScreenState.Loaded(accounts = response.data)
                    }
                    Response.Loading -> _state.update { AccountsScreenState.Loading }
                }
            }
        }
    }

    fun requestAddAccount() {
        viewModelScope.launch {
            _state.update { s -> if (s is AccountsScreenState.Loaded) s.copy(isAddingAccount = true) else s }
            try {
                val token = accountUseCase.CreateLinkToken()
                _state.update { s -> if (s is AccountsScreenState.Loaded) s.copy(isAddingAccount = false) else s }
                _plaidLinkTokenEvent.emit(token)
            } catch (e: Exception) {
                _state.update { AccountsScreenState.Error(e.message ?: "Failed to start account linking") }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/AccountsViewModel.kt
git commit -m "feat: subscribe AccountsViewModel to PlaidItemsEventBus for auto-reload"
```

---

### Task 7: Navigation — `AppRoute`, `AppNavigation`, `HomeScreen`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/navigation/AppRoute.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/navigation/AppNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreen.kt`

- [ ] **Step 1: Add `ManageAccounts` to `AppRoute.kt`**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class AppRoute : NavKey {
    @Serializable data object Login : AppRoute()
    @Serializable data object Register : AppRoute()
    @Serializable data object Home : AppRoute()
    @Serializable data class DashboardDetail(val dashboardId: String) : AppRoute()
    @Serializable data object ManageAccounts : AppRoute()
}
```

- [ ] **Step 2: Register `ManageAccounts` and add its entry in `AppNavigation.kt`**

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
import com.daytimegaming.ourdailyfinances.presentation.manageaccounts.ManageAccountsScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val navSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Login::class, AppRoute.Login.serializer())
            subclass(AppRoute.Register::class, AppRoute.Register.serializer())
            subclass(AppRoute.Home::class, AppRoute.Home.serializer())
            subclass(AppRoute.DashboardDetail::class, AppRoute.DashboardDetail.serializer())
            subclass(AppRoute.ManageAccounts::class, AppRoute.ManageAccounts.serializer())
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
                    onManageAccounts = { backStack.add(AppRoute.ManageAccounts) },
                )
            }
            entry<AppRoute.DashboardDetail> { route ->
                DashboardDetailScreen(
                    dashboardId = route.dashboardId,
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
            entry<AppRoute.ManageAccounts> {
                ManageAccountsScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
```

- [ ] **Step 3: Add `onManageAccounts` parameter to `HomeScreen` and pass it to `AccountsScreen`**

In `HomeScreen.kt`, add `onManageAccounts: () -> Unit` to the `HomeScreen` composable signature and pass it through to `AccountsScreen`. The `HomeScreen` function currently has this signature:

```kotlin
fun HomeScreen(
    onDashboardClick: (String) -> Unit,
    onPlaidTokenReady: (String) -> Unit,
)
```

Change it to:

```kotlin
fun HomeScreen(
    onDashboardClick: (String) -> Unit,
    onPlaidTokenReady: (String) -> Unit,
    onManageAccounts: () -> Unit,
)
```

And update the `AccountsScreen` call inside the `when (selectedTab)` block from:

```kotlin
HomeTab.Accounts -> AccountsScreen(
    onPlaidTokenReady = onPlaidTokenReady
)
```

to:

```kotlin
HomeTab.Accounts -> AccountsScreen(
    onPlaidTokenReady = onPlaidTokenReady,
    onManageAccounts = onManageAccounts,
)
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/navigation/AppRoute.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/navigation/AppNavigation.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/HomeScreen.kt
git commit -m "feat: add ManageAccounts route and wire onManageAccounts callback through navigation"
```

---

### Task 8: `AccountsScreen` — configure button

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/AccountsScreen.kt`

- [ ] **Step 1: Add `onManageAccounts` parameter and configure button**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daytimegaming.ourdailyfinances.presentation.theme.GlassCard
import com.daytimegaming.ourdailyfinances.presentation.util.formatAmount
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountsScreen(
    onPlaidTokenReady: (String) -> Unit,
    onManageAccounts: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.plaidLinkTokenEvent.collect { token ->
            onPlaidTokenReady(token)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Linked Accounts",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val loaded = state as? AccountsScreenState.Loaded
                if (loaded != null && loaded.accounts.isNotEmpty()) {
                    IconButton(onClick = onManageAccounts) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Manage Accounts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (loaded?.isAddingAccount == true) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Button(
                        onClick = { viewModel.requestAddAccount() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF003731),
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Link")
                    }
                }
            }
        }

        when (val s = state) {
            is AccountsScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AccountsScreenState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = s.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is AccountsScreenState.Loaded -> {
                if (s.isLinkingAccount) {
                    androidx.compose.material3.AlertDialog(
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
                if (s.accounts.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No linked bank accounts. Click Link above to connect via Plaid.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        items(s.accounts, key = { it.accountId }) { account ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        account.subtype?.let {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0x1FBEC6E0), shape = RoundedCornerShape(999.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    text = it.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                )
                                            }
                                        }
                                    }
                                    account.currentBalance?.let { balance ->
                                        Text(
                                            text = "${account.isoCurrencyCode ?: "$"} ${balance.formatAmount()}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary,
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
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/home/AccountsScreen.kt
git commit -m "feat: add configure button to AccountsScreen header"
```

---

### Task 9: `ManageAccountsScreenState` and `ManageAccountsViewModel`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/manageaccounts/ManageAccountsScreenState.kt`
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/manageaccounts/ManageAccountsViewModel.kt`

- [ ] **Step 1: Create `ManageAccountsScreenState.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.manageaccounts

import com.daytimegaming.ourdailyfinances.domain.model.PlaidItem

sealed class ManageAccountsScreenState {
    data object Loading : ManageAccountsScreenState()
    data class Error(val message: String) : ManageAccountsScreenState()
    data class Loaded(
        val items: List<PlaidItem>,
        val unlinkingItemId: String? = null,
    ) : ManageAccountsScreenState()
}
```

- [ ] **Step 2: Create `ManageAccountsViewModel.kt`**

```kotlin
package com.daytimegaming.ourdailyfinances.presentation.manageaccounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daytimegaming.ourdailyfinances.domain.Response
import com.daytimegaming.ourdailyfinances.domain.plaid.PlaidItemsEventBus
import com.daytimegaming.ourdailyfinances.domain.usecase.AccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageAccountsViewModel(
    private val accountUseCase: AccountUseCase,
    private val plaidItemsEventBus: PlaidItemsEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<ManageAccountsScreenState>(ManageAccountsScreenState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { ManageAccountsScreenState.Loading }
            accountUseCase.GetPlaidItems().collect { response ->
                _state.update {
                    when (response) {
                        is Response.Loading -> ManageAccountsScreenState.Loading
                        is Response.Success -> ManageAccountsScreenState.Loaded(response.data)
                        is Response.Error -> ManageAccountsScreenState.Error(response.message)
                    }
                }
            }
        }
    }

    fun confirmUnlink(itemId: String) {
        _state.update { s ->
            if (s is ManageAccountsScreenState.Loaded) s.copy(unlinkingItemId = itemId) else s
        }
    }

    fun dismissUnlink() {
        _state.update { s ->
            if (s is ManageAccountsScreenState.Loaded) s.copy(unlinkingItemId = null) else s
        }
    }

    fun executeUnlink(itemId: String) {
        viewModelScope.launch {
            try {
                accountUseCase.UnlinkPlaidItem(itemId)
                plaidItemsEventBus.itemsChanged()
                load()
            } catch (e: Exception) {
                _state.update { ManageAccountsScreenState.Error(e.message ?: "Failed to unlink account") }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/manageaccounts/ManageAccountsScreenState.kt \
        shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/manageaccounts/ManageAccountsViewModel.kt
git commit -m "feat: add ManageAccountsScreenState and ManageAccountsViewModel"
```

---

### Task 10: `ManageAccountsScreen`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/manageaccounts/ManageAccountsScreen.kt`

- [ ] **Step 1: Create `ManageAccountsScreen.kt`**

```kotlin
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
```

- [ ] **Step 2: Commit**

```bash
git add shared/src/commonMain/kotlin/com/daytimegaming/ourdailyfinances/presentation/manageaccounts/ManageAccountsScreen.kt
git commit -m "feat: add ManageAccountsScreen with unlink confirmation dialog"
```

---

### Task 11: `ViewModelModule` — register `ManageAccountsViewModel`

**Files:**
- Modify: `shared/src/androidMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.android.kt`
- Modify: `shared/src/iosMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.ios.kt`
- Modify: `shared/src/jvmMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.jvm.kt`

- [ ] **Step 1: Add `ManageAccountsViewModel` to the Android module**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.AccountsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.DashboardsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.SettingsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.TransactionsViewModel
import com.daytimegaming.ourdailyfinances.presentation.manageaccounts.ManageAccountsViewModel
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
    viewModelOf(::ManageAccountsViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
}
```

- [ ] **Step 2: Add `ManageAccountsViewModel` to the iOS module**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.AccountsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.DashboardsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.SettingsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.TransactionsViewModel
import com.daytimegaming.ourdailyfinances.presentation.manageaccounts.ManageAccountsViewModel
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
    viewModelOf(::ManageAccountsViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
}
```

- [ ] **Step 3: Add `ManageAccountsViewModel` to the JVM module**

Replace the entire file:

```kotlin
package com.daytimegaming.ourdailyfinances.di

import com.daytimegaming.ourdailyfinances.presentation.MainViewModel
import com.daytimegaming.ourdailyfinances.presentation.auth.AuthViewModel
import com.daytimegaming.ourdailyfinances.presentation.dashboard.DashboardDetailViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.AccountsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.DashboardsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.SettingsViewModel
import com.daytimegaming.ourdailyfinances.presentation.home.TransactionsViewModel
import com.daytimegaming.ourdailyfinances.presentation.manageaccounts.ManageAccountsViewModel
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
    viewModelOf(::ManageAccountsViewModel)
    viewModel { params -> DashboardDetailViewModel(params.get(), get()) }
}
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/androidMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.android.kt \
        shared/src/iosMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.ios.kt \
        shared/src/jvmMain/kotlin/com/daytimegaming/ourdailyfinances/di/ViewModelModule.jvm.kt
git commit -m "feat: register ManageAccountsViewModel in all platform ViewModelModules"
```

---

### Task 12: Compile and verify

- [ ] **Step 1: Build the Android app**

```bash
JAVA_HOME=/home/richard/.local/share/JetBrains/Toolbox/apps/android-studio/jbr ./gradlew :androidApp:assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Verify the commit log**

```bash
git log --oneline -12
```

Expected (most recent first):
```
<hash> feat: register ManageAccountsViewModel in all platform ViewModelModules
<hash> feat: add ManageAccountsScreen with unlink confirmation dialog
<hash> feat: add ManageAccountsScreenState and ManageAccountsViewModel
<hash> feat: add configure button to AccountsScreen header
<hash> feat: add ManageAccounts route and wire onManageAccounts callback through navigation
<hash> feat: subscribe AccountsViewModel to PlaidItemsEventBus for auto-reload
<hash> feat: add PlaidItemsEventBus and register in RepositoryModule
<hash> feat: add GetPlaidItems and UnlinkPlaidItem use cases
<hash> feat: add getPlaidItems() and unlinkItem() to AccountRepository
<hash> feat: add getPlaidItems() and unlinkItem() to AccountService
<hash> feat: add PlaidItem domain model and DTOs
```
