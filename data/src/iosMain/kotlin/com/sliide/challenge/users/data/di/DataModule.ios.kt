package com.sliide.challenge.users.data.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.sliide.challenge.users.data.local.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual val platformDataModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        Room.databaseBuilder<AppDatabase>(
            name = "${documentDirectory()}/${AppDatabase.NAME}",
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { "Unable to resolve documents directory" }
}
