# Account Detail — Real Transactions Design Spec

**Date:** 2026-05-31
**Status:** Approved

## Overview

Replace mock transaction data in `AccountDetailScreen` with real data from `GET /plaid/transactions`. A new `TransactionRepository` singleton caches the full transaction list in a `StateFlow`; `AccountDetailViewModel` filters it by `accountId`. The screen UI field mappings and amount sign convention are updated to match the real API.

---

## Section 1: Data Layer

### Domain model

**`Transaction`** (`domain/model/Transaction.kt`):
```kotlin
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

### DTOs

**`TransactionDto` + `TransactionsResponse`** (`data/network/dto/TransactionDto.kt`):
```kotlin
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

### `AccountService`

Add:
```kotlin
suspend fun getTransactions(): TransactionsResponse =
    apiClient.http.get("${apiClient.baseUrl}/plaid/transactions").body()
```

### `TransactionRepository` interface

New interface (`domain/repository/TransactionRepository.kt`):
```kotlin
interface TransactionRepository {
    val transactions: StateFlow<Response<List<Transaction>>>
    suspend fun refresh()
}
```

`transactions` is the in-memory cache. `refresh()` always fetches from the API and overwrites the cache.

### `TransactionRepositoryImpl`

New class (`data/repository/TransactionRepositoryImpl.kt`):
```kotlin
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

### `RepositoryModule`

Add registration:
```kotlin
single<TransactionRepository> { TransactionRepositoryImpl(get()) }
```

---

## Section 2: `AccountDetailViewModel` and State

### `AccountDetailScreenState`

Change `List<MockTransaction>` → `List<Transaction>` in `Loaded`. `MockTransaction` is no longer referenced here.

```kotlin
sealed class AccountDetailScreenState {
    data object Loading : AccountDetailScreenState()
    data class Error(val message: String) : AccountDetailScreenState()
    data class Loaded(
        val account: Account,
        val transactions: List<Transaction>,
    ) : AccountDetailScreenState()
}
```

### `AccountDetailViewModel`

Add `TransactionRepository` as a constructor parameter. In `init`, call `transactionRepository.refresh()` to populate the cache (no-op benefit: if another screen has already populated it this session, refresh still fetches fresh data for the detail view). Then `combine()` the two flows:

```kotlin
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

### `ViewModelModule` (Android, iOS, JVM)

Update `AccountDetailViewModel` registration to inject `TransactionRepository`:

```kotlin
viewModel { params -> AccountDetailViewModel(params.get(), get(), get()) }
```

The first `get()` resolves `AccountUseCase`; the second resolves `TransactionRepository`.

---

## Section 3: `AccountDetailScreen` UI Updates

Three field-mapping changes in the transaction row composable:

| Field | Old (mock) | New (real) |
|---|---|---|
| Merchant name | `tx.merchant` | `tx.merchantName ?: tx.name` |
| Category | `tx.category` | `tx.category.firstOrNull() ?: ""` |
| Pending flag | `tx.isPending` | `tx.pending` |

**Amount sign fix** — Plaid API: positive = debit (money out), negative = credit (money in). Inverted from mock convention:

```kotlin
val formattedAmount = if (tx.amount > 0) {
    "-$currencySymbol${tx.amount.formatAmount()}"
} else {
    "+$currencySymbol${kotlin.math.abs(tx.amount).formatAmount()}"
}
val color = if (tx.amount < 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
```

Remove `MockTransaction` import. No structural changes to the scaffold, hero card, or list layout.

---

## Data Flow Summary

```
AccountDetailScreen opened
        ↓
AccountDetailViewModel.init
        ↓
transactionRepository.refresh()           accountUseCase.GetAccounts()
GET /plaid/transactions                   GET /plaid/balance
        ↓                                         ↓
_transactions StateFlow updated           Flow<Response<List<Account>>>
        ↓
combine(GetAccounts flow, transactions StateFlow)
        ↓
filter transactions where accountId matches
        ↓
AccountDetailScreenState.Loaded(account, filteredTransactions)
        ↓
AccountDetailScreen renders hero card + transaction list
```

---

## Files Changed

| File | Action |
|---|---|
| `domain/model/Transaction.kt` | New |
| `data/network/dto/TransactionDto.kt` | New |
| `data/network/service/AccountService.kt` | Add `getTransactions()` |
| `domain/repository/TransactionRepository.kt` | New |
| `data/repository/TransactionRepositoryImpl.kt` | New |
| `di/RepositoryModule.kt` | Register `TransactionRepository` |
| `presentation/account/AccountDetailViewModel.kt` | Inject `TransactionRepository`, real combine logic |
| `di/ViewModelModule.android.kt` | Update `AccountDetailViewModel` registration |
| `di/ViewModelModule.ios.kt` | Update `AccountDetailViewModel` registration |
| `di/ViewModelModule.jvm.kt` | Update `AccountDetailViewModel` registration |
| `presentation/account/AccountDetailScreen.kt` | Field mappings + amount sign fix |
