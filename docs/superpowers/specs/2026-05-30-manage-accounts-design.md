# Manage Accounts — Design Spec

**Date:** 2026-05-30
**Status:** Approved

## Overview

Add a Manage Accounts screen reachable from the Accounts tab. A configure button appears in the Accounts header when at least one account is linked. The screen lists all Plaid items from `GET /plaid/items`; each row shows the institution name and an Unlink button. Pressing Unlink shows a confirmation dialog; confirming calls `DELETE /plaid/items/{item_id}`. On success, `PlaidItemsEventBus` triggers an automatic reload of the Accounts tab.

---

## Section 1: Data Layer

### Domain model

**`PlaidItem`** (`domain/model/PlaidItem.kt`):
```kotlin
data class PlaidItem(
    val itemId: String,
    val institutionName: String,
    val createdAt: String,
)
```

### DTOs

**`PlaidItemDto`** (`data/network/dto/PlaidItemDto.kt`):
```kotlin
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

### `AccountService`

Add two methods:
```kotlin
suspend fun getPlaidItems(): PlaidItemsResponse =
    apiClient.http.get("${apiClient.baseUrl}/plaid/items").body()

suspend fun unlinkItem(itemId: String) =
    apiClient.http.delete("${apiClient.baseUrl}/plaid/items/$itemId")
```

### `AccountRepository` interface

Add:
```kotlin
fun getPlaidItems(): Flow<Response<List<PlaidItem>>>
suspend fun unlinkItem(itemId: String)
```

### `AccountRepositoryImpl`

Add:
```kotlin
override fun getPlaidItems(): Flow<Response<List<PlaidItem>>> = flow {
    emit(Response.Loading)
    try {
        val items = service.getPlaidItems().items.map { dto ->
            PlaidItem(itemId = dto.itemId, institutionName = dto.institutionName, createdAt = dto.createdAt)
        }
        emit(Response.Success(items))
    } catch (e: Exception) {
        emit(Response.Error(e.message ?: "Failed to load items"))
    }
}

override suspend fun unlinkItem(itemId: String) {
    service.unlinkItem(itemId)
}
```

### Use cases

New use cases added to `domain/usecase/AccountUseCase.kt`:
```kotlin
class GetPlaidItems(private val repo: AccountRepository) {
    operator fun invoke(): Flow<Response<List<PlaidItem>>> = repo.getPlaidItems()
}

