package com.sliide.challenge.users.data.remote

import com.sliide.challenge.users.domain.error.AppError
import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.error.asFailure
import com.sliide.challenge.users.domain.error.asSuccess
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * Thin GoRest client: HTTP + status mapping only. No caching, no domain
 * decisions — those live in the repository.
 */
internal class GoRestApi(private val client: HttpClient) {

    /**
     * The brief asks for the *last page* of /users. GoRest exposes total
     * pages via the `x-pagination-pages` response header, so this costs two
     * requests: one to learn the page count, one to fetch that page.
     */
    suspend fun fetchLastPageUsers(): AppResult<List<UserDto>> = safeCall {
        val first = client.get("users")
        first.requireOk()
        val lastPage = first.headers[HEADER_TOTAL_PAGES]?.toIntOrNull() ?: 1
        if (lastPage <= 1) {
            first.body()
        } else {
            val last = client.get("users") { url.parameters.append("page", lastPage.toString()) }
            last.requireOk()
            last.body()
        }
    }

    suspend fun createUser(request: CreateUserRequest): AppResult<UserDto> = safeCall {
        val response = client.post("users") { setBody(request) }
        when (response.status) {
            HttpStatusCode.Created -> response.body<UserDto>()
            HttpStatusCode.UnprocessableEntity -> throw GoRestException(
                AppError.Validation(
                    response.body<List<FieldErrorDto>>().associate { it.field to it.message }
                )
            )
            else -> throw response.toHttpException()
        }
    }

    suspend fun deleteUser(id: Long): AppResult<Unit> = safeCall {
        val response = client.delete("users/$id")
        when (response.status) {
            HttpStatusCode.NoContent -> Unit
            // Already gone server-side — the caller's intent is satisfied.
            HttpStatusCode.NotFound -> Unit
            else -> throw response.toHttpException()
        }
    }

    private suspend fun HttpResponse.requireOk() {
        if (status != HttpStatusCode.OK) throw toHttpException()
    }

    private suspend fun HttpResponse.toHttpException(): GoRestException {
        val serverMessage = runCatching { body<MessageDto>().message }.getOrNull()
        return GoRestException(AppError.Http(status.value, serverMessage))
    }

    private companion object {
        const val HEADER_TOTAL_PAGES = "x-pagination-pages"
    }
}

/** Carrier so [safeCall] can rethrow typed errors from response handling. */
internal class GoRestException(val error: AppError) : Exception()

private suspend fun <T> safeCall(block: suspend () -> T): AppResult<T> = try {
    block().asSuccess()
} catch (e: CancellationException) {
    throw e
} catch (e: GoRestException) {
    e.error.asFailure()
} catch (e: HttpRequestTimeoutException) {
    AppError.Network.asFailure()
} catch (e: ConnectTimeoutException) {
    AppError.Network.asFailure()
} catch (e: SocketTimeoutException) {
    AppError.Network.asFailure()
} catch (e: IOException) {
    AppError.Network.asFailure()
} catch (e: Exception) {
    if (isPlatformNetworkException(e)) AppError.Network.asFailure()
    else AppError.Unknown(e.message).asFailure()
}
