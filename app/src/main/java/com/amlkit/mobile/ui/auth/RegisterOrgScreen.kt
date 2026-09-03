package com.amlkit.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.TextLink
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegisterOrgUiState(
    val orgName: String = "",
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    // Non-null once registration succeeds: the account exists but has no
    // usable session yet (see AmlkitRepository.registerOrganization) until
    // the link mailed to this address is opened, so the screen switches to
    // a "check your email" panel instead of navigating anywhere.
    val registeredEmail: String? = null,
    val resendInFlight: Boolean = false,
    val resendMessage: String? = null,
)

class RegisterOrgViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterOrgUiState())
    val state: StateFlow<RegisterOrgUiState> = _state

    fun onOrgNameChange(v: String) { _state.value = _state.value.copy(orgName = v, error = null) }
    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, error = null) }
    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }

    fun register() {
        val s = _state.value
        if (s.orgName.isBlank() || s.name.isBlank() || s.email.isBlank()) {
            _state.value = s.copy(error = "All fields are required.")
            return
        }
        if (!EMAIL_PATTERN.matches(s.email.trim())) {
            _state.value = s.copy(error = "Enter a valid email address.")
            return
        }
        if (s.password.length < 10) {
            _state.value = s.copy(error = "Password must be at least 10 characters.")
            return
        }
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = repository.registerOrganization(s.orgName.trim(), s.name.trim(), s.email.trim(), s.password)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(loading = false, registeredEmail = result.data.email)
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }

    fun resend() {
        val email = _state.value.registeredEmail ?: return
        _state.value = _state.value.copy(resendInFlight = true, resendMessage = null)
        viewModelScope.launch {
            val result = repository.resendVerification(email)
            val message = when (result) {
                is ApiResult.Success -> result.data.message
                is ApiResult.Failure -> result.message
            }
            _state.value = _state.value.copy(resendInFlight = false, resendMessage = message)
        }
    }
}

/** Basic shape check (one @, something on both sides, a dot in the domain)
 * -- not a full RFC 5322 validator, just enough to catch an obviously
 * malformed address before it reaches the server. */
private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@Composable
fun RegisterOrgScreen(
    repository: AmlkitRepository,
    onBackToLogin: () -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { RegisterOrgViewModel(it) }
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    if (state.registeredEmail != null) {
        CheckYourEmailPanel(
            email = state.registeredEmail!!,
            resendInFlight = state.resendInFlight,
            resendMessage = state.resendMessage,
            onResend = viewModel::resend,
            onBackToLogin = onBackToLogin,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Register your firm", style = MaterialTheme.typography.displaySmall, color = AmlInk)
        Text(
            text = "You'll be the first MLRO for this organization.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmlInk3,
            modifier = Modifier.padding(bottom = 20.dp, top = 4.dp),
        )

        if (state.error != null) {
            ErrorBanner(message = state.error!!, modifier = Modifier.padding(bottom = 12.dp))
        }

        OutlinedTextField(
            value = state.orgName, onValueChange = viewModel::onOrgNameChange,
            label = { Text("Organization name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        OutlinedTextField(
            value = state.name, onValueChange = viewModel::onNameChange,
            label = { Text("Your name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        OutlinedTextField(
            value = state.email, onValueChange = viewModel::onEmailChange,
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        OutlinedTextField(
            value = state.password, onValueChange = viewModel::onPasswordChange,
            label = { Text("Password (min. 10 characters)") }, singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = AmlInk3,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        )

        PillButton(
            text = "Register",
            onClick = { viewModel.register() },
            enabled = !state.loading,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )

        TextLink(text = "Already have an account? Sign in", onClick = onBackToLogin, modifier = Modifier.padding(top = 16.dp))
    }
}

/** Shown after a successful registration in place of the form. There is no
 * session to navigate into yet -- the account only becomes usable once the
 * emailed link is opened (see /verify-email, handled by the amlkit web app
 * since it works from any browser regardless of whether this app is
 * installed), so this screen's job is just to point the user at their
 * inbox and offer a resend rather than pretending sign-in already happened. */
@Composable
private fun CheckYourEmailPanel(
    email: String,
    resendInFlight: Boolean,
    resendMessage: String?,
    onResend: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Check your email", style = MaterialTheme.typography.displaySmall, color = AmlInk)
        Text(
            text = "We sent a verification link to $email. Open it on any device to " +
                "activate your account, then come back here and sign in.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmlInk3,
            modifier = Modifier.padding(bottom = 20.dp, top = 4.dp),
        )

        if (resendMessage != null) {
            Text(
                text = resendMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = AmlInk3,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        PillButton(
            text = "Resend verification email",
            onClick = onResend,
            enabled = !resendInFlight,
            loading = resendInFlight,
            modifier = Modifier.fillMaxWidth(),
        )

        TextLink(text = "Back to sign in", onClick = onBackToLogin, modifier = Modifier.padding(top = 16.dp))
    }
}
