package com.sliide.challenge.users.domain.usecase

import com.sliide.challenge.users.domain.error.AppError
import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.error.asSuccess
import com.sliide.challenge.users.domain.model.Gender
import com.sliide.challenge.users.domain.model.NewUser
import com.sliide.challenge.users.domain.model.User
import com.sliide.challenge.users.domain.model.UserStatus
import com.sliide.challenge.users.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateUserUseCaseTest {

    private class RecordingRepository : UserRepository {
        var received: NewUser? = null
        override fun observeUsers(): Flow<List<User>> = emptyFlow()
        override suspend fun refreshUsers(): AppResult<Unit> = Unit.asSuccess()
        override suspend fun deleteUser(userId: Long): AppResult<Unit> = Unit.asSuccess()
        override suspend fun createUser(newUser: NewUser): AppResult<User> {
            received = newUser
            return User(
                id = 1, name = newUser.name, email = newUser.email,
                gender = newUser.gender, status = newUser.status,
                firstSeenAtEpochMillis = 0,
            ).asSuccess()
        }
    }

    @Test
    fun `invalid name and email are both reported, repository never called`() = runTest {
        val repo = RecordingRepository()
        val result = CreateUserUseCase(repo)(name = "", email = "not-an-email")

        val failure = assertIs<AppResult.Failure>(result)
        val error = assertIs<AppError.Validation>(failure.error)
        assertEquals(setOf("name", "email"), error.fieldErrors.keys)
        assertNull(repo.received, "repository must not be hit with invalid input")
    }

    @Test
    fun `valid input is trimmed and forwarded`() = runTest {
        val repo = RecordingRepository()
        val result = CreateUserUseCase(repo)(
            name = "  Ada Lovelace ",
            email = " ada@example.com ",
            gender = Gender.FEMALE,
            status = UserStatus.ACTIVE,
        )

        assertTrue(result is AppResult.Success)
        assertEquals("Ada Lovelace", repo.received?.name)
        assertEquals("ada@example.com", repo.received?.email)
    }

    @Test
    fun `repository failure is passed through untouched`() = runTest {
        val repo = object : UserRepository by RecordingRepository() {
            override suspend fun createUser(newUser: NewUser): AppResult<User> =
                AppResult.Failure(AppError.Http(422, "email already taken"))
        }
        val result = CreateUserUseCase(repo)(name = "Ada", email = "ada@example.com")

        val failure = assertIs<AppResult.Failure>(result)
        val http = assertIs<AppError.Http>(failure.error)
        assertEquals(422, http.code)
    }
}
