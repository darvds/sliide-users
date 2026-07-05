package com.sliide.challenge.users.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sliide.challenge.users.domain.model.Gender
import com.sliide.challenge.users.domain.model.UserStatus
import com.sliide.challenge.users.presentation.feed.UserItemUi
import com.sliide.challenge.users.ui.components.UserAvatar

/** Shared detail content: side pane on wide layouts, bottom sheet on compact. */
@Composable
private fun UserDetailContent(user: UserItemUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        UserAvatar(name = user.name, initials = user.initials, size = 96.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            user.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            user.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = {},
                label = { Text(if (user.gender == Gender.MALE) "Male" else "Female") },
            )
            AssistChip(
                onClick = {},
                label = { Text(if (user.status == UserStatus.ACTIVE) "Active" else "Inactive") },
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Added ${user.relativeTime}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Right pane of the two-pane (landscape/tablet) layout. */
@Composable
internal fun UserDetailPane(
    user: UserItemUi?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxSize().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        if (user == null) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Select a user",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Details appear here. Long-press a row to delete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            UserDetailContent(user, Modifier.fillMaxSize())
        }
    }
}

/** Compact-mode detail: same content, presented as a dismissible sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserDetailSheet(
    user: UserItemUi,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        UserDetailContent(
            user,
            Modifier.fillMaxWidth().navigationBarsPadding(),
        )
    }
}
