package com.sliide.challenge.users.presentation.feed

import androidx.lifecycle.viewModelScope
import com.sliide.challenge.users.domain.error.AppError
import com.sliide.challenge.users.domain.error.onFailure
import com.sliide.challenge.users.domain.error.onSuccess
import com.sliide.challenge.users.domain.model.User
import com.sliide.challenge.users.domain.time.RelativeTime
import com.sliide.challenge.users.domain.time.TimeProvider
import com.sliide.challenge.users.domain.usecase.CreateUserUseCase
import com.sliide.challenge.users.domain.usecase.DeleteUserUseCase
import com.sliide.challenge.users.domain.usecase.ObserveUsersUseCase
import com.sliide.challenge.users.domain.usecase.RefreshUsersUseCase
import com.sliide.challenge.users.domain.validation.EmailValidator
import com.sliide.challenge.users.domain.validation.FieldValidation
import com.sliide.challenge.users.domain.validation.NameValidator
import com.sliide.challenge.users.presentation.mvi.MviViewModel
import com.sliide.challenge.users.presentation.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI state machine for the user feed.
 *
 * Undo design ("deferred delete"): confirming a delete only hides the row
 * (pendingDeletes) and starts a countdown. The DELETE request fires when the
 * countdown expires; Undo cancels it entirely. The server is never touched
 * until the user's undo window has passed — a real undo, not delete+recreate.
 */
