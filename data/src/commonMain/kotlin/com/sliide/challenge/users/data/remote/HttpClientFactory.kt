package com.sliide.challenge.users.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Platform HTTP engine: OkHttp on Android, Darwin (NSURLSession) on iOS. */
internal expect fun httpClientEngine(): HttpClientEngine

/** True for platform-specific connectivity failures that common code can't name. */
internal expect fun isPlatformNetworkException(t: Throwable): Boolean

internal fun createHttpClient(engine: HttpClientEngine = httpClientEngine()): HttpClient =
    HttpClient(engine) {
        // Status codes are business signals here (201/204/401/422) — we map
        // them explicitly in GoRestApi rather than throwing generically.
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    coerceInputValues = true
                }
            )
        }
        // Timeouts are configured on the platform engines (see httpClientEngine
        // actuals), NOT via the HttpTimeout plugin: the plugin's delay-based
        // watchdog interacts badly with kotlinx-coroutines virtual time in
        // runTest, instantly timing out MockEngine-backed tests.
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            url(ApiConfig.BASE_URL + "/")
            contentType(ContentType.Application.Json)
            if (ApiConfig.API_TOKEN.isNotBlank()) {
                headers.append("Authorization", "Bearer ${ApiConfig.API_TOKEN}")
            }
        }
    }
