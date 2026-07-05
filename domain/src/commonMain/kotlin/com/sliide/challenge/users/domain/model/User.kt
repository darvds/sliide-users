package com.sliide.challenge.users.domain.model

/**
 * A user in the directory.
 *
 * @property firstSeenAtEpochMillis when this user first became known to the
 * app (locally created or first fetched). The GoRest v2 API exposes no
 * creation timestamp, so relative times are anchored to local first-sight.
 * See README "API limitations".
 */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val gender: Gender,
    val status: UserStatus,
    val firstSeenAtEpochMillis: Long,
)

enum class Gender { MALE, FEMALE }

enum class UserStatus { ACTIVE, INACTIVE }
