package com.sliide.challenge.users.di

import org.koin.core.context.startKoin
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

/** Populated as layers land: data module, presentation module. */
internal fun appModules() = emptyList<org.koin.core.module.Module>()