class UserFeedViewModel(
    private val observeUsers: ObserveUsersUseCase,
    private val refreshUsers: RefreshUsersUseCase,
    private val createUser: CreateUserUseCase,
    private val deleteUser: DeleteUserUseCase,
    private val timeProvider: TimeProvider,
    private val undoTimeoutMillis: Long = UNDO_TIMEOUT_MILLIS,
    private val clockTickMillis: Long = CLOCK_TICK_MILLIS,
) : MviViewModel<UserFeedState, UserFeedIntent, UserFeedEffect>(UserFeedState()) {

    private val pendingDeletes = MutableStateFlow<Set<Long>>(emptySet())
    private val undoJobs = mutableMapOf<Long, Job>()

    /** Re-emits periodically so "5 minutes ago" labels stay honest. */
    private val clock = flow {
        while (true) {
            emit(Unit)
            delay(clockTickMillis)
        }
    }

    init {
        viewModelScope.launch {
            combine(observeUsers(), pendingDeletes, clock) { users, pending, _ ->
                users.filter { it.id !in pending }.map { it.toUi() }
            }.collect { items ->
                setState {
                    copy(
                        users = items,
                        // Anything cached beats a spinner.
                        isInitialLoading = isInitialLoading && items.isEmpty(),
                        fullScreenError = if (items.isEmpty()) fullScreenError else null,
                    )
                }
            }
        }
        refresh(initial = true)
    }

    override fun onIntent(intent: UserFeedIntent) {
        when (intent) {
            UserFeedIntent.PullToRefresh -> refresh(initial = false)
            UserFeedIntent.Retry -> refresh(initial = true)

            UserFeedIntent.OpenAddUser ->
                setState { copy(addForm = AddUserFormState()) }
            UserFeedIntent.DismissAddUser ->
                setState { copy(addForm = null) }
            is UserFeedIntent.NameChanged -> updateForm {
                copy(name = intent.value, nameError = liveError(intent.value, NameValidator::validate, ::nameErrorCopy))
            }
            is UserFeedIntent.EmailChanged -> updateForm {
                copy(email = intent.value, emailError = liveError(intent.value, EmailValidator::validate, ::emailErrorCopy))
            }
            is UserFeedIntent.GenderChanged -> updateForm { copy(gender = intent.value) }
            is UserFeedIntent.StatusChanged -> updateForm { copy(status = intent.value) }
            UserFeedIntent.SubmitNewUser -> submitNewUser()

            is UserFeedIntent.UserLongPressed ->
                setState { copy(deleteConfirm = users.firstOrNull { it.id == intent.id }) }
            UserFeedIntent.DismissDeleteConfirm ->
                setState { copy(deleteConfirm = null) }
            UserFeedIntent.ConfirmDelete -> confirmDelete()
            is UserFeedIntent.UndoDelete -> undoDelete(intent.id)

            is UserFeedIntent.UserSelected ->
                setState { copy(selectedUserId = intent.id) }
            UserFeedIntent.ClearSelection ->
                setState { copy(selectedUserId = null) }
        }
    }

    // -- feed -----------------------------------------------------------------

    private fun refresh(initial: Boolean) {
        viewModelScope.launch {
            setState {
                if (initial && users.isEmpty()) {
                    copy(isInitialLoading = true, fullScreenError = null)
                } else {
                    copy(isRefreshing = true)
                }
            }
            refreshUsers()
                .onSuccess {
                    setState {
                        copy(isInitialLoading = false, isRefreshing = false, isStale = false, fullScreenError = null)
                    }
                }
                .onFailure { error ->
                    val message = error.toUserMessage()
                    setState {
                        if (users.isEmpty()) {
                            copy(isInitialLoading = false, isRefreshing = false, fullScreenError = message)
                        } else {
                            copy(isInitialLoading = false, isRefreshing = false, isStale = error is AppError.Network)
                        }
                    }
                    if (currentState.users.isNotEmpty()) {
                        sendEffect(UserFeedEffect.ShowMessage(message))
                    }
                }
        }
    }

    // -- add user ---------------------------------------------------------------

    private inline fun updateForm(reduce: AddUserFormState.() -> AddUserFormState) {
        setState { copy(addForm = addForm?.reduce()) }
    }

    /** Real-time validation, but a pristine/empty field is not shouted at. */
    private fun liveError(
        value: String,
        validate: (String) -> FieldValidation,
        copyFor: (FieldValidation.Reason) -> String,
    ): String? {
        if (value.isBlank()) return null
        val result = validate(value)
        return (result as? FieldValidation.Invalid)?.let { copyFor(it.reason) }
    }

    private fun submitNewUser() {
        val form = currentState.addForm ?: return

        val nameInvalid = NameValidator.validate(form.name) as? FieldValidation.Invalid
        val emailInvalid = EmailValidator.validate(form.email) as? FieldValidation.Invalid
        if (nameInvalid != null || emailInvalid != null) {
            updateForm {
                copy(
                    nameError = nameInvalid?.let { nameErrorCopy(it.reason) },
                    emailError = emailInvalid?.let { emailErrorCopy(it.reason) },
                )
            }
            return
        }

        updateForm { copy(isSubmitting = true) }
        viewModelScope.launch {
            createUser(form.name, form.email, form.gender, form.status)
                .onSuccess { user ->
                    setState { copy(addForm = null) }
                    sendEffect(UserFeedEffect.ShowMessage("${user.name} added"))
                }
                .onFailure { error ->
                    when (error) {
                        is AppError.Validation -> updateForm {
                            copy(
                                isSubmitting = false,
                                nameError = error.fieldErrors["name"]?.let { "Name $it" } ?: nameError,
                                emailError = error.fieldErrors["email"]?.let { "Email $it" } ?: emailError,
                            )
                        }
                        else -> {
                            updateForm { copy(isSubmitting = false) }
                            sendEffect(UserFeedEffect.ShowMessage(error.toUserMessage()))
                        }
                    }
                }
        }
    }

    // -- delete + undo ------------------------------------------------------

    private fun confirmDelete() {
        val target = currentState.deleteConfirm ?: return
        setState { copy(deleteConfirm = null) }

        pendingDeletes.update { it + target.id }
        sendEffect(UserFeedEffect.ShowUndoDelete(target.id, target.name, undoTimeoutMillis))

        undoJobs[target.id] = viewModelScope.launch {
            delay(undoTimeoutMillis)
            finalizeDelete(target)
        }
    }

    private fun undoDelete(id: Long) {
        undoJobs.remove(id)?.cancel()
        pendingDeletes.update { it - id }
    }

    private suspend fun finalizeDelete(target: UserItemUi) {
        undoJobs.remove(target.id)
        deleteUser(target.id)
            .onSuccess {
                pendingDeletes.update { it - target.id }
            }
            .onFailure { error ->
                // Restore the row — the server still has it.
                pendingDeletes.update { it - target.id }
                sendEffect(
                    UserFeedEffect.ShowMessage("Couldn't delete ${target.name}: ${error.toUserMessage()}")
                )
            }
    }

    // -- mapping --------------------------------------------------------------

    private fun User.toUi() = UserItemUi(
        id = id,
        name = name,
        email = email,
        relativeTime = RelativeTime.format(timeProvider.nowEpochMillis(), firstSeenAtEpochMillis),
        initials = initialsOf(name),
        gender = gender,
        status = status,
    )

    private companion object {
        const val UNDO_TIMEOUT_MILLIS = 5_000L
        const val CLOCK_TICK_MILLIS = 60_000L

        fun initialsOf(name: String): String =
            name.split(' ', '-', '.')
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifEmpty { "?" }

        fun nameErrorCopy(reason: FieldValidation.Reason): String = when (reason) {
            FieldValidation.Reason.EMPTY -> "Name is required"
            FieldValidation.Reason.TOO_SHORT -> "Name is too short"
            FieldValidation.Reason.TOO_LONG -> "Name is too long (200 max)"
            else -> "Only letters, spaces, hyphens and apostrophes"
        }

        fun emailErrorCopy(reason: FieldValidation.Reason): String = when (reason) {
            FieldValidation.Reason.EMPTY -> "Email is required"
            FieldValidation.Reason.TOO_LONG -> "Email is too long"
            else -> "That doesn't look like a valid email"
        }
    }
}
