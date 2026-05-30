# Plaid Add Account — Design Spec
**Date:** 2026-05-29  
**Scope:** Android only (early implementation)  
**Feature:** "Add Account" button on HomeScreen that fetches a Plaid link token from the backend and launches Plaid Link to let the user connect a bank account.

---

## Overview

The HomeScreen shows a list of linked accounts. An "Add Account" icon button (`+`) placed inline with the "Accounts" section header triggers the Plaid Link flow. The link token is fetched from the backend API (`POST /link/token/create`) in shared code. The Plaid SDK (already a dependency) is launched from `MainActivity` via a callback. After the user successfully links an account, the accounts list auto-refreshes.

---

## Architecture

### Approach

Callback threading (Approach A): an `onPlaidTokenReady: (String) -> Unit` lambda threads from `MainActivity` → `App` → `AppNavigation` → `HomeScreen`. The ViewModel fetches the link token and emits it via a `SharedFlow`. A `PlaidEventBus` Koin singleton signals the ViewModel to reload after Plaid reports success.

---

## Shared Layer (`shared/src/commonMain`)

### New: `LinkTokenDto`
`data/network/dto/LinkTokenDto.kt`

```kotlin
@Serializable
data class LinkTokenResponse(
    @SerialName("link_token") val linkToken: String,
)
```

### New: `PlaidEventBus`
`domain/plaid/PlaidEventBus.kt`

A Koin singleton. Holds a `MutableSharedFlow<Unit>`. `HomeViewModel` collects from it and calls `load()` on each emission (i.e., after a successful account link).

```kotlin
class PlaidEventBus {
    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()
    suspend fun accountLinked() = _events.emit(Unit)
}
```

### Modified: `AccountService`
Add `createLinkToken()`:
```kotlin
suspend fun createLinkToken(): LinkTokenResponse =
    apiClient.http.post("${apiClient.baseUrl}/link/token/create").body()
```

### Modified: `AccountRepository` (interface)
Add:
```kotlin
suspend fun createLinkToken(): String
```

### Modified: `AccountRepositoryImpl`
Implement `createLinkToken()`: call `service.createLinkToken()`, return `response.linkToken`. Wrap in try/catch and throw on error.

### New: `CreateLinkToken` use case
`domain/usecase/AccountUseCase.kt`

```kotlin
class CreateLinkToken(private val repo: AccountRepository) {
    suspend operator fun invoke(): String = repo.createLinkToken()
}
```

Add to `AccountUseCase`:
```kotlin
class AccountUseCase(
    val GetAccounts: GetAccounts,
    val CreateLinkToken: CreateLinkToken,
)
```

### Modified: `HomeScreenState`
Add `isAddingAccount` to `Loaded`:
```kotlin
data class Loaded(
    val dashboards: List<Dashboard>,
    val accounts: List<Account>,
    val isAddingAccount: Boolean = false,
)
```

### Modified: `HomeViewModel`
- Add constructor params: `CreateLinkToken` use case, `PlaidEventBus`
- Add `private val _plaidLinkTokenEvent = MutableSharedFlow<String>()`
- Expose `val plaidLinkTokenEvent: SharedFlow<String> = _plaidLinkTokenEvent.asSharedFlow()`
- In `init`, collect `plaidEventBus.events` and call `load()` on each emission
- Add `requestAddAccount()`:
  - Set `isAddingAccount = true` on current Loaded state (no-op if not loaded)
  - Call `accountUseCase.CreateLinkToken()`
  - On success: emit the token to `_plaidLinkTokenEvent`; reset `isAddingAccount = false`
  - On error: set `HomeScreenState.Error(message)`

### Modified: `HomeScreen`
- Add `onPlaidTokenReady: (String) -> Unit` parameter
- Add `LaunchedEffect(Unit)` that collects `viewModel.plaidLinkTokenEvent` and calls `onPlaidTokenReady(token)`
- Add "Add Account" button inline with the "Accounts" section header:
  - `Row` with `Text("Accounts")` at `weight(1f)` and an `IconButton(onClick = { viewModel.requestAddAccount() })` with `Icons.Default.Add`
  - Disable the button and show a small `CircularProgressIndicator` when `state.isAddingAccount`
- Error state for `requestAddAccount` failure: surface via the existing `HomeScreenState.Error` path (ViewModel already handles this)

### Modified: `AppNavigation`
- Add `onPlaidTokenReady: (String) -> Unit` parameter
- Pass it to `HomeScreen` in the `AppRoute.Home` entry

### Modified: `App`
- Add `onPlaidTokenReady: (String) -> Unit = {}` parameter
- Pass it to `AppNavigation`

---

## DI Layer (`shared/src/commonMain/di`)

### Modified: `RepositoryModule`
Add:
```kotlin
single { PlaidEventBus() }
```

### Modified: `UseCaseModule`
Add:
```kotlin
singleOf(::CreateLinkToken)
```

Update `AccountUseCase` registration so Koin injects both `GetAccounts` and `CreateLinkToken`.

### `ViewModelModule` (no change required)
`HomeViewModel` is registered via `viewModelOf(::HomeViewModel)` which auto-resolves all constructor params. Adding `CreateLinkToken` and `PlaidEventBus` to the constructor requires no manual DI change.

---

## Android Layer (`androidApp`)

### Modified: `MainActivity`

**Plaid result launcher** — registered in `onCreate` before `setContent`:
```kotlin
private val linkLauncher = registerForActivityResult(PlaidActivityResultContract()) { result ->
    when (result) {
        is LinkSuccess -> lifecycleScope.launch { plaidEventBus.accountLinked() }
        is LinkExit -> { /* no-op */ }
        is LinkError -> { /* log error */ }
    }
}
```

**PlaidEventBus injection** — via Koin's `by inject<PlaidEventBus>()`.

**`onPlaidTokenReady` lambda** passed to `App`:
```kotlin
onPlaidTokenReady = { linkToken ->
    val config = LinkTokenConfiguration.Builder().token(linkToken).build()
    Plaid.create(application, config).submit(linkLauncher)
}
```

No new files in `androidApp`. Plaid SDK (`com.plaid.link:sdk-core:5.5.2`) already declared.

---

## Data Flow

```
User taps "Add Account"
  → HomeScreen calls viewModel.requestAddAccount()
  → HomeViewModel: isAddingAccount = true, POST /link/token/create
  → On success: emit linkToken to plaidLinkTokenEvent
  → HomeScreen LaunchedEffect: onPlaidTokenReady(token)
  → MainActivity lambda: Plaid.create(config).submit(linkLauncher)
  → Plaid Link UI launches
  → User completes flow
  → PlaidActivityResultContract → LinkSuccess
  → plaidEventBus.accountLinked()
  → HomeViewModel collects event → load()
  → Accounts list refreshes
```

---

## Error Handling

| Scenario | Behavior |
|---|---|
| `/link/token/create` network error | ViewModel transitions to `HomeScreenState.Error` with message |
| Plaid Link exits (user cancelled) | No-op; button is already re-enabled (token fetch completed before Plaid launched) |
| Plaid Link error | Log; optionally show snackbar (out of scope for initial cut) |

---

## Out of Scope (initial cut)

- iOS Plaid integration (requires react-native bridge or separate native SDK)
- Desktop support
- Snackbar/toast feedback on Plaid link errors
- Account de-linking
