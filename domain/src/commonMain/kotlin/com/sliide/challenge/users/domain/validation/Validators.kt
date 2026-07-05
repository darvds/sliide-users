package com.sliide.challenge.users.domain.validation

/**
 * Real-time form validation, shared across platforms.
 *
 * Kept as pure functions returning typed results (not booleans) so the UI can
 * render specific, helpful messages and tests can assert exact causes.
 */
sealed interface FieldValidation {
    data object Valid : FieldValidation
    data class Invalid(val reason: Reason) : FieldValidation

    enum class Reason {
        EMPTY,
        TOO_SHORT,
        TOO_LONG,
        INVALID_CHARACTERS,
        INVALID_EMAIL_FORMAT,
    }

    val isValid: Boolean get() = this is Valid
}

object NameValidator {
    // GoRest accepts up to 200 chars; 2 is the shortest real-world given name.
    private const val MIN = 2
    private const val MAX = 200

    // Letters (any script), marks (accents), spaces, hyphens, apostrophes,
    // periods (initials). Rejects digits and symbols.
    private val allowed = Regex("""^[\p{L}\p{M}][\p{L}\p{M} .'\-]*$""")

    fun validate(raw: String): FieldValidation {
        val name = raw.trim()
        return when {
            name.isEmpty() -> FieldValidation.Invalid(FieldValidation.Reason.EMPTY)
            name.length < MIN -> FieldValidation.Invalid(FieldValidation.Reason.TOO_SHORT)
            name.length > MAX -> FieldValidation.Invalid(FieldValidation.Reason.TOO_LONG)
            !allowed.matches(name) -> FieldValidation.Invalid(FieldValidation.Reason.INVALID_CHARACTERS)
            else -> FieldValidation.Valid
        }
    }
}

object EmailValidator {
    private const val MAX = 254 // RFC 5321 path limit

    // Pragmatic RFC-5322 subset: rejects consecutive/leading/trailing dots in
    // the local part, requires a dotted domain with a 2+ char alpha TLD.
    // Deliberately NOT the full RFC grammar — the grammar admits addresses
    // (quoted locals, IP literals) that real-world providers reject.
    private val pattern = Regex(
        """^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*""" +
            """@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$"""
    )

    fun validate(raw: String): FieldValidation {
        val email = raw.trim()
        return when {
            email.isEmpty() -> FieldValidation.Invalid(FieldValidation.Reason.EMPTY)
            email.length > MAX -> FieldValidation.Invalid(FieldValidation.Reason.TOO_LONG)
            !pattern.matches(email) -> FieldValidation.Invalid(FieldValidation.Reason.INVALID_EMAIL_FORMAT)
            else -> FieldValidation.Valid
        }
    }
}
