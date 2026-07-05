package com.sliide.challenge.users.domain.model

/** Payload for creating a user. Assumed already validated — see [com.sliide.challenge.users.domain.validation]. */
data class NewUser(
    val name: String,
    val email: String,
    val gender: Gender = Gender.FEMALE,
    val status: UserStatus = UserStatus.ACTIVE,
)
