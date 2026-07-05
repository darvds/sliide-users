package com.sliide.challenge.users.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.TimeUnit

internal actual fun httpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
    }
}

internal actual fun isPlatformNetworkException(t: Throwable): Boolean =
    // DNS resolution failures surface as UnresolvedAddressException, which is
    // an IllegalArgumentException — NOT an IOException — so the common catch
    // misses it. Everything else network-ish on JVM is an IOException.
    t is UnresolvedAddressException
