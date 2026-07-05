package com.sliide.challenge.users.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinHttpRequestException

internal actual fun httpClientEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        setTimeoutIntervalForRequest(30.0)
        setTimeoutIntervalForResource(60.0)
    }
}

internal actual fun isPlatformNetworkException(t: Throwable): Boolean =
    // NSURLSession failures (no connectivity, DNS, airplane mode) surface as
    // DarwinHttpRequestException wrapping an NSError.
    t is DarwinHttpRequestException
