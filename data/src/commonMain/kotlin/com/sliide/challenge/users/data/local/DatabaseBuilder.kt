package com.sliide.challenge.users.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Finishes a platform-provided builder (see platformDataModule in DI):
 * Android needs a Context for the db path; iOS uses the documents directory.
 * The bundled SQLite driver gives both platforms the same, current SQLite.
 */
internal fun createDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
