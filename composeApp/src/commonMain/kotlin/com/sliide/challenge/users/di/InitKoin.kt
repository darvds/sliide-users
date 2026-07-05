package com.sliide.challenge.users.di

import com.sliide.challenge.users.data.di.dataModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Single Koin entry point shared by both platforms.
 * Platform-specific configuration (e.g. androidContext) is passed via [config].
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModules())
    }
}

internal fun appModules(): List<Module> = listOf(
    dataModule,
    presentationModule,
)
