package com.sliide.challenge.users.data.repository

import com.sliide.challenge.users.data.local.UserDao
import com.sliide.challenge.users.data.remote.GoRestApi
import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.error.map
import com.sliide.challenge.users.domain.error.onSuccess
import com.sliide.challenge.users.domain.model.NewUser
import com.sliide.challenge.users.domain.model.User
import com.sliide.challenge.users.domain.repository.UserRepository
import com.sliide.challenge.users.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first: Room is the single source of truth; the UI only ever renders
 * what [observeUsers] emits. Network calls mutate the cache, never the UI.
 *
 * Refresh reconciliation rules (see UserDao.reconcile):
 *  - first-seen timestamps of already-known users are preserved, so relative
 *    times stay stable across refreshes;
 *  - locally-created users survive even after GoRest's last page (a moving
 *    window) stops including them;
 *  - remote users that left the window are evicted.
 */
internal class OfflineFirstUserRepository(
    private val api: GoRestApi,
    private val dao: UserDao,
    private val timeProvider: TimeProvider,
) : UserRepository {

    override fun observeUsers(): Flow<List<User>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshUsers(): AppResult<Unit> =
        api.fetchLastPageUsers()
            .onSuccess { dtos ->
                val known = dao.getAll().associateBy { it.id }
                val now = timeProvider.nowEpochMillis()
                val snapshot = dtos.map { dto ->
                    val existing = known[dto.id]
                    dto.toEntity(
                        firstSeenAtEpochMillis = existing?.firstSeenAtEpochMillis ?: now,
                        createdLocally = existing?.createdLocally ?: false,
                    )
                }
                dao.reconcile(snapshot)
            }
            .map { }

    override suspend fun createUser(newUser: NewUser): AppResult<User> =
        api.createUser(newUser.toRequest()).map { dto ->
            val entity = dto.toEntity(
                firstSeenAtEpochMillis = timeProvider.nowEpochMillis(),
                createdLocally = true,
            )
            dao.upsertAll(listOf(entity))
            entity.toDomain()
        }

    override suspend fun deleteUser(userId: Long): AppResult<Unit> =
        api.deleteUser(userId).onSuccess {
            dao.deleteById(userId)
        }
}
