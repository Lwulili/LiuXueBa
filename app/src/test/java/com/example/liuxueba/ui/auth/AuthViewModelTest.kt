package com.example.liuxueba.ui.auth

import com.example.liuxueba.domain.auth.AuthValidationError
import com.example.liuxueba.domain.auth.AuthException
import com.example.liuxueba.domain.auth.AuthFailure
import com.example.liuxueba.domain.auth.AuthRepository
import com.example.liuxueba.domain.auth.AuthSession
import com.example.liuxueba.domain.auth.AuthUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class AuthViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()
    private val repository = TestAuthRepository()
    private val viewModel = AuthViewModel(repository)

    @Test
    fun pristineFormsDisableSubmissionWithoutShowingErrors() {
        assertFalse(viewModel.uiState.login.canSubmit)
        assertFalse(viewModel.uiState.registration.canSubmit)
        assertNull(viewModel.uiState.login.errorFor(AuthField.EMAIL))
        assertNull(viewModel.uiState.registration.errorFor(AuthField.USERNAME))
    }

    @Test
    fun clearingAnEditedEmailShowsRequiredError() {
        viewModel.updateLoginEmail("student@example.com")
        viewModel.updateLoginEmail("")

        assertEquals(AuthValidationError.REQUIRED, viewModel.uiState.login.errorFor(AuthField.EMAIL))
        assertFalse(viewModel.uiState.login.canSubmit)
    }

    @Test
    fun fixingLoginInputsEnablesSubmissionAndClearsFieldErrors() {
        viewModel.updateLoginEmail("invalid")
        assertEquals(AuthValidationError.INVALID_EMAIL,
            viewModel.uiState.login.errorFor(AuthField.EMAIL))

        viewModel.updateLoginEmail("student@example.com")
        viewModel.updateLoginPassword("study2026")

        assertTrue(viewModel.uiState.login.canSubmit)
        assertNull(viewModel.uiState.login.errorFor(AuthField.EMAIL))
    }

    @Test
    fun submittingInvalidLoginShowsErrorsWithoutReportingSuccess() {
        viewModel.submitLogin()

        assertEquals(AuthValidationError.REQUIRED, viewModel.uiState.login.errorFor(AuthField.EMAIL))
        assertEquals(AuthValidationError.REQUIRED, viewModel.uiState.login.errorFor(AuthField.PASSWORD))
        assertNull(viewModel.uiState.notice)
        assertEquals(AuthPage.LOGIN, viewModel.uiState.page)
    }

    @Test
    fun validLoginNormalizesEmailButPreservesPasswordCharacters() {
        viewModel.updateLoginEmail(" student@example.com ")
        viewModel.updateLoginPassword(" study2026 ")
        viewModel.submitLogin()

        assertEquals("student@example.com", viewModel.uiState.login.email)
        assertEquals(" study2026 ", viewModel.uiState.login.password)
        assertEquals(AuthNotice.LOGIN_UNAVAILABLE, viewModel.uiState.notice)
        assertEquals(AuthPage.LOGIN, viewModel.uiState.page)
    }

    @Test
    fun changingRegistrationPasswordRevalidatesExistingConfirmation() {
        fillRegistration()
        assertTrue(viewModel.uiState.registration.canSubmit)

        viewModel.updateRegistrationPassword("newstudy2026")

        assertFalse(viewModel.uiState.registration.canSubmit)
        assertEquals(AuthValidationError.PASSWORD_MISMATCH,
            viewModel.uiState.registration.errorFor(AuthField.CONFIRM_PASSWORD))
        viewModel.updateRegistrationConfirmation("newstudy2026")
        assertTrue(viewModel.uiState.registration.canSubmit)
    }

    @Test
    fun registrationSubmissionNormalizesPublicFields() {
        fillRegistration()
        viewModel.updateRegistrationUsername(" 留学同学 ")
        viewModel.updateRegistrationEmail(" student@example.com ")
        viewModel.submitRegistration()

        assertEquals("留学同学", viewModel.uiState.registration.username)
        assertEquals("student@example.com", viewModel.uiState.registration.email)
        assertEquals(AuthNotice.REGISTRATION_UNAVAILABLE, viewModel.uiState.notice)
    }

    @Test
    fun invalidRegistrationCannotShowSubmissionNotice() {
        viewModel.submitRegistration()

        assertNull(viewModel.uiState.notice)
        assertEquals(AuthValidationError.REQUIRED,
            viewModel.uiState.registration.errorFor(AuthField.USERNAME))
    }

    @Test
    fun pageSwitchingKeepsSeparateFormDraftsAndClearsNotice() {
        viewModel.updateLoginEmail("login@example.com")
        viewModel.updateLoginPassword("study2026")
        viewModel.submitLogin()
        viewModel.showRegistration()
        viewModel.updateRegistrationEmail("register@example.com")
        assertEquals(AuthPage.REGISTRATION, viewModel.uiState.page)
        assertNull(viewModel.uiState.notice)

        viewModel.showLogin()

        assertEquals(AuthPage.LOGIN, viewModel.uiState.page)
        assertEquals("login@example.com", viewModel.uiState.login.email)
        assertEquals("register@example.com", viewModel.uiState.registration.email)
    }

    @Test
    fun passwordVisibilityTogglesAreIndependent() {
        viewModel.toggleLoginPasswordVisibility()
        viewModel.toggleRegistrationPasswordVisibility()
        assertTrue(viewModel.uiState.login.passwordVisible)
        assertTrue(viewModel.uiState.registration.passwordVisible)
        assertFalse(viewModel.uiState.registration.confirmPasswordVisible)

        viewModel.toggleRegistrationConfirmationVisibility()
        viewModel.toggleRegistrationPasswordVisibility()
        assertTrue(viewModel.uiState.registration.confirmPasswordVisible)
        assertFalse(viewModel.uiState.registration.passwordVisible)
    }

    @Test
    fun dismissingNoticeAllowsAnotherAttempt() {
        viewModel.updateLoginEmail("student@example.com")
        viewModel.updateLoginPassword("study2026")
        viewModel.submitLogin()
        viewModel.dismissNotice()
        assertNull(viewModel.uiState.notice)

        viewModel.submitLogin()
        assertEquals(AuthNotice.LOGIN_UNAVAILABLE, viewModel.uiState.notice)
    }

    @Test
    fun resetClearsBothFormsAndHidesPasswords() {
        viewModel.updateLoginEmail("student@example.com")
        viewModel.updateLoginPassword("study2026")
        viewModel.toggleLoginPasswordVisibility()
        fillRegistration()
        viewModel.toggleRegistrationConfirmationVisibility()
        viewModel.reset()

        assertEquals(AuthPage.LOGIN, viewModel.uiState.page)
        assertEquals("", viewModel.uiState.login.password)
        assertEquals("", viewModel.uiState.registration.password)
        assertEquals("", viewModel.uiState.registration.confirmPassword)
        assertFalse(viewModel.uiState.login.passwordVisible)
        assertFalse(viewModel.uiState.registration.confirmPasswordVisible)
        assertFalse(viewModel.uiState.login.canSubmit)
        assertFalse(viewModel.uiState.registration.canSubmit)
    }

    @Test
    fun successfulLoginSetsCurrentUserAndClearsPasswords() {
        repository.loginResult = AuthSession(testUser, "token", 3600)
        fillLogin()
        viewModel.submitLogin()
        assertEquals(testUser, viewModel.currentUser)
        assertEquals("", viewModel.uiState.login.password)
        assertFalse(viewModel.uiState.isSubmitting)
    }

    @Test
    fun successfulRegistrationReturnsToLoginWithoutCreatingSession() {
        repository.registrationResult = testUser
        fillRegistration()
        viewModel.submitRegistration()
        assertEquals(AuthPage.LOGIN, viewModel.uiState.page)
        assertEquals("student@example.com", viewModel.uiState.login.email)
        assertEquals(AuthNotice.REGISTRATION_SUCCESS, viewModel.uiState.notice)
        assertNull(viewModel.currentUser)
        assertEquals("", viewModel.uiState.registration.password)
    }

    @Test
    fun inFlightRequestBlocksDuplicateSubmissionsAndLateResultAfterExit() {
        repository.pendingLogin = CompletableDeferred()
        fillLogin()
        viewModel.submitLogin()
        assertTrue(viewModel.uiState.isSubmitting)
        viewModel.submitLogin()
        assertEquals(1, repository.loginCalls)
        viewModel.reset()
        repository.pendingLogin!!.complete(AuthSession(testUser, "token", 3600))
        assertNull(viewModel.currentUser)
        assertFalse(viewModel.uiState.isSubmitting)
        assertEquals("", viewModel.uiState.login.password)
    }

    @Test
    fun failedLoginKeepsUserLoggedOutAndShowsActionableMessage() {
        repository.failure = AuthFailure.INVALID_CREDENTIALS
        fillLogin()
        viewModel.submitLogin()
        assertNull(viewModel.currentUser)
        assertEquals(AuthNotice.INVALID_CREDENTIALS, viewModel.uiState.notice)
    }

    @Test
    fun logoutClearsInMemoryIdentity() {
        repository.loginResult = AuthSession(testUser, "token", 3600)
        fillLogin()
        viewModel.submitLogin()
        viewModel.logout()
        assertNull(viewModel.currentUser)
        assertEquals("", viewModel.uiState.login.password)
    }

    private fun fillLogin() {
        viewModel.updateLoginEmail("student@example.com")
        viewModel.updateLoginPassword("study2026")
    }

    private fun fillRegistration() {
        viewModel.showRegistration()
        viewModel.updateRegistrationUsername("留学同学")
        viewModel.updateRegistrationEmail("student@example.com")
        viewModel.updateRegistrationPassword("study2026")
        viewModel.updateRegistrationConfirmation("study2026")
    }
}

private val testUser = AuthUser("user-123", "student", "student@example.com", "同学", "user")

private class TestAuthRepository : AuthRepository {
    var loginResult: AuthSession? = null
    var registrationResult: AuthUser? = null
    var pendingLogin: CompletableDeferred<AuthSession>? = null
    var failure = AuthFailure.SERVICE_UNAVAILABLE
    var loginCalls = 0
    override suspend fun register(username: String, email: String, password: String): AuthUser =
        registrationResult ?: throw AuthException(failure)
    override suspend fun login(email: String, password: String): AuthSession {
        loginCalls++
        return pendingLogin?.await() ?: loginResult ?: throw AuthException(failure)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private val dispatcher = UnconfinedTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
