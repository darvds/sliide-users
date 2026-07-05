package com.sliide.challenge.users.domain.error

/**
 * Typed failure model. The data layer maps transport/persistence exceptions
 * into these; the UI maps them to user-facing copy. No layer above data ever
 * sees a raw exception.
 */
sealed interface AppError {

    /** No connectivity / DNS failure / socket drop — retryable, offline UI. */
    data object Network : AppError

    /** The request reached the server but was rejected. */
    data class Http(val code: Int, val serverMessage: String? = null) : AppError {
        val isAuth: Boolean get() = code == 401 || code == 403
        val isRateLimit: Boolean get() = code == 429
    }

    /** Server-side field validation (GoRest 422), keyed by field name. */
    data class Validation(val fieldErrors: Map<String, String>) : AppError

    /** Anything unexpected — kept for diagnostics, shown generically. */
    data class Unknown(val debugMessage: String? = null) : AppError
}
