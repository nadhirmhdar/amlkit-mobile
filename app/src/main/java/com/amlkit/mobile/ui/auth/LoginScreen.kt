package com.amlkit.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.GrovisorLogo
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.TextLink
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    // Set only when login fails specifically because the account's email
    // was never verified (see auth.login()'s guard on the server) -- lets
    // the screen offer a resend instead of just showing a dead-end error.
    val unverifiedEmail: String? = null,
    val resendInFlight: Boolean = false,
    val resendMessage: String? = null,
)

class LoginViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmailChange(value: String) { _state.value = _state.value.copy(email = value, error = null) }
    fun onPasswordChange(value: String) { _state.value = _state.value.copy(password = value, error = null) }

    fun login(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.value = current.copy(error = "Enter your email and password.")
            return
        }
        _state.value = current.copy(loading = true, error = null, unverifiedEmail = null, resendMessage = null)
        viewModelScope.launch {
            when (val result = repository.login(current.email.trim(), current.password)) {
                is com.amlkit.mobile.data.ApiResult.Success -> {
                    _state.value = _state.value.copy(loading = false)
                    onSuccess()
                }
                is com.amlkit.mobile.data.ApiResult.Failure -> {
                    val unverified = result.message.contains("verify your email", ignoreCase = true)
                    _state.value = _state.value.copy(
                        loading = false,
                        error = result.message,
                        unverifiedEmail = if (unverified) current.email.trim() else null,
                    )
                }
            }
        }
    }

    fun resend() {
        val email = _state.value.unverifiedEmail ?: return
        _state.value = _state.value.copy(resendInFlight = true, resendMessage = null)
        viewModelScope.launch {
            val result = repository.resendVerification(email)
            val message = when (result) {
                is com.amlkit.mobile.data.ApiResult.Success -> result.data.message
                is com.amlkit.mobile.data.ApiResult.Failure -> result.message
            }
            _state.value = _state.value.copy(resendInFlight = false, resendMessage = message)
        }
    }
}

@Composable
fun LoginScreen(
    repository: AmlkitRepository,
    onLoggedIn: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToSetup: () -> Unit,
    onGoToVerifyEmail: () -> Unit = {},
    prefillEmail: String? = null,
) {
    val viewModel = amlkitViewModel(repository) { LoginViewModel(it) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(prefillEmail) {
        if (!prefillEmail.isNullOrBlank()) viewModel.onEmailChange(prefillEmail)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) {
            GrovisorLogo(height = 28.dp)
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "amlkit",
                style = com.amlkit.mobile.ui.theme.AmlkitExtraType.wordmark,
                color = AmlInk,
            )
            Text(
                text = "UAE sanctions screening & CDD",
                style = MaterialTheme.typography.bodyMedium,
                color = AmlInk3,
                modifier = Modifier.padding(top = 10.dp, bottom = 36.dp),
            )

            if (state.error != null) {
                ErrorBanner(message = state.error!!, modifier = Modifier.padding(bottom = 12.dp))
            }
            if (state.unverifiedEmail != null) {
                if (state.resendMessage != null) {
                    Text(
                        text = state.resendMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmlInk3,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                } else {
                    TextLink(
                        text = if (state.resendInFlight) "Sending…" else "Resend verification email",
                        onClick = viewModel::resend,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            UnderlineField(
                label = "Email",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.padding(bottom = 22.dp),
            )
            UnderlineField(
                label = "Password",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                keyboardType = KeyboardType.Password,
                password = true,
                modifier = Modifier.padding(bottom = 30.dp),
            )

            PillButton(
                text = "Sign in",
                onClick = { viewModel.login(onLoggedIn) },
                enabled = !state.loading,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )

            TextLink(
                text = "Register a new organization",
                onClick = onGoToRegister,
                modifier = Modifier.padding(top = 16.dp),
            )
            TextLink(
                text = "Have a setup code?",
                onClick = onGoToSetup,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextLink(
                text = "Have a verification link or code?",
                onClick = onGoToVerifyEmail,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        androidx.compose.material3.HorizontalDivider(color = AmlLine)
        Text(
            text = "A product by Grovisor Business Consultants LLC.",
            style = MaterialTheme.typography.bodySmall,
            color = AmlInk3,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun UnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = AmlInk3,
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AmlInk, fontWeight = FontWeight.Normal),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = when {
                !password -> VisualTransformation.None
                passwordVisible -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            trailingIcon = if (password) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = AmlInk3,
                        )
                    }
                }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = AmlInk,
                unfocusedIndicatorColor = AmlLine,
                cursorColor = AmlInk,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
