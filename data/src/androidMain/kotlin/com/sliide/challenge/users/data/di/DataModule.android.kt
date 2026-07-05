package com.sliide.challenge.users.data.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.sliide.challenge.users.data.local.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val context = androidContext()
        Room.databaseBuilder<AppDatabase>(
            context = context,
            name = context.getDatabasePath(AppDatabase.NAME).absolutePath,
        )
    }
}
