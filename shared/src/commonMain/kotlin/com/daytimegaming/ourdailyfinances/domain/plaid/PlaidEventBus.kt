package com.daytimegaming.ourdailyfinances.domain.plaid

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PlaidEventBus {
    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    suspend fun accountLinked() = _events.emit(Unit)
}
