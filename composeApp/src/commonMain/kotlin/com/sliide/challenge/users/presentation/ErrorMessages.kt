package com.sliide.challenge.users.presentation

import com.sliide.challenge.users.domain.error.AppError

/** Single place where typed errors become user-facing copy. */
fun AppError.toUserMessage(): String = when (this) {
    AppError.Network ->
        "No internet connection"

    is AppError.Http -> when {
        isAuth -> "GoRest API token missing or invalid — see README setup"
        isRateLimit -> "GoRest rate limit hit — try again in a moment"
        else -> serverMessage?.replaceFirstChar { it.uppercase() } ?: "Server error ($code)"
    }

    is AppError.Validation ->
        fieldErrors.entries.joinToString("; ") { (field, message) ->
            "${field.replaceFirstChar { it.uppercase() }} $message"
        }

    is AppError.Unknown ->
        "Something went wrong — please try again"
}
