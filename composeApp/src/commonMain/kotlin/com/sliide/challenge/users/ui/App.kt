package com.sliide.challenge.users.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.sliide.challenge.users.ui.feed.UserFeedRoute
import com.sliide.challenge.users.ui.theme.AppTheme

/** Root of the 100% shared UI. */
@Composable
fun App() {
    AppTheme {
        Surface {
            UserFeedRoute()
        }
    }
}
