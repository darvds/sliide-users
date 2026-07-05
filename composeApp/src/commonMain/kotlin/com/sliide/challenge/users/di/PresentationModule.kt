package com.sliide.challenge.users.di

import com.sliide.challenge.users.domain.usecase.CreateUserUseCase
import com.sliide.challenge.users.domain.usecase.DeleteUserUseCase
import com.sliide.challenge.users.domain.usecase.ObserveUsersUseCase
import com.sliide.challenge.users.domain.usecase.RefreshUsersUseCase
import com.sliide.challenge.users.presentation.feed.UserFeedViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule: Module = module {
    factory { ObserveUsersUseCase(get()) }
    factory { RefreshUsersUseCase(get()) }
    factory { CreateUserUseCase(get()) }
    factory { DeleteUserUseCase(get()) }

    viewModel { UserFeedViewModel(get(), get(), get(), get(), get()) }
}
