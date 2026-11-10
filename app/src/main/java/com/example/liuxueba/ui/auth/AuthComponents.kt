package com.example.liuxueba.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.liuxueba.R
import com.example.liuxueba.domain.auth.AuthValidationError

@Composable
internal fun AuthPageLayout(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.auth_back)) }
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(28.dp))
            content()
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: AuthValidationError?,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    helperText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onSubmit: () -> Unit = {},
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val supportingMessage = validationMessage(error, label) ?: helperText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        isError = error != null,
        supportingText = supportingMessage?.let { message -> { Text(message) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onSubmit()
        }),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon
    )
}

@Composable
internal fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    error: AuthValidationError?,
    imeAction: ImeAction = ImeAction.Next,
    helperText: String? = null,
    onSubmit: () -> Unit = {},
    enabled: Boolean = true
) {
    val toggleDescription = stringResource(
        if (visible) R.string.auth_hide_field else R.string.auth_show_field, label
    )
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        helperText = helperText,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(
                onClick = onToggleVisibility,
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = toggleDescription }
            ) {
                Text(stringResource(if (visible) R.string.auth_hide else R.string.auth_show))
            }
        },
        onSubmit = onSubmit
    )
}

@Composable
private fun validationMessage(error: AuthValidationError?, label: String): String? = when (error) {
    null -> null
    AuthValidationError.REQUIRED -> stringResource(R.string.auth_required, label)
    AuthValidationError.INVALID_EMAIL -> stringResource(R.string.auth_invalid_email)
    AuthValidationError.USERNAME_LENGTH -> stringResource(R.string.auth_username_length)
    AuthValidationError.USERNAME_WHITESPACE -> stringResource(R.string.auth_username_whitespace)
    AuthValidationError.PASSWORD_LENGTH -> stringResource(R.string.auth_password_length)
    AuthValidationError.PASSWORD_MISMATCH -> stringResource(R.string.auth_password_mismatch)
}
