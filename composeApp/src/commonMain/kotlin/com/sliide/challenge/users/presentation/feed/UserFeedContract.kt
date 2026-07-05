package com.sliide.challenge.users.presentation.feed

import com.sliide.challenge.users.domain.model.Gender
import com.sliide.challenge.users.domain.model.UserStatus

// ---------------------------------------------------------------------------
// UI models
// ---------------------------------------------------------------------------

data class UserItemUi(
    val id: Long,
    val name: String,
    val email: String,
    val relativeTime: String,
    val initials: String,
    val gender: Gender,
    val status: UserStatus,
)

data class AddUserFormState(
    val name: String = "",
    val email: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val gender: Gender = Gender.FEMALE,
    val status: UserStatus = UserStatus.ACTIVE,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean =
        name.isNotBlank() && email.isNotBlank() &&
            nameError == null && emailError == null && !isSubmitting
}

// ---------------------------------------------------------------------------
// MVI contract
// ---------------------------------------------------------------------------

data class UserFeedState(
    val users: List<UserItemUi> = emptyList(),
    /** Shimmer: nothing cached yet and the first load is still in flight. */
    val isInitialLoading: Boolean = true,
    /** Pull-to-refresh spinner. */
    val isRefreshing: Boolean = false,
    /** Full-screen error — only when there is no cached content to show. */
    val fullScreenError: String? = null,
    /** Cached content is on screen but the last refresh failed (e.g. offline). */
    val isStale: Boolean = false,
    /** Non-null while the add-user sheet is open. */
    val addForm: AddUserFormState? = null,
    /** Non-null while the delete confirmation dialog is open. */
    val deleteConfirm: UserItemUi? = null,
    /** Selection for the master-detail (expanded/landscape) layout. */
    val selectedUserId: Long? = null,
) {
    val selectedUser: UserItemUi? get() = users.firstOrNull { it.id == selectedUserId }
}

sealed interface UserFeedIntent {
    data object PullToRefresh : UserFeedIntent
    data object Retry : UserFeedIntent

    data object OpenAddUser : UserFeedIntent
    data object DismissAddUser : UserFeedIntent
    data class NameChanged(val value: String) : UserFeedIntent
    data class EmailChanged(val value: String) : UserFeedIntent
    data class GenderChanged(val value: Gender) : UserFeedIntent
    data class StatusChanged(val value: UserStatus) : UserFeedIntent
    data object SubmitNewUser : UserFeedIntent

    data class UserLongPressed(val id: Long) : UserFeedIntent
    data object DismissDeleteConfirm : UserFeedIntent
    data object ConfirmDelete : UserFeedIntent
    data class UndoDelete(val id: Long) : UserFeedIntent

    data class UserSelected(val id: Long) : UserFeedIntent
    data object ClearSelection : UserFeedIntent
}

sealed interface UserFeedEffect {
    data class ShowMessage(val message: String) : UserFeedEffect

    /** UI shows a snackbar with an Undo action lasting [timeoutMillis]. */
    data class ShowUndoDelete(
        val userId: Long,
        val userName: String,
        val timeoutMillis: Long,
    ) : UserFeedEffect
}
