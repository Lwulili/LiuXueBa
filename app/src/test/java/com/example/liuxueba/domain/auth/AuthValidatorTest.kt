package com.example.liuxueba.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun blankLoginFieldsAreRequired() {
        val errors = AuthValidator.validateLogin("  ", "  ")

        assertEquals(AuthValidationError.REQUIRED, errors.email)
        assertEquals(AuthValidationError.REQUIRED, errors.password)
        assertFalse(errors.isValid)
    }

    @Test
    fun commonEmailAddressesAndSurroundingWhitespaceAreAccepted() {
        listOf("student@example.com", " first.last+study@dept.example.ac.uk ").forEach { email ->
            assertTrue(email, AuthValidator.validateLogin(email, "study2026").isValid)
        }
    }

    @Test
    fun malformedEmailAddressesAreRejected() {
        listOf(
            "student", "student@", "@example.com", ".a@example.com", "a..b@example.com",
            "a@example..com", "a@-example.com", "a@example-.com", "a@exam ple.com",
            "a@example.com/path", "a".repeat(65) + "@example.com"
        ).forEach { email ->
            assertEquals(email, AuthValidationError.INVALID_EMAIL,
                AuthValidator.validateLogin(email, "study2026").email)
        }
    }

    @Test
    fun passwordLengthLimitsAreInclusive() {
        listOf(8, 64).forEach { length ->
            assertTrue(AuthValidator.validateLogin("a@example.com", "a".repeat(length)).isValid)
        }
        listOf(7, 65).forEach { length ->
            assertEquals(AuthValidationError.PASSWORD_LENGTH,
                AuthValidator.validateLogin("a@example.com", "a".repeat(length)).password)
        }
    }

    @Test
    fun registrationRequiresAllFourFields() {
        val errors = AuthValidator.validateRegistration("", "", "", "")

        assertEquals(AuthValidationError.REQUIRED, errors.username)
        assertEquals(AuthValidationError.REQUIRED, errors.email)
        assertEquals(AuthValidationError.REQUIRED, errors.password)
        assertEquals(AuthValidationError.REQUIRED, errors.confirmPassword)
        assertFalse(errors.isValid)
    }

    @Test
    fun usernameLengthLimitsApplyAfterTrimming() {
        listOf(" 同学 ", "a".repeat(20)).forEach { username ->
            assertNull(registration(username = username).username)
        }
        listOf("a", "a".repeat(21)).forEach { username ->
            assertEquals(AuthValidationError.USERNAME_LENGTH, registration(username).username)
        }
    }

    @Test
    fun usernamesCannotContainInternalWhitespace() {
        listOf("同 学", "first\tlast", "first\nlast").forEach { username ->
            assertEquals(AuthValidationError.USERNAME_WHITESPACE, registration(username).username)
        }
    }

    @Test
    fun passwordConfirmationMustMatchExactlyWithoutTrimming() {
        val errors = AuthValidator.validateRegistration(
            "同学", "a@example.com", " study2026 ", "study2026"
        )

        assertNull(errors.password)
        assertEquals(AuthValidationError.PASSWORD_MISMATCH, errors.confirmPassword)
        assertFalse(errors.isValid)
    }

    @Test
    fun validRegistrationHasNoErrors() {
        assertTrue(registration().isValid)
    }

    private fun registration(username: String = "留学同学") =
        AuthValidator.validateRegistration(username, "student@example.com", "study2026", "study2026")
}
