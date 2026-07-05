package com.sliide.challenge.users.domain.repository

import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.model.NewUser
import com.sliide.challenge.users.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for users, offline-first:
 * [observeUsers] always emits from the local cache; [refreshUsers] reconciles
 * the cache with the remote feed (last page of GoRest /users).
 */
interface UserRepository {

    /** Cache-backed stream, newest first. Emits on every local change. */
    fun observeUsers(): Flow<List<User>>

    /**
     * Fetches the last page of /users and reconciles the cache with it.
     * Locally-created users that GoRest already returns are de-duplicated
     * by id; first-seen timestamps of known users are preserved.
     */
    suspend fun refreshUsers(): AppResult<Unit>

    /** POSTs the user; on 201, inserts into the cache (top of the feed). */
    suspend fun createUser(newUser: NewUser): AppResult<User>

    /** DELETEs remotely; on 204 (or 404 — already gone), removes from cache. */
    suspend fun deleteUser(userId: Long): AppResult<Unit>
}
