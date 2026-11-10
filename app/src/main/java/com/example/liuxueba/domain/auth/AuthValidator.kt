package com.example.liuxueba.domain.auth

enum class AuthValidationError {
    REQUIRED,
    INVALID_EMAIL,
    USERNAME_LENGTH,
    USERNAME_WHITESPACE,
    PASSWORD_LENGTH,
    PASSWORD_MISMATCH
}

data class LoginErrors(
    val email: AuthValidationError? = null,
    val password: AuthValidationError? = null
) {
    val isValid: Boolean get() = email == null && password == null
}

data class RegistrationErrors(
    val username: AuthValidationError? = null,
    val email: AuthValidationError? = null,
    val password: AuthValidationError? = null,
    val confirmPassword: AuthValidationError? = null
) {
    val isValid: Boolean
        get() = username == null && email == null && password == null && confirmPassword == null
}

/** Form rules shared by both screens, independent of Android and the UI. */
object AuthValidator {
    private val emailPattern = Regex(
        "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@" +
            "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
            "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+"
    )

    fun validateLogin(email: String, password: String) = LoginErrors(
        email = validateEmail(email),
        password = validatePassword(password)
    )

    fun validateRegistration(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) = RegistrationErrors(
        username = validateUsername(username),
        email = validateEmail(email),
        password = validatePassword(password),
        confirmPassword = when {
            confirmPassword.isBlank() -> AuthValidationError.REQUIRED
            confirmPassword != password -> AuthValidationError.PASSWORD_MISMATCH
            else -> null
        }
    )

    private fun validateUsername(value: String): AuthValidationError? {
        val username = value.trim()
        return when {
            username.isEmpty() -> AuthValidationError.REQUIRED
            username.length !in 2..20 -> AuthValidationError.USERNAME_LENGTH
            username.any(Char::isWhitespace) -> AuthValidationError.USERNAME_WHITESPACE
            else -> null
        }
    }

    private fun validateEmail(value: String): AuthValidationError? {
        val email = value.trim()
        return when {
            email.isEmpty() -> AuthValidationError.REQUIRED
            email.length > 254 || email.substringBefore('@').length > 64 ||
                !emailPattern.matches(email) -> AuthValidationError.INVALID_EMAIL
            else -> null
        }
    }

    private fun validatePassword(password: String): AuthValidationError? = when {
        password.isBlank() -> AuthValidationError.REQUIRED
        password.length !in 8..64 -> AuthValidationError.PASSWORD_LENGTH
        else -> null
    }
}
