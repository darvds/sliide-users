package com.sliide.challenge.users.domain.usecase

import com.sliide.challenge.users.domain.error.AppError
import com.sliide.challenge.users.domain.error.AppResult
import com.sliide.challenge.users.domain.error.asFailure
import com.sliide.challenge.users.domain.model.Gender
import com.sliide.challenge.users.domain.model.NewUser
import com.sliide.challenge.users.domain.model.User
import com.sliide.challenge.users.domain.model.UserStatus
import com.sliide.challenge.users.domain.repository.UserRepository
import com.sliide.challenge.users.domain.validation.EmailValidator
import com.sliide.challenge.users.domain.validation.FieldValidation
import com.sliide.challenge.users.domain.validation.NameValidator

/**
 * Validates then creates. The UI already validates in real time; this is
 * defence in depth so no invalid payload can reach the network regardless of
 * how the use case is invoked.
 */
class CreateUserUseCase(private val repository: UserRepository) {

    suspend operator fun invoke(
        name: String,
        email: String,
        gender: Gender = Gender.FEMALE,
        status: UserStatus = UserStatus.ACTIVE,
    ): AppResult<User> {
        val errors = buildMap {
            (NameValidator.validate(name) as? FieldValidation.Invalid)
                ?.let { put("name", it.reason.name) }
            (EmailValidator.validate(email) as? FieldValidation.Invalid)
                ?.let { put("email", it.reason.name) }
        }
        if (errors.isNotEmpty()) return AppError.Validation(errors).asFailure()

        return repository.createUser(
            NewUser(name = name.trim(), email = email.trim(), gender = gender, status = status)
        )
    }
}
