package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkExchangeRequest(
    @SerialName("public_token") val publicToken: String,
    @SerialName("institution_name") val institutionName: String? = null,
)
