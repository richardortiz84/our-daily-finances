package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkExchangeResponse(
    @SerialName("item_id") val itemId: String,
    @SerialName("institution_name") val institutionName: String,
)
