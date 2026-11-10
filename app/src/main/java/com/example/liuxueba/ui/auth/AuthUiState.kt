package com.example.liuxueba.ui.auth

import com.example.liuxueba.domain.auth.AuthValidationError
import com.example.liuxueba.domain.auth.AuthValidator

enum class AuthPage { LOGIN, REGISTRATION }

enum class AuthField { USERNAME, EMAIL, PASSWORD, CONFIRM_PASSWORD }

enum class AuthNotice {
    LOGIN_UNAVAILABLE, REGISTRATION_UNAVAILABLE, REGISTRATION_SUCCESS,
    INVALID_CREDENTIALS, ACCOUNT_EXISTS, INVALID_INPUT, NETWORK_ERROR,
    LOCAL_NETWORK_PERMISSION_REQUIRED
}

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val editedFields: Set<AuthField> = emptySet()
) {
    private val errors get() = AuthValidator.validateLogin(email, password)
    val canSubmit: Boolean get() = errors.isValid

    fun errorFor(field: AuthField): AuthValidationError? {
        if (field !in editedFields) return null
        return when (field) {
            AuthField.EMAIL -> errors.email
            AuthField.PASSWORD -> errors.password
            else -> null
        }
    }
}

data class RegistrationFormState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val editedFields: Set<AuthField> = emptySet()
) {
    private val errors
        get() = AuthValidator.validateRegistration(username, email, password, confirmPassword)
    val canSubmit: Boolean get() = errors.isValid

    fun errorFor(field: AuthField): AuthValidationError? {
        if (field !in editedFields) return null
        return when (field) {
            AuthField.USERNAME -> errors.username
            AuthField.EMAIL -> errors.email
            AuthField.PASSWORD -> errors.password
            AuthField.CONFIRM_PASSWORD -> errors.confirmPassword
        }
    }
}

data class AuthUiState(
    val page: AuthPage = AuthPage.LOGIN,
    val login: LoginFormState = LoginFormState(),
    val registration: RegistrationFormState = RegistrationFormState(),
    val notice: AuthNotice? = null,
    val isSubmitting: Boolean = false
)
