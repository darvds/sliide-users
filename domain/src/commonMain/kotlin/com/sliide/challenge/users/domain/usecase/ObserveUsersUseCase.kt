package com.sliide.challenge.users.domain.usecase

import com.sliide.challenge.users.domain.model.User
import com.sliide.challenge.users.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class ObserveUsersUseCase(private val repository: UserRepository) {
    operator fun invoke(): Flow<List<User>> = repository.observeUsers()
}
