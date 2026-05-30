package com.daytimegaming.ourdailyfinances.domain.plaid

data class PlaidLinkResult(
    val publicToken: String,
    val institutionName: String?,
)
