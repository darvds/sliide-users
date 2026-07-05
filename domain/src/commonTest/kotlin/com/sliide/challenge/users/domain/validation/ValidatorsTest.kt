package com.sliide.challenge.users.domain.validation

import com.sliide.challenge.users.domain.validation.FieldValidation.Reason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NameValidatorTest {

    private fun reasonOf(input: String): Reason? =
        (NameValidator.validate(input) as? FieldValidation.Invalid)?.reason

    @Test
    fun `accepts realistic names`() {
        listOf(
            "Ada Lovelace",
            "José Núñez",
            "O'Brien",
            "Jean-Luc Picard",
            "J. R. R. Tolkien",
            "李小龙",
            "Renée",
        ).forEach { assertTrue(NameValidator.validate(it).isValid, "expected valid: $it") }
    }

    @Test
    fun `rejects empty and whitespace-only`() {
        assertEquals(Reason.EMPTY, reasonOf(""))
        assertEquals(Reason.EMPTY, reasonOf("   "))
    }

    @Test
    fun `rejects single character`() {
        assertEquals(Reason.TOO_SHORT, reasonOf("A"))
    }

    @Test
    fun `rejects over 200 chars`() {
        assertEquals(Reason.TOO_LONG, reasonOf("a".repeat(201)))
    }

    @Test
    fun `rejects digits and symbols`() {
        assertEquals(Reason.INVALID_CHARACTERS, reasonOf("R2D2"))
        assertEquals(Reason.INVALID_CHARACTERS, reasonOf("name@domain"))
        assertEquals(Reason.INVALID_CHARACTERS, reasonOf("<script>"))
    }

    @Test
    fun `rejects leading punctuation`() {
        assertEquals(Reason.INVALID_CHARACTERS, reasonOf("-Ada"))
        assertEquals(Reason.INVALID_CHARACTERS, reasonOf("'Brien"))
    }

    @Test
    fun `trims surrounding whitespace before validating`() {
        assertTrue(NameValidator.validate("  Ada Lovelace  ").isValid)
    }
}

class EmailValidatorTest {

    private fun reasonOf(input: String): Reason? =
        (EmailValidator.validate(input) as? FieldValidation.Invalid)?.reason

    @Test
    fun `accepts common formats`() {
        listOf(
            "a@b.co",
            "user@example.com",
            "first.last@example.com",
            "user+tag@example.co.uk",
            "user_name-1@sub.domain.example.org",
        ).forEach { assertTrue(EmailValidator.validate(it).isValid, "expected valid: $it") }
    }

    @Test
    fun `rejects empty`() {
        assertEquals(Reason.EMPTY, reasonOf(""))
        assertEquals(Reason.EMPTY, reasonOf("  "))
    }

    @Test
    fun `rejects structural garbage`() {
        listOf(
            "plainaddress",
            "@no-local.com",
            "no-at-sign.com",
            "user@",
            "user@domain",          // no TLD
            "user@domain.c",        // 1-char TLD
            "user@domain.123",      // numeric TLD
            "user@@double.com",
            "user@.leadingdot.com",
            "user@domain..com",     // empty label
            "user name@domain.com", // space in local
        ).forEach {
            assertEquals(Reason.INVALID_EMAIL_FORMAT, reasonOf(it), "expected invalid: $it")
        }
    }

    @Test
    fun `rejects dot abuse in local part`() {
        assertEquals(Reason.INVALID_EMAIL_FORMAT, reasonOf(".user@domain.com"))
        assertEquals(Reason.INVALID_EMAIL_FORMAT, reasonOf("user.@domain.com"))
        assertEquals(Reason.INVALID_EMAIL_FORMAT, reasonOf("us..er@domain.com"))
    }

    @Test
    fun `rejects over 254 chars`() {
        val local = "a".repeat(250)
        assertEquals(Reason.TOO_LONG, reasonOf("$local@example.com"))
    }

    @Test
    fun `rejects hyphen at domain label edges`() {
        assertEquals(Reason.INVALID_EMAIL_FORMAT, reasonOf("user@-bad.com"))
        assertEquals(Reason.INVALID_EMAIL_FORMAT, reasonOf("user@bad-.com"))
    }
}
