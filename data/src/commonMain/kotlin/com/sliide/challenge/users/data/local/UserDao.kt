package com.sliide.challenge.users.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface UserDao {

    /** Newest first; id breaks ties within one sync batch. */
    @Query("SELECT * FROM users ORDER BY firstSeenAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserEntity>

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM users WHERE createdLocally = 0 AND id NOT IN (:keepIds)")
    suspend fun deleteRemoteUsersNotIn(keepIds: List<Long>)

    /**
     * Reconciles the cache with a fresh snapshot of the feed:
     * remote rows not in the snapshot go away, locally-created rows persist.
     */
    @Transaction
    suspend fun reconcile(snapshot: List<UserEntity>) {
        deleteRemoteUsersNotIn(snapshot.map { it.id })
        upsertAll(snapshot)
    }
}
