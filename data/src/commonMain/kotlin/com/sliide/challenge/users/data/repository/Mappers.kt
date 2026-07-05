package com.sliide.challenge.users.data.repository

import com.sliide.challenge.users.data.local.UserEntity
import com.sliide.challenge.users.data.remote.CreateUserRequest
import com.sliide.challenge.users.data.remote.UserDto
import com.sliide.challenge.users.domain.model.Gender
import com.sliide.challenge.users.domain.model.NewUser
import com.sliide.challenge.users.domain.model.User
import com.sliide.challenge.users.domain.model.UserStatus

internal fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    email = email,
    gender = if (gender.equals("male", ignoreCase = true)) Gender.MALE else Gender.FEMALE,
    status = if (status.equals("active", ignoreCase = true)) UserStatus.ACTIVE else UserStatus.INACTIVE,
    firstSeenAtEpochMillis = firstSeenAtEpochMillis,
)

internal fun UserDto.toEntity(firstSeenAtEpochMillis: Long, createdLocally: Boolean) = UserEntity(
    id = id,
    name = name,
    email = email,
    gender = gender,
    status = status,
    firstSeenAtEpochMillis = firstSeenAtEpochMillis,
    createdLocally = createdLocally,
)

internal fun NewUser.toRequest() = CreateUserRequest(
    name = name,
    email = email,
    gender = if (gender == Gender.MALE) "male" else "female",
    status = if (status == UserStatus.ACTIVE) "active" else "inactive",
)
