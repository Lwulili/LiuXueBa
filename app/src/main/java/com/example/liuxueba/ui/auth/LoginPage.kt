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
fun LoginPage(
    state: LoginFormState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onRegisterClick: () -> Unit,
    onBack: () -> Unit
) {
    AuthPageLayout(
        title = stringResource(R.string.auth_login_title),
        subtitle = stringResource(R.string.auth_login_subtitle),
        onBack = onBack
    ) {
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_email),
            error = state.errorFor(AuthField.EMAIL),
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(16.dp))
        AuthPasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            visible = state.passwordVisible,
            onToggleVisibility = onTogglePasswordVisibility,
            error = state.errorFor(AuthField.PASSWORD),
            imeAction = ImeAction.Done,
            helperText = stringResource(R.string.auth_password_hint),
            onSubmit = onSubmit
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.auth_login)) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_go_register))
        }
    }
}
