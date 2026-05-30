package com.daytimegaming.ourdailyfinances.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkTokenResponse(
    @SerialName("link_token") val linkToken: String,
)
