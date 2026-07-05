package com.sliide.challenge.users.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [UserEntity::class], version = 1, exportSchema = true)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "sliide_users.db"
    }
}

/** Room's KSP processor generates the per-platform actuals for this. */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
