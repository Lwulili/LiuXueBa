package com.example.liuxueba.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.liuxueba.R

@Composable
fun RegisterPage(
    state: RegistrationFormState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmationVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onLoginClick: () -> Unit,
    onBack: () -> Unit,
    isSubmitting: Boolean = false
) {
    AuthPageLayout(
        title = stringResource(R.string.auth_register_title),
        subtitle = stringResource(R.string.auth_register_subtitle),
        onBack = onBack
    ) {
        AuthTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.auth_username),
            error = state.errorFor(AuthField.USERNAME),
            enabled = !isSubmitting,
            helperText = stringResource(R.string.auth_username_hint)
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_email),
            error = state.errorFor(AuthField.EMAIL),
            enabled = !isSubmitting,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(12.dp))
        AuthPasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            visible = state.passwordVisible,
            onToggleVisibility = onTogglePasswordVisibility,
            error = state.errorFor(AuthField.PASSWORD),
            enabled = !isSubmitting,
            helperText = stringResource(R.string.auth_password_hint)
        )
        Spacer(Modifier.height(12.dp))
        AuthPasswordField(
            value = state.confirmPassword,
            onValueChange = onConfirmationChange,
            label = stringResource(R.string.auth_confirm_password),
            visible = state.confirmPasswordVisible,
            onToggleVisibility = onToggleConfirmationVisibility,
            error = state.errorFor(AuthField.CONFIRM_PASSWORD),
            enabled = !isSubmitting,
            imeAction = ImeAction.Done,
            onSubmit = onSubmit
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit && !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(if (isSubmitting) R.string.auth_registering else R.string.auth_register)) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onLoginClick, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_go_login))
        }
    }
}
