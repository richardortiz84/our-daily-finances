package com.daytimegaming.ourdailyfinances.data.network

import com.daytimegaming.ourdailyfinances.BuildKonfig
import com.daytimegaming.ourdailyfinances.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiClient(
    engine: HttpClientEngine,
    private val authRepository: AuthRepository,
) {
    val baseUrl: String = BuildKonfig.API_URL

    val http: HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(authRepository.getIdToken(), "")
                }
                refreshTokens {
                    BearerTokens(authRepository.getIdToken(forceRefresh = true), "")
                }
            }
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
    }
}
