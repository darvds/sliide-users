package com.sliide.challenge.users.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached user row.
 *
 * @property firstSeenAtEpochMillis anchor for relative timestamps (GoRest has
 * none). Preserved across refreshes so times don't reset on every sync.
 * @property createdLocally true for users created from this device. These
 * survive feed reconciliation even when GoRest's last page (a moving window)
 * no longer contains them.
 */
@Entity(tableName = "users")
internal data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
    val firstSeenAtEpochMillis: Long,
    val createdLocally: Boolean,
)
