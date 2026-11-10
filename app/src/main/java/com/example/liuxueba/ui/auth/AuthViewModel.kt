package com.example.liuxueba.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Owns form drafts in memory across configuration changes; passwords are never persisted. */
class AuthViewModel : ViewModel() {
    var uiState by mutableStateOf(AuthUiState())
        private set

    fun showLogin() {
        uiState = uiState.copy(page = AuthPage.LOGIN, notice = null)
    }

    fun showRegistration() {
        uiState = uiState.copy(page = AuthPage.REGISTRATION, notice = null)
    }

    fun updateLoginEmail(value: String) = updateLogin {
        copy(email = value, editedFields = editedFields + AuthField.EMAIL)
    }

    fun updateLoginPassword(value: String) = updateLogin {
        copy(password = value, editedFields = editedFields + AuthField.PASSWORD)
    }

    fun toggleLoginPasswordVisibility() = updateLogin {
        copy(passwordVisible = !passwordVisible)
    }

    fun updateRegistrationUsername(value: String) = updateRegistration {
        copy(username = value, editedFields = editedFields + AuthField.USERNAME)
    }

    fun updateRegistrationEmail(value: String) = updateRegistration {
        copy(email = value, editedFields = editedFields + AuthField.EMAIL)
    }

    fun updateRegistrationPassword(value: String) = updateRegistration {
        copy(password = value, editedFields = editedFields + AuthField.PASSWORD)
    }

    fun updateRegistrationConfirmation(value: String) = updateRegistration {
        copy(confirmPassword = value, editedFields = editedFields + AuthField.CONFIRM_PASSWORD)
    }

    fun toggleRegistrationPasswordVisibility() = updateRegistration {
        copy(passwordVisible = !passwordVisible)
    }

    fun toggleRegistrationConfirmationVisibility() = updateRegistration {
        copy(confirmPasswordVisible = !confirmPasswordVisible)
    }

    fun submitLogin() {
        if (uiState.notice != null) return
        val form = uiState.login.copy(
            email = uiState.login.email.trim(),
            editedFields = setOf(AuthField.EMAIL, AuthField.PASSWORD)
        )
        // Authentication will be connected here when the account API is implemented.
        uiState = uiState.copy(
            login = form,
            notice = if (form.canSubmit) AuthNotice.LOGIN_UNAVAILABLE else null
        )
    }

    fun submitRegistration() {
        if (uiState.notice != null) return
        val form = uiState.registration.copy(
            username = uiState.registration.username.trim(),
            email = uiState.registration.email.trim(),
            editedFields = AuthField.entries.toSet()
        )
        uiState = uiState.copy(
            registration = form,
            notice = if (form.canSubmit) AuthNotice.REGISTRATION_UNAVAILABLE else null
        )
    }

    fun dismissNotice() {
        uiState = uiState.copy(notice = null)
    }

    fun reset() {
        uiState = AuthUiState()
    }

    private fun updateLogin(update: LoginFormState.() -> LoginFormState) {
        uiState = uiState.copy(login = uiState.login.update(), notice = null)
    }

    private fun updateRegistration(update: RegistrationFormState.() -> RegistrationFormState) {
        uiState = uiState.copy(registration = uiState.registration.update(), notice = null)
    }
}
