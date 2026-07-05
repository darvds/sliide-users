package com.sliide.challenge.users.domain.usecase

import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.repository.UserRepository

class DeleteUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: Long): AppResult<Unit> =
        repository.deleteUser(userId)
}
