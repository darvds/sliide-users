package com.sliide.challenge.users.data.di

import com.sliide.challenge.users.data.local.AppDatabase
import com.sliide.challenge.users.data.local.UserDao
import com.sliide.challenge.users.data.local.createDatabase
import com.sliide.challenge.users.data.remote.GoRestApi
import com.sliide.challenge.users.data.remote.createHttpClient
import com.sliide.challenge.users.data.repository.OfflineFirstUserRepository
import com.sliide.challenge.users.data.time.currentEpochMillis
import com.sliide.challenge.users.domain.repository.UserRepository
import com.sliide.challenge.users.domain.time.TimeProvider
import org.koin.core.module.Module
import org.koin.dsl.module

/** Platform contributions: RoomDatabase.Builder (needs Context on Android). */
internal expect val platformDataModule: Module

/** Everything the :data layer exports to the graph. */
val dataModule: Module = module {
    includes(platformDataModule)

    single { createHttpClient() }
    single { GoRestApi(get()) }
    single<AppDatabase> { createDatabase(get()) }
    single<UserDao> { get<AppDatabase>().userDao() }
    single<TimeProvider> { TimeProvider { currentEpochMillis() } }
    single<UserRepository> { OfflineFirstUserRepository(get(), get(), get()) }
}
