package com.sliide.challenge.users.ui.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sliide.challenge.users.presentation.feed.UserFeedEffect
import com.sliide.challenge.users.presentation.feed.UserFeedIntent
import com.sliide.challenge.users.presentation.feed.UserFeedState
import com.sliide.challenge.users.presentation.feed.UserFeedViewModel
import com.sliide.challenge.users.ui.components.EmptyState
import com.sliide.challenge.users.ui.components.ErrorState
import com.sliide.challenge.users.ui.components.ShimmerFeed
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Width at which the layout graduates to master-detail. */
private val TwoPaneBreakpoint = 700.dp

@Composable
internal fun UserFeedRoute(viewModel: UserFeedViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UserFeedEffect.ShowMessage -> scope.launch {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is UserFeedEffect.ShowUndoDelete -> scope.launch {
                    // Undo must be visible for its whole window — evict
                    // whatever else is showing.
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "${effect.userName} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short, // 4s — matches the VM window
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(UserFeedIntent.UndoDelete(effect.userId))
                    }
                }
            }
        }
    }

    UserFeedScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserFeedScreen(
    state: UserFeedState,
    snackbarHostState: SnackbarHostState,
    onIntent: (UserFeedIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sliide Users") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onIntent(UserFeedIntent.OpenAddUser) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add user") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AnimatedContent(
            targetState = state.contentPhase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { phase ->
            when (phase) {
                ContentPhase.LOADING -> ShimmerFeed()

                ContentPhase.ERROR -> ErrorState(
                    message = state.fullScreenError.orEmpty(),
                    onRetry = { onIntent(UserFeedIntent.Retry) },
                )

                ContentPhase.EMPTY -> EmptyState(
                    onAddUser = { onIntent(UserFeedIntent.OpenAddUser) },
                )

                ContentPhase.CONTENT -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onIntent(UserFeedIntent.PullToRefresh) },
                ) {
                    AdaptiveFeed(state, onIntent)
                }
            }
        }
    }

    state.addForm?.let { AddUserSheet(form = it, onIntent = onIntent) }

    state.deleteConfirm?.let { user ->
        DeleteConfirmDialog(
            user = user,
            onConfirm = { onIntent(UserFeedIntent.ConfirmDelete) },
            onDismiss = { onIntent(UserFeedIntent.DismissDeleteConfirm) },
        )
    }
}

// -- phases -------------------------------------------------------------

internal enum class ContentPhase { LOADING, ERROR, EMPTY, CONTENT }

internal val UserFeedState.contentPhase: ContentPhase
    get() = when {
        isInitialLoading -> ContentPhase.LOADING
        fullScreenError != null -> ContentPhase.ERROR
        users.isEmpty() -> ContentPhase.EMPTY
        else -> ContentPhase.CONTENT
    }

// -- adaptive content -----------------------------------------------------

/**
 * Portrait/compact: single list. Landscape/tablet (>= 700dp): master-detail.
 * BoxWithConstraints keeps this decision local and dependency-free.
 */
@Composable
private fun AdaptiveFeed(
    state: UserFeedState,
    onIntent: (UserFeedIntent) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= TwoPaneBreakpoint
        if (twoPane) {
            Row(Modifier.fillMaxSize()) {
                UserList(
                    state = state,
                    onIntent = onIntent,
                    highlightSelection = true,
                    modifier = Modifier.weight(0.42f),
                )
                UserDetailPane(
                    user = state.selectedUser,
                    modifier = Modifier.weight(0.58f),
                )
            }
        } else {
            UserList(
                state = state,
                onIntent = onIntent,
                highlightSelection = false,
                modifier = Modifier.fillMaxSize(),
            )
            // Compact mode: tapping a user opens detail as a sheet, so no
            // interaction is ever a dead end on any form factor.
            state.selectedUser?.let { user ->
                UserDetailSheet(
                    user = user,
                    onDismiss = { onIntent(UserFeedIntent.ClearSelection) },
                )
            }
        }
    }
}

@Composable
private fun UserList(
    state: UserFeedState,
    onIntent: (UserFeedIntent) -> Unit,
    highlightSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (state.isStale) StaleBanner()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(state.users, key = { it.id }) { user ->
                UserListItem(
                    user = user,
                    selected = highlightSelection && user.id == state.selectedUserId,
                    onClick = { onIntent(UserFeedIntent.UserSelected(user.id)) },
                    onLongClick = { onIntent(UserFeedIntent.UserLongPressed(user.id)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun StaleBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            "Offline — showing cached users",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}
