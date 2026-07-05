package com.sliide.challenge.users.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sliide.challenge.users.domain.model.Gender
import com.sliide.challenge.users.domain.model.UserStatus
import com.sliide.challenge.users.presentation.feed.AddUserFormState
import com.sliide.challenge.users.presentation.feed.UserFeedIntent

/**
 * Add-user form in a modal sheet. Validation runs as the user types
 * (via NameChanged/EmailChanged intents); errors render inline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddUserSheet(
    form: AddUserFormState,
    onIntent: (UserFeedIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onIntent(UserFeedIntent.DismissAddUser) },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("Add user", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = form.name,
                onValueChange = { onIntent(UserFeedIntent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                isError = form.nameError != null,
                supportingText = { form.nameError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
                enabled = !form.isSubmitting,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = form.email,
                onValueChange = { onIntent(UserFeedIntent.EmailChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                isError = form.emailError != null,
                supportingText = { form.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                ),
                enabled = !form.isSubmitting,
            )
            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = form.gender == Gender.FEMALE,
                    onClick = { onIntent(UserFeedIntent.GenderChanged(Gender.FEMALE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    enabled = !form.isSubmitting,
                ) { Text("Female") }
                SegmentedButton(
                    selected = form.gender == Gender.MALE,
                    onClick = { onIntent(UserFeedIntent.GenderChanged(Gender.MALE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = !form.isSubmitting,
                ) { Text("Male") }
            }
            Spacer(Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = form.status == UserStatus.ACTIVE,
                    onClick = { onIntent(UserFeedIntent.StatusChanged(UserStatus.ACTIVE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    enabled = !form.isSubmitting,
                ) { Text("Active") }
                SegmentedButton(
                    selected = form.status == UserStatus.INACTIVE,
                    onClick = { onIntent(UserFeedIntent.StatusChanged(UserStatus.INACTIVE)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = !form.isSubmitting,
                ) { Text("Inactive") }
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onIntent(UserFeedIntent.SubmitNewUser) },
                modifier = Modifier.fillMaxWidth(),
                enabled = form.canSubmit,
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Create user")
                }
            }
            Spacer(Modifier.height(24.dp))

            Row {} // keeps the sheet from clipping the button's shadow
        }
    }
}
