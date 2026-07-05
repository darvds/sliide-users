package com.sliide.challenge.users.domain.usecase

import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.repository.UserRepository

class RefreshUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshUsers()
}
