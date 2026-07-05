package com.sliide.challenge.users

import android.app.Application
import com.sliide.challenge.users.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class SliideUsersApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@SliideUsersApplication)
        }
    }
}