class UnlinkPlaidItem(private val repo: AccountRepository) {
    suspend operator fun invoke(itemId: String) = repo.unlinkItem(itemId)
}
```

Both added to `AccountUseCase` facade:
```kotlin
class AccountUseCase(
    val GetAccounts: GetAccounts,
    val CreateLinkToken: CreateLinkToken,
    val ExchangePublicToken: ExchangePublicToken,
    val GetPlaidItems: GetPlaidItems,
    val UnlinkPlaidItem: UnlinkPlaidItem,
)
```

Both registered in `UseCaseModule`:
```kotlin
singleOf(::GetPlaidItems)
singleOf(::UnlinkPlaidItem)
```

### `PlaidItemsEventBus`

New singleton in `domain/plaid/PlaidItemsEventBus.kt`:
```kotlin
class PlaidItemsEventBus {
    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    suspend fun itemsChanged() = _events.emit(Unit)
}
```

Registered as a singleton in `RepositoryModule`, alongside the existing `PlaidEventBus` registration.

---

## Section 2: Navigation Wiring

### `AppRoute.ManageAccounts`

Add to `AppRoute.kt`:
```kotlin
@Serializable data object ManageAccounts : AppRoute()
```

Register in `navSavedStateConfig` serializers module:
```kotlin
subclass(AppRoute.ManageAccounts::class, AppRoute.ManageAccounts.serializer())
```

### `AppNavigation`

Add entry:
```kotlin
entry<AppRoute.ManageAccounts> {
    ManageAccountsScreen(
        onNavigateBack = { backStack.removeLastOrNull() },
    )
}
```

### `HomeScreen`

Add `onManageAccounts: () -> Unit` parameter and pass to `AccountsScreen`:
```kotlin
HomeTab.Accounts -> AccountsScreen(
    onPlaidTokenReady = onPlaidTokenReady,
    onManageAccounts = onManageAccounts,
)
```

`AppNavigation` passes the callback:
```kotlin
entry<AppRoute.Home> {
    HomeScreen(
        onDashboardClick = { dashboardId -> backStack.add(AppRoute.DashboardDetail(dashboardId)) },
        onPlaidTokenReady = onPlaidTokenReady,
        onManageAccounts = { backStack.add(AppRoute.ManageAccounts) },
    )
}
```

### `AccountsScreen` — configure button

Add `onManageAccounts: () -> Unit` parameter. In the header row, add a settings/tune icon button after the Add button, only when `accounts.isNotEmpty()`:
```kotlin
if (s.accounts.isNotEmpty()) {
    IconButton(onClick = onManageAccounts) {
        Icon(imageVector = Icons.Default.Tune, contentDescription = "Manage Accounts")
    }
}
```

The configure button sits beside the existing Add button and is always visible when `accounts.isNotEmpty()`, regardless of `isAddingAccount` state. Only the Add button is replaced by the progress indicator during account linking.

### `AccountsViewModel` — refresh subscription

In `init`, subscribe to `PlaidItemsEventBus`:
```kotlin
viewModelScope.launch {
    plaidItemsEventBus.events.collect { load() }
}
```

`PlaidItemsEventBus` is injected via constructor.

---

## Section 3: Manage Accounts Screen

### `ManageAccountsScreenState`

```kotlin
sealed class ManageAccountsScreenState {
    data object Loading : ManageAccountsScreenState()
    data class Error(val message: String) : ManageAccountsScreenState()
    data class Loaded(
        val items: List<PlaidItem>,
        val unlinkingItemId: String? = null,
    ) : ManageAccountsScreenState()
}
```

`unlinkingItemId` is the `item_id` of the item whose confirmation dialog is currently shown. `null` means no dialog.

### `ManageAccountsViewModel`

```kotlin
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
        _state.update { s -> if (s is ManageAccountsScreenState.Loaded) s.copy(unlinkingItemId = itemId) else s }
    }

    fun dismissUnlink() {
        _state.update { s -> if (s is ManageAccountsScreenState.Loaded) s.copy(unlinkingItemId = null) else s }
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

### `ManageAccountsScreen`

- Top bar with back arrow (`onNavigateBack`) and title "Manage Accounts"
- `LazyColumn` of `GlassCard` rows matching the existing `AccountsScreen` style
- Each row: institution name on the left, "Unlink" `OutlinedButton` on the right
- When `state.unlinkingItemId != null`, an `AlertDialog`:
  - Title: "Unlink [institutionName]?"
  - Body: "This will remove all accounts and transactions linked through this connection."
  - Buttons: Cancel (calls `dismissUnlink()`) and Unlink (calls `executeUnlink(itemId)`)
  - `onDismissRequest` calls `dismissUnlink()` (dialog is dismissable — unlike the linking dialog)

### `ViewModelModule`

Register `ManageAccountsViewModel` using Koin's `viewModelOf`:
```kotlin
viewModelOf(::ManageAccountsViewModel)
```

---

## Data Flow Summary

```
User taps configure button (accounts.isNotEmpty())
        ↓
onManageAccounts() → AppNavigation pushes AppRoute.ManageAccounts
        ↓
ManageAccountsScreen loads → GET /plaid/items → list rendered
        ↓
User taps Unlink → confirmUnlink(itemId) → dialog shown
        ↓
User confirms → executeUnlink(itemId)
        ↓
DELETE /plaid/items/{item_id}
        ↓ success                    ↓ failure
plaidItemsEventBus.itemsChanged()   ManageAccountsScreenState.Error
load() → item removed from list
        ↓
AccountsViewModel collects PlaidItemsEventBus.events → load()
Accounts tab refreshes automatically
```

---

## Files Changed

| File | Action |
|---|---|
| `domain/model/PlaidItem.kt` | New |
| `data/network/dto/PlaidItemDto.kt` | New (contains `PlaidItemDto` + `PlaidItemsResponse`) |
| `data/network/service/AccountService.kt` | Add `getPlaidItems()`, `unlinkItem()` |
| `domain/repository/AccountRepository.kt` | Add `getPlaidItems()`, `unlinkItem()` |
| `data/repository/AccountRepositoryImpl.kt` | Implement both |
| `domain/usecase/AccountUseCase.kt` | Add `GetPlaidItems`, `UnlinkPlaidItem` |
| `di/UseCaseModule.kt` | Wire new use cases |
| `domain/plaid/PlaidItemsEventBus.kt` | New |
| `di/RepositoryModule.kt` | Register `PlaidItemsEventBus` singleton |
| `presentation/navigation/AppRoute.kt` | Add `ManageAccounts` route |
| `presentation/navigation/AppNavigation.kt` | Register route, add `onManageAccounts` callback |
| `presentation/home/HomeScreen.kt` | Add `onManageAccounts` param, pass to `AccountsScreen` |
| `presentation/home/AccountsScreen.kt` | Add configure button, `onManageAccounts` param |
| `presentation/home/AccountsViewModel.kt` | Inject `PlaidItemsEventBus`, subscribe for reload |
| `presentation/manageaccounts/ManageAccountsScreenState.kt` | New |
| `presentation/manageaccounts/ManageAccountsViewModel.kt` | New |
| `presentation/manageaccounts/ManageAccountsScreen.kt` | New |
| `di/ViewModelModule.kt` | Register `ManageAccountsViewModel` |
