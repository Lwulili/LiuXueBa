package com.example.liuxueba.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.liuxueba.domain.auth.AuthException
import com.example.liuxueba.domain.auth.AuthFailure
import com.example.liuxueba.domain.auth.AuthRepository
import com.example.liuxueba.domain.auth.AuthSession
import com.example.liuxueba.domain.auth.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Owns form drafts in memory across configuration changes; passwords are never persisted. */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    var uiState by mutableStateOf(AuthUiState())
        private set
    private var session by mutableStateOf<AuthSession?>(null)
    val currentUser: AuthUser? get() = session?.user
    private var submissionJob: Job? = null
    private var requestVersion = 0

    fun showLogin() {
        cancelSubmission()
        uiState = uiState.copy(page = AuthPage.LOGIN, notice = null)
    }

    fun showRegistration() {
        cancelSubmission()
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
        if (uiState.notice != null || uiState.isSubmitting) return
        val form = uiState.login.copy(
            email = uiState.login.email.trim(),
            editedFields = setOf(AuthField.EMAIL, AuthField.PASSWORD)
        )
        uiState = uiState.copy(login = form)
        if (!form.canSubmit) return
        submit(
            page = AuthPage.LOGIN,
            operation = { repository.login(form.email, form.password) },
            onSuccess = {
                session = it
                uiState = AuthUiState()
            }
        )
    }

    fun submitRegistration() {
        if (uiState.notice != null || uiState.isSubmitting) return
        val form = uiState.registration.copy(
            username = uiState.registration.username.trim(),
            email = uiState.registration.email.trim(),
            editedFields = AuthField.entries.toSet()
        )
        uiState = uiState.copy(registration = form)
        if (!form.canSubmit) return
        submit(
            page = AuthPage.REGISTRATION,
            operation = { repository.register(form.username, form.email, form.password) },
            onSuccess = {
                uiState = AuthUiState(
                    login = LoginFormState(email = it.email),
                    notice = AuthNotice.REGISTRATION_SUCCESS
                )
            }
        )
    }

    fun dismissNotice() {
        uiState = uiState.copy(notice = null)
    }

    fun showLocalNetworkPermissionNotice() {
        uiState = uiState.copy(notice = AuthNotice.LOCAL_NETWORK_PERMISSION_REQUIRED)
    }

    fun reset() {
        cancelSubmission()
        uiState = AuthUiState()
    }

    fun logout() {
        reset()
        session = null
    }

    private fun updateLogin(update: LoginFormState.() -> LoginFormState) {
        if (uiState.isSubmitting) return
        uiState = uiState.copy(login = uiState.login.update(), notice = null)
    }

    private fun updateRegistration(update: RegistrationFormState.() -> RegistrationFormState) {
        if (uiState.isSubmitting) return
        uiState = uiState.copy(registration = uiState.registration.update(), notice = null)
    }

    private fun cancelSubmission() {
        requestVersion++
        submissionJob?.cancel()
        submissionJob = null
        uiState = uiState.copy(isSubmitting = false)
    }

    private fun <T> submit(page: AuthPage, operation: suspend () -> T, onSuccess: (T) -> Unit) {
        val version = ++requestVersion
        uiState = uiState.copy(isSubmitting = true)
        submissionJob = viewModelScope.launch {
            try {
                val result = operation()
                ensureActive()
                if (version == requestVersion) onSuccess(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AuthException) {
                if (version == requestVersion) {
                    uiState = uiState.copy(notice = failureNotice(error.reason, page))
                }
            } catch (_: Exception) {
                if (version == requestVersion) {
                    uiState = uiState.copy(notice = failureNotice(AuthFailure.SERVICE_UNAVAILABLE, page))
                }
            } finally {
                if (version == requestVersion) uiState = uiState.copy(isSubmitting = false)
            }
        }
    }

    private fun failureNotice(reason: AuthFailure, page: AuthPage): AuthNotice = when (reason) {
        AuthFailure.NETWORK -> AuthNotice.NETWORK_ERROR
        AuthFailure.INVALID_CREDENTIALS -> AuthNotice.INVALID_CREDENTIALS
        AuthFailure.ACCOUNT_EXISTS -> AuthNotice.ACCOUNT_EXISTS
        AuthFailure.INVALID_INPUT -> AuthNotice.INVALID_INPUT
        AuthFailure.SERVICE_UNAVAILABLE -> if (page == AuthPage.LOGIN) {
            AuthNotice.LOGIN_UNAVAILABLE
        } else AuthNotice.REGISTRATION_UNAVAILABLE
    }

    companion object {
        fun factory(repository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AuthViewModel::class.java))
                    return AuthViewModel(repository) as T
                }
            }
    }
}
