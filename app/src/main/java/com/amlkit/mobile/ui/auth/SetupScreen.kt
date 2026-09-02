package com.amlkit.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

private val SETUP_EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

data class SetupUiState(
    val token: String = "",
    val checking: Boolean = false,
    val tokenValid: Boolean = false,
    val orgName: String? = null,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

/** One-time claim-invite flow for the setup link amlkit prints to the server
 * console the first time it upgrades an existing single-firm database to the
 * multi-tenant model (see the web app's setup.html). The app has no deep-link
 * / App Links handling for that link today, so this screen asks the operator
 * to paste the token in by hand rather than building link-capture
 * infrastructure for a one-off, low-traffic flow. */
class SetupViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state

    fun onTokenChange(value: String) { _state.value = _state.value.copy(token = value, error = null) }
    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, error = null) }
    fun onEmailChange(value: String) { _state.value = _state.value.copy(email = value, error = null) }
    fun onPasswordChange(value: String) { _state.value = _state.value.copy(password = value, error = null) }

    fun checkToken() {
        val current = _state.value
        if (current.token.isBlank()) {
            _state.value = current.copy(error = "Enter the setup code from the server console.")
            return
        }
        _state.value = current.copy(checking = true, error = null)
        viewModelScope.launch {
            when (val result = repository.setupCheck(current.token.trim())) {
                is ApiResult.Success -> {
                    if (result.data.valid) {
                        _state.value = _state.value.copy(checking = false, tokenValid = true, orgName = result.data.org_name)
                    } else {
                        _state.value = _state.value.copy(
                            checking = false,
                            error = "This setup link is invalid or has already been used.",
                        )
                    }
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(checking = false, error = result.message)
            }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.name.isBlank() || current.email.isBlank()) {
            _state.value = current.copy(error = "Enter your name and email.")
            return
        }
        if (!SETUP_EMAIL_PATTERN.matches(current.email.trim())) {
            _state.value = current.copy(error = "Enter a valid email address.")
            return
        }
        if (current.password.length < 10) {
            _state.value = current.copy(error = "Password must be at least 10 characters.")
            return
        }
        _state.value = current.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = repository.setupSubmit(
                token = current.token.trim(),
                name = current.name.trim(),
                email = current.email.trim(),
                password = current.password,
            )
            when (result) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(loading = false)
                    onSuccess()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }
}

@Composable
fun SetupScreen(
    repository: AmlkitRepository,
    onSetupComplete: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { SetupViewModel(it) }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Complete setup", style = MaterialTheme.typography.displaySmall, color = AmlInk)

        if (!state.tokenValid) {
            Text(
                text = "This link is printed once, to the console, the first time amlkit " +
                    "upgrades an existing single-firm database to the multi-tenant model. " +
                    "Paste the code here to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = AmlInk3,
                modifier = Modifier.padding(bottom = 20.dp, top = 4.dp),
            )

            if (state.error != null) {
                ErrorBanner(message = state.error!!, modifier = Modifier.padding(bottom = 12.dp))
            }

            OutlinedTextField(
                value = state.token, onValueChange = viewModel::onTokenChange,
                label = { Text("Setup code") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            )

            PillButton(
                text = "Continue",
                onClick = viewModel::checkToken,
                enabled = !state.checking,
                loading = state.checking,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = "Creating the first login for ${state.orgName ?: "your organization"}. " +
                    "This link can only be used once.",
                style = MaterialTheme.typography.bodyMedium,
                color = AmlInk3,
                modifier = Modifier.padding(bottom = 20.dp, top = 4.dp),
            )

            if (state.error != null) {
                ErrorBanner(message = state.error!!, modifier = Modifier.padding(bottom = 12.dp))
            }

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
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            )

            PillButton(
                text = "Create login & sign in",
                onClick = { viewModel.submit(onSetupComplete) },
                enabled = !state.loading,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "This account is created with the MLRO role, so it can manage other operators afterward.",
                style = MaterialTheme.typography.bodySmall,
                color = AmlInk3,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        TextLink(text = "Back to sign in", onClick = onBackToLogin, modifier = Modifier.padding(top = 16.dp))
    }
}
