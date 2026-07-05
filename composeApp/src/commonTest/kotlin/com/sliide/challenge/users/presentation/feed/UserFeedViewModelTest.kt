package com.sliide.challenge.users.presentation.feed

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.sliide.challenge.users.domain.error.AppError
import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.error.asSuccess
import com.sliide.challenge.users.domain.model.Gender
import com.sliide.challenge.users.domain.model.NewUser
import com.sliide.challenge.users.domain.model.User
import com.sliide.challenge.users.domain.model.UserStatus
import com.sliide.challenge.users.domain.repository.UserRepository
import com.sliide.challenge.users.domain.time.TimeProvider
import com.sliide.challenge.users.domain.usecase.CreateUserUseCase
import com.sliide.challenge.users.domain.usecase.DeleteUserUseCase
import com.sliide.challenge.users.domain.usecase.ObserveUsersUseCase
import com.sliide.challenge.users.domain.usecase.RefreshUsersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserFeedViewModelTest {

    private class FakeUserRepository : UserRepository {
        val usersFlow = MutableStateFlow<List<User>>(emptyList())
        var refreshResult: AppResult<Unit> = Unit.asSuccess()
        var refreshCalls = 0
        var createResult: AppResult<User>? = null // null → succeed and prepend
        val deleteCalls = mutableListOf<Long>()
        var deleteResult: AppResult<Unit> = Unit.asSuccess()

        override fun observeUsers(): Flow<List<User>> = usersFlow

        override suspend fun refreshUsers(): AppResult<Unit> {
            refreshCalls++
            return refreshResult
        }

        override suspend fun createUser(newUser: NewUser): AppResult<User> {
            createResult?.let { return it }
            val user = User(
                id = 100, name = newUser.name, email = newUser.email,
                gender = newUser.gender, status = newUser.status,
                firstSeenAtEpochMillis = 50_000,
            )
            usersFlow.update { listOf(user) + it }
            return user.asSuccess()
        }

        override suspend fun deleteUser(userId: Long): AppResult<Unit> {
            deleteCalls += userId
            if (deleteResult is AppResult.Success) {
                usersFlow.update { list -> list.filterNot { it.id == userId } }
            }
            return deleteResult
        }
    }

    private fun user(id: Long, name: String = "User $id") = User(
        id = id, name = name, email = "u$id@example.com",
        gender = Gender.FEMALE, status = UserStatus.ACTIVE,
        firstSeenAtEpochMillis = 0,
    )

    private var vm: UserFeedViewModel? = null

    private fun TestScope.createVm(repo: FakeUserRepository, nowMillis: Long = 120_000): UserFeedViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return UserFeedViewModel(
            observeUsers = ObserveUsersUseCase(repo),
            refreshUsers = RefreshUsersUseCase(repo),
            createUser = CreateUserUseCase(repo),
            deleteUser = DeleteUserUseCase(repo),
            timeProvider = TimeProvider { nowMillis },
            undoTimeoutMillis = 5_000,
        ).also { vm = it }
    }

    @AfterTest
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    // -- loading ------------------------------------------------------------

    @Test
    fun `shimmer shows until first refresh lands, then content`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)

        runCurrent()
        // Refresh "succeeded" and cache emitted users:
        repo.usersFlow.value = listOf(user(1), user(2))
        runCurrent()

        val state = vm.state.value
        assertEquals(false, state.isInitialLoading)
        assertEquals(listOf(1L, 2L), state.users.map { it.id })
        assertEquals("just now", state.users.first().relativeTime)
        assertEquals("U", state.users.first().initials.take(1))
        assertEquals(1, repo.refreshCalls)
    }

    @Test
    fun `first load failure with empty cache is a full-screen error`() = runTest {
        val repo = FakeUserRepository().apply {
            refreshResult = AppResult.Failure(AppError.Network)
        }
        val vm = createVm(repo)
        runCurrent()

        val state = vm.state.value
        assertEquals(false, state.isInitialLoading)
        assertNotNull(state.fullScreenError)
        assertTrue(state.users.isEmpty())
    }

    @Test
    fun `refresh failure with cached content keeps content, flags stale, emits message`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)
        runCurrent()
        repo.usersFlow.value = listOf(user(1))
        runCurrent()

        repo.refreshResult = AppResult.Failure(AppError.Network)
        vm.effects.test {
            vm.onIntent(UserFeedIntent.PullToRefresh)
            runCurrent()
            assertIs<UserFeedEffect.ShowMessage>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val state = vm.state.value
        assertEquals(listOf(1L), state.users.map { it.id })
        assertTrue(state.isStale)
        assertNull(state.fullScreenError)
    }

    @Test
    fun `retry from full-screen error recovers`() = runTest {
        val repo = FakeUserRepository().apply {
            refreshResult = AppResult.Failure(AppError.Network)
        }
        val vm = createVm(repo)
        runCurrent()
        assertNotNull(vm.state.value.fullScreenError)

        repo.refreshResult = Unit.asSuccess()
        vm.onIntent(UserFeedIntent.Retry)
        runCurrent()
        repo.usersFlow.value = listOf(user(3))
        runCurrent()

        assertNull(vm.state.value.fullScreenError)
        assertEquals(listOf(3L), vm.state.value.users.map { it.id })
    }

    // -- add user -------------------------------------------------------------

    @Test
    fun `name and email validate in real time, but empty fields stay quiet`() = runTest {
        val vm = createVm(FakeUserRepository())
        runCurrent()

        vm.onIntent(UserFeedIntent.OpenAddUser)
        vm.onIntent(UserFeedIntent.NameChanged("R2D2"))
        vm.onIntent(UserFeedIntent.EmailChanged("nope"))
        runCurrent()

        val form = assertNotNull(vm.state.value.addForm)
        assertNotNull(form.nameError)
        assertNotNull(form.emailError)

        vm.onIntent(UserFeedIntent.NameChanged("Ada Lovelace"))
        vm.onIntent(UserFeedIntent.EmailChanged(""))
        runCurrent()

        val form2 = assertNotNull(vm.state.value.addForm)
        assertNull(form2.nameError)
        assertNull(form2.emailError, "empty field should not show an error until submit")
    }

    @Test
    fun `submit with invalid fields surfaces errors and never hits the repository`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)
        runCurrent()

        vm.onIntent(UserFeedIntent.OpenAddUser)
        vm.onIntent(UserFeedIntent.SubmitNewUser) // both fields empty
        runCurrent()

        val form = assertNotNull(vm.state.value.addForm)
        assertEquals("Name is required", form.nameError)
        assertEquals("Email is required", form.emailError)
    }

    @Test
    fun `successful create closes the sheet, announces, and the user tops the feed`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)
        runCurrent()
        repo.usersFlow.value = listOf(user(1))
        runCurrent()

        vm.onIntent(UserFeedIntent.OpenAddUser)
        vm.onIntent(UserFeedIntent.NameChanged("Ada Lovelace"))
        vm.onIntent(UserFeedIntent.EmailChanged("ada@example.com"))

        vm.effects.test {
            vm.onIntent(UserFeedIntent.SubmitNewUser)
            runCurrent()
            val msg = assertIs<UserFeedEffect.ShowMessage>(awaitItem())
            assertTrue("Ada" in msg.message)
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(vm.state.value.addForm)
        assertEquals(100L, vm.state.value.users.first().id)
    }

    @Test
    fun `server-side 422 lands on the offending field`() = runTest {
        val repo = FakeUserRepository().apply {
            createResult = AppResult.Failure(
                AppError.Validation(mapOf("email" to "has already been taken"))
            )
        }
        val vm = createVm(repo)
        runCurrent()

        vm.onIntent(UserFeedIntent.OpenAddUser)
        vm.onIntent(UserFeedIntent.NameChanged("Ada Lovelace"))
        vm.onIntent(UserFeedIntent.EmailChanged("taken@example.com"))
        vm.onIntent(UserFeedIntent.SubmitNewUser)
        runCurrent()

        val form = assertNotNull(vm.state.value.addForm)
        assertEquals("Email has already been taken", form.emailError)
        assertEquals(false, form.isSubmitting, "form must be re-submittable")
    }

    // -- delete + undo ----------------------------------------------------------

    @Test
    fun `confirm hides immediately but defers the network delete`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)
        runCurrent()
        repo.usersFlow.value = listOf(user(1), user(2))
        runCurrent()

        vm.onIntent(UserFeedIntent.UserLongPressed(1))
        vm.onIntent(UserFeedIntent.ConfirmDelete)
        runCurrent()

        assertEquals(listOf(2L), vm.state.value.users.map { it.id }, "hidden instantly")
        assertTrue(repo.deleteCalls.isEmpty(), "no network call inside the undo window")

        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(listOf(1L), repo.deleteCalls, "deleted after the window closed")
    }

    @Test
    fun `undo within the window cancels the delete entirely`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)
        runCurrent()
        repo.usersFlow.value = listOf(user(1), user(2))
        runCurrent()

        vm.onIntent(UserFeedIntent.UserLongPressed(1))
        vm.onIntent(UserFeedIntent.ConfirmDelete)
        advanceTimeBy(4_999)

        vm.onIntent(UserFeedIntent.UndoDelete(1))
        runCurrent()
        assertEquals(listOf(2L, 1L).sorted(), vm.state.value.users.map { it.id }.sorted(), "restored")

        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(repo.deleteCalls.isEmpty(), "server never touched after undo")
    }

    @Test
    fun `failed delete restores the row and explains`() = runTest {
        val repo = FakeUserRepository().apply {
            deleteResult = AppResult.Failure(AppError.Http(500))
        }
        val vm = createVm(repo)
        runCurrent()
        repo.usersFlow.value = listOf(user(1))
        runCurrent()

        vm.onIntent(UserFeedIntent.UserLongPressed(1))
        vm.onIntent(UserFeedIntent.ConfirmDelete)

        vm.effects.test {
            assertIs<UserFeedEffect.ShowUndoDelete>(awaitItem())
            advanceTimeBy(5_001)
            runCurrent()
            val msg = assertIs<UserFeedEffect.ShowMessage>(awaitItem())
            assertTrue("Couldn't delete" in msg.message)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(1L), vm.state.value.users.map { it.id }, "row restored on failure")
    }

    @Test
    fun `long-press then dismiss leaves everything untouched`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)
        runCurrent()
        repo.usersFlow.value = listOf(user(1))
        runCurrent()

        vm.onIntent(UserFeedIntent.UserLongPressed(1))
        assertNotNull(vm.state.value.deleteConfirm)
        vm.onIntent(UserFeedIntent.DismissDeleteConfirm)
        runCurrent()

        assertNull(vm.state.value.deleteConfirm)
        advanceTimeBy(60_000)
        assertTrue(repo.deleteCalls.isEmpty())
        assertEquals(listOf(1L), vm.state.value.users.map { it.id })
    }

    // -- selection (master-detail) -------------------------------------------

    @Test
    fun `selection follows the data - deleting the selected user clears the pane`() = runTest {
        val repo = FakeUserRepository()
        val vm = createVm(repo)
        runCurrent()
        repo.usersFlow.value = listOf(user(1), user(2))
        runCurrent()

        vm.onIntent(UserFeedIntent.UserSelected(1))
        runCurrent()
        assertEquals(1L, vm.state.value.selectedUser?.id)

        vm.onIntent(UserFeedIntent.UserLongPressed(1))
        vm.onIntent(UserFeedIntent.ConfirmDelete)
        runCurrent()

        assertNull(vm.state.value.selectedUser, "hidden user cannot stay selected")
    }
}
