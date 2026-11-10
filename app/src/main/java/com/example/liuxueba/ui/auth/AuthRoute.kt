package com.example.liuxueba.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.liuxueba.R

/** Connects stateless screens to their state holder and handles navigation within authentication. */
@Composable
fun AuthRoute(viewModel: AuthViewModel, onExit: () -> Unit) {
    val state = viewModel.uiState
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
            onSubmit = viewModel::submitLogin,
            onRegisterClick = viewModel::showRegistration,
            onBack = onBack
        )
        AuthPage.REGISTRATION -> RegisterPage(
            state = state.registration,
            onUsernameChange = viewModel::updateRegistrationUsername,
            onEmailChange = viewModel::updateRegistrationEmail,
            onPasswordChange = viewModel::updateRegistrationPassword,
            onConfirmationChange = viewModel::updateRegistrationConfirmation,
            onTogglePasswordVisibility = viewModel::toggleRegistrationPasswordVisibility,
            onToggleConfirmationVisibility = viewModel::toggleRegistrationConfirmationVisibility,
            onSubmit = viewModel::submitRegistration,
            onLoginClick = viewModel::showLogin,
            onBack = onBack
        )
    }

    state.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            title = {
                Text(stringResource(when (notice) {
                    AuthNotice.LOGIN_UNAVAILABLE -> R.string.auth_login_unavailable
                    AuthNotice.REGISTRATION_UNAVAILABLE -> R.string.auth_register_unavailable
                }))
            },
            text = { Text(stringResource(R.string.auth_try_again)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) {
                    Text(stringResource(R.string.auth_notice_confirm))
                }
            }
        )
    }
}
