package com.sliide.challenge.users.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GoRest /public/v2 user shape. Note: no timestamps — see README "API limitations". */
@Serializable
internal data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
)

@Serializable
internal data class CreateUserRequest(
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
)

/** GoRest 422 body: `[{"field": "email", "message": "has already been taken"}]` */
@Serializable
internal data class FieldErrorDto(
    val field: String,
    val message: String,
)

/** GoRest 401/404 body: `{"message": "Authentication failed"}` */
@Serializable
internal data class MessageDto(
    @SerialName("message") val message: String? = null,
)
