package com.sliide.challenge.users

import androidx.compose.ui.window.ComposeUIViewController
import com.sliide.challenge.users.di.initKoin
import com.sliide.challenge.users.ui.App
import platform.UIKit.UIViewController

/**
 * iOS entry point, called from SwiftUI via [MainViewControllerKt.MainViewController].
 * Koin is started lazily on first access so the Swift side needs no DI knowledge.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    ensureKoin()
    return ComposeUIViewController { App() }
}

private var koinStarted = false

private fun ensureKoin() {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
}
