package com.daytimegaming.ourdailyfinances.domain.plaid

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PlaidItemsEventBus {
    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    suspend fun itemsChanged() = _events.emit(Unit)
}
