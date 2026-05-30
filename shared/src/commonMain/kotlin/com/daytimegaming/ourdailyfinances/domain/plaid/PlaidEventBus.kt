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
