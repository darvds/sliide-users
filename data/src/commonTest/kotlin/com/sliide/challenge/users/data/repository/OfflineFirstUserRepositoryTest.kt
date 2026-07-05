package com.sliide.challenge.users.data.repository

import com.sliide.challenge.users.data.local.UserDao
import com.sliide.challenge.users.data.local.UserEntity
import com.sliide.challenge.users.data.remote.GoRestApi
import com.sliide.challenge.users.data.remote.createHttpClient
import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.model.NewUser
import com.sliide.challenge.users.domain.time.TimeProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Exercises the repository against a real GoRestApi over MockEngine and an
 * in-memory DAO — the full :data stack minus SQLite itself.
 */
class OfflineFirstUserRepositoryTest {

    private class InMemoryDao : UserDao {
        val rows = MutableStateFlow<Map<Long, UserEntity>>(emptyMap())

        override fun observeAll(): Flow<List<UserEntity>> = rows.map { m ->
            m.values.sortedWith(
                compareByDescending<UserEntity> { it.firstSeenAtEpochMillis }
                    .thenByDescending { it.id }
            )
        }

        override suspend fun getAll(): List<UserEntity> = rows.value.values.toList()

        override suspend fun upsertAll(users: List<UserEntity>) {
            rows.value = rows.value + users.associateBy { it.id }
        }

        override suspend fun deleteById(id: Long) {
            rows.value = rows.value - id
        }

        override suspend fun deleteRemoteUsersNotIn(keepIds: List<Long>) {
            rows.value = rows.value.filterValues { it.createdLocally || it.id in keepIds }
        }
    }

    private class FixedTime(var now: Long) : TimeProvider {
        override fun nowEpochMillis(): Long = now
    }

    private fun jsonHeaders(vararg extra: Pair<String, String>) = headersOf(
        *(listOf(HttpHeaders.ContentType to listOf("application/json")) +
            extra.map { it.first to listOf(it.second) }).toTypedArray()
    )

    private fun usersJson(vararg ids: Long) = ids.joinToString(prefix = "[", postfix = "]") {
        """{"id":$it,"name":"User $it","email":"u$it@example.com","gender":"female","status":"active"}"""
    }

    private fun entity(id: Long, firstSeen: Long, local: Boolean = false) = UserEntity(
        id = id, name = "User $id", email = "u$id@example.com",
        gender = "female", status = "active",
        firstSeenAtEpochMillis = firstSeen, createdLocally = local,
    )

    @Test
    fun `refresh preserves first-seen for known users and stamps now for new ones`() = runTest {
        val dao = InMemoryDao().apply { rows.value = mapOf(10L to entity(10, firstSeen = 1_000)) }
        val time = FixedTime(now = 9_000)
        val engine = MockEngine {
            respond(usersJson(10, 11), HttpStatusCode.OK, jsonHeaders("x-pagination-pages" to "1"))
        }
        val repo = OfflineFirstUserRepository(GoRestApi(createHttpClient(engine)), dao, time)

        assertIs<AppResult.Success<Unit>>(repo.refreshUsers())

        val byId = dao.rows.value
        assertEquals(1_000, byId.getValue(10).firstSeenAtEpochMillis, "known user keeps timestamp")
        assertEquals(9_000, byId.getValue(11).firstSeenAtEpochMillis, "new user stamped now")
    }

    @Test
    fun `refresh evicts departed remote users but keeps locally-created ones`() = runTest {
        val dao = InMemoryDao().apply {
            rows.value = mapOf(
                1L to entity(1, firstSeen = 100),               // remote, gone from feed
                999L to entity(999, firstSeen = 200, local = true), // created on device
            )
        }
        val engine = MockEngine {
            respond(usersJson(2), HttpStatusCode.OK, jsonHeaders("x-pagination-pages" to "1"))
        }
        val repo = OfflineFirstUserRepository(GoRestApi(createHttpClient(engine)), dao, FixedTime(500))

        assertIs<AppResult.Success<Unit>>(repo.refreshUsers())

        assertEquals(setOf(2L, 999L), dao.rows.value.keys)
    }

    @Test
    fun `created user lands at the top of the observed feed`() = runTest {
        val dao = InMemoryDao().apply { rows.value = mapOf(1L to entity(1, firstSeen = 100)) }
        val engine = MockEngine {
            respond(
                """{"id":50,"name":"Ada","email":"ada@example.com","gender":"female","status":"active"}""",
                HttpStatusCode.Created,
                jsonHeaders(),
            )
        }
        val repo = OfflineFirstUserRepository(GoRestApi(createHttpClient(engine)), dao, FixedTime(9_999))

        val created = assertIs<AppResult.Success<com.sliide.challenge.users.domain.model.User>>(
            repo.createUser(NewUser(name = "Ada", email = "ada@example.com"))
        )
        assertEquals(50, created.value.id)

        val feed = repo.observeUsers().first()
        assertEquals(50, feed.first().id, "created user must appear at the top")
        assertTrue(dao.rows.value.getValue(50).createdLocally)
    }

    @Test
    fun `delete removes the row only after the api confirms`() = runTest {
        val dao = InMemoryDao().apply { rows.value = mapOf(1L to entity(1, firstSeen = 100)) }
        var deleted = false
        val engine = MockEngine {
            deleted = true
            respond("", HttpStatusCode.NoContent, jsonHeaders())
        }
        val repo = OfflineFirstUserRepository(GoRestApi(createHttpClient(engine)), dao, FixedTime(0))

        assertIs<AppResult.Success<Unit>>(repo.deleteUser(1))
        assertTrue(deleted)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `failed delete leaves the cache untouched`() = runTest {
        val dao = InMemoryDao().apply { rows.value = mapOf(1L to entity(1, firstSeen = 100)) }
        val engine = MockEngine {
            respond("""{"message":"boom"}""", HttpStatusCode.InternalServerError, jsonHeaders())
        }
        val repo = OfflineFirstUserRepository(GoRestApi(createHttpClient(engine)), dao, FixedTime(0))

        assertIs<AppResult.Failure>(repo.deleteUser(1))
        assertEquals(setOf(1L), dao.rows.value.keys)
    }
}
