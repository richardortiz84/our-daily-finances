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
