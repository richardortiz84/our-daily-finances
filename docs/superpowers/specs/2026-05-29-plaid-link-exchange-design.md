# Plaid Link Exchange — Design Spec

**Date:** 2026-05-29  
**Status:** Approved

## Overview

After the user completes the Plaid Link SDK flow, the app must call `POST /plaid/link/exchange` with the `public_token` and `institution_name` returned by the SDK to finalize account linking on the backend. During the exchange, a non-dismissable loading dialog is shown. On success the account list reloads; on failure the screen transitions to the error state.

---

## Section 1: Data Layer

### New DTOs

**`LinkExchangeRequest`** (`data/network/dto/`)
```kotlin
@Serializable
data class LinkExchangeRequest(
    @SerialName("public_token") val publicToken: String,
    @SerialName("institution_name") val institutionName: String? = null,
)
```

**`LinkExchangeResponse`** (`data/network/dto/`)
```kotlin
@Serializable
data class LinkExchangeResponse(
    @SerialName("item_id") val itemId: String,
    @SerialName("institution_name") val institutionName: String,
)
```

### `AccountService`

Add:
```kotlin
suspend fun exchangePublicToken(publicToken: String, institutionName: String?): LinkExchangeResponse =
    apiClient.http.post("${apiClient.baseUrl}/plaid/link/exchange") {
        contentType(ContentType.Application.Json)
        setBody(LinkExchangeRequest(publicToken, institutionName))
    }.body()
```

### `AccountRepository` interface

Add:
```kotlin
suspend fun exchangePublicToken(publicToken: String, institutionName: String?)
```

Return value is not exposed to the domain — success/failure is all that matters.

### `AccountRepositoryImpl`

Add:
```kotlin
override suspend fun exchangePublicToken(publicToken: String, institutionName: String?) {
    service.exchangePublicToken(publicToken, institutionName)
}
```

### `ExchangePublicToken` use case

New use case in `domain/usecase/AccountUseCase.kt`:
```kotlin
class ExchangePublicToken(private val repo: AccountRepository) {
    suspend operator fun invoke(publicToken: String, institutionName: String?) =
        repo.exchangePublicToken(publicToken, institutionName)
}
```

Added to `AccountUseCase` alongside `GetAccounts` and `CreateLinkToken`. Wired in `UseCaseModule`.

---

## Section 2: `PlaidEventBus` Evolution

### New domain model

New `PlaidLinkResult` data class in `domain/plaid/`:
```kotlin
data class PlaidLinkResult(
    val publicToken: String,
    val institutionName: String?,
)
```

### Updated `PlaidEventBus`

Change event type from `Unit` to `PlaidLinkResult`:
```kotlin
class PlaidEventBus {
    private val _events = MutableSharedFlow<PlaidLinkResult>()
    val events: SharedFlow<PlaidLinkResult> = _events.asSharedFlow()

    suspend fun accountLinked(publicToken: String, institutionName: String?) =
        _events.emit(PlaidLinkResult(publicToken, institutionName))
}
```

### `MainActivity`

Update `LinkSuccess` handler to pass token data:
```kotlin
is LinkSuccess -> lifecycleScope.launch {
    plaidEventBus.accountLinked(
        publicToken = result.publicToken,
        institutionName = result.metadata.institution?.name,
    )
}
```

---

## Section 3: `HomeViewModel` State and Logic

### `HomeScreenState.Loaded`

Add `isLinkingAccount: Boolean = false`:
```kotlin
data class Loaded(
    val dashboards: List<Dashboard>,
    val accounts: List<Account>,
    val isAddingAccount: Boolean = false,
    val isLinkingAccount: Boolean = false,
)
```

### `HomeViewModel.init`

Replace the existing `plaidEventBus.events.collect { load() }` with:
```kotlin
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
```

`isLinkingAccount` does not need explicit reset — `load()` replaces the entire state with a fresh `Loaded` instance (defaulting to `false`).

### Error handling

Exchange failure transitions to `HomeScreenState.Error`, consistent with the existing pattern in `requestAddAccount`.

---

## Section 4: UI

### Loading dialog

Added to `HomeScreen` inside the `Loaded` branch:
```kotlin
if (loadedState.isLinkingAccount) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Linking your account...") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
    )
}
```

- `onDismissRequest = {}` makes the dialog non-dismissable (back button and outside taps ignored)
- No confirm button — dialog is purely informational during the async operation
- Dialog disappears automatically when `load()` replaces state after a successful exchange

---

## Data Flow Summary

```
User completes Plaid Link SDK
        ↓
MainActivity: LinkSuccess
        ↓
plaidEventBus.accountLinked(publicToken, institutionName)
        ↓
HomeViewModel collects PlaidLinkResult
        ↓
state → isLinkingAccount = true  →  Dialog shown
        ↓
accountUseCase.ExchangePublicToken(publicToken, institutionName)
        ↓
POST /plaid/link/exchange
        ↓ success              ↓ failure
     load()             HomeScreenState.Error
        ↓
state replaced (isLinkingAccount defaults false)
Dialog dismissed, accounts refreshed
```

---

## Files Changed

| File | Change |
|---|---|
| `data/network/dto/LinkExchangeRequest.kt` | New |
| `data/network/dto/LinkExchangeResponse.kt` | New |
| `data/network/service/AccountService.kt` | Add `exchangePublicToken()` |
| `domain/repository/AccountRepository.kt` | Add `exchangePublicToken()` |
| `data/repository/AccountRepositoryImpl.kt` | Implement `exchangePublicToken()` |
| `domain/usecase/AccountUseCase.kt` | Add `ExchangePublicToken` use case |
| `di/UseCaseModule.kt` | Wire `ExchangePublicToken` |
| `domain/plaid/PlaidLinkResult.kt` | New |
| `domain/plaid/PlaidEventBus.kt` | Evolve to `SharedFlow<PlaidLinkResult>` |
| `androidApp/.../MainActivity.kt` | Pass token data to `accountLinked()` |
| `presentation/home/HomeScreenState.kt` | Add `isLinkingAccount` |
| `presentation/home/HomeViewModel.kt` | Drive exchange flow from event |
| `presentation/home/HomeScreen.kt` | Add loading dialog |
