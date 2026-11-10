package com.example.liuxueba.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.example.liuxueba.R

/** Connects stateless screens to their state holder and handles navigation within authentication. */
@Composable
fun AuthRoute(viewModel: AuthViewModel, onExit: () -> Unit) {
    val state = viewModel.uiState
    val submit = rememberLocalApiSubmission(
        onGranted = { page ->
            if (page == viewModel.uiState.page) {
                when (page) {
                    AuthPage.LOGIN -> viewModel.submitLogin()
                    AuthPage.REGISTRATION -> viewModel.submitRegistration()
                }
            }
        },
        onDenied = viewModel::showLocalNetworkPermissionNotice
    )
    LaunchedEffect(viewModel.currentUser) {
        if (viewModel.currentUser != null) onExit()
    }
    val onBack: () -> Unit = {
        if (state.page == AuthPage.REGISTRATION) {
            viewModel.showLogin()
        } else {
            viewModel.reset()
            onExit()
        }
    }
    BackHandler(onBack = onBack)

    when (state.page) {
        AuthPage.LOGIN -> LoginPage(
            state = state.login,
            onEmailChange = viewModel::updateLoginEmail,
            onPasswordChange = viewModel::updateLoginPassword,
            onTogglePasswordVisibility = viewModel::toggleLoginPasswordVisibility,
            onSubmit = {
                if (state.login.canSubmit) submit(AuthPage.LOGIN) else viewModel.submitLogin()
            },
            onRegisterClick = viewModel::showRegistration,
            onBack = onBack,
            isSubmitting = state.isSubmitting
        )
        AuthPage.REGISTRATION -> RegisterPage(
            state = state.registration,
            onUsernameChange = viewModel::updateRegistrationUsername,
            onEmailChange = viewModel::updateRegistrationEmail,
            onPasswordChange = viewModel::updateRegistrationPassword,
            onConfirmationChange = viewModel::updateRegistrationConfirmation,
            onTogglePasswordVisibility = viewModel::toggleRegistrationPasswordVisibility,
            onToggleConfirmationVisibility = viewModel::toggleRegistrationConfirmationVisibility,
            onSubmit = {
                if (state.registration.canSubmit) submit(AuthPage.REGISTRATION) else viewModel.submitRegistration()
            },
            onLoginClick = viewModel::showLogin,
            onBack = onBack,
            isSubmitting = state.isSubmitting
        )
    }

    state.notice?.let { notice ->
        val (title, message) = when (notice) {
            AuthNotice.LOGIN_UNAVAILABLE -> R.string.auth_login_unavailable to R.string.auth_try_again
            AuthNotice.REGISTRATION_UNAVAILABLE -> R.string.auth_register_unavailable to R.string.auth_try_again
            AuthNotice.REGISTRATION_SUCCESS -> R.string.auth_registration_success to R.string.auth_login_after_registration
            AuthNotice.INVALID_CREDENTIALS -> R.string.auth_wrong_credentials to R.string.auth_check_credentials
            AuthNotice.ACCOUNT_EXISTS -> R.string.auth_account_exists to R.string.auth_change_account
            AuthNotice.INVALID_INPUT -> R.string.auth_invalid_input to R.string.auth_check_input
            AuthNotice.NETWORK_ERROR -> R.string.auth_network_error to R.string.auth_check_network
            AuthNotice.LOCAL_NETWORK_PERMISSION_REQUIRED -> R.string.auth_network_permission_required to R.string.auth_allow_local_network
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) {
                    Text(stringResource(R.string.auth_notice_confirm))
                }
            }
        )
    }
}
