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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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

/** Pulls the `token` query param out of the full link a verification email
 * contains, so a user can paste either the raw token or the whole URL. */
private fun extractToken(pasted: String): String {
    val trimmed = pasted.trim()
    val marker = "token="
    val index = trimmed.indexOf(marker)
    if (index == -1) return trimmed
    return trimmed.substring(index + marker.length).substringBefore('&')
}

data class VerifyEmailUiState(
    val email: String = "",
    val token: String = "",
    val verifying: Boolean = false,
    val resending: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

class VerifyEmailViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state: StateFlow<VerifyEmailUiState> = _state

    fun setInitial(email: String, prefillToken: String?) {
        _state.value = _state.value.copy(email = email, token = prefillToken ?: _state.value.token)
    }

    fun onEmailChange(value: String) { _state.value = _state.value.copy(email = value, error = null, info = null) }
    fun onTokenChange(value: String) { _state.value = _state.value.copy(token = extractToken(value), error = null, info = null) }

    fun verify(onVerified: () -> Unit) {
        val current = _state.value
        if (current.token.isBlank()) {
            _state.value = current.copy(error = "Paste the verification link or code from your email.")
            return
        }
        _state.value = current.copy(verifying = true, error = null, info = null)
        viewModelScope.launch {
            when (val result = repository.verifyEmail(current.token.trim())) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(verifying = false)
                    onVerified()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(verifying = false, error = result.message)
            }
        }
    }

    fun resend() {
        val current = _state.value
        if (current.email.isBlank()) {
            _state.value = current.copy(error = "Enter the email you registered with.")
            return
        }
        _state.value = current.copy(resending = true, error = null, info = null)
        viewModelScope.launch {
            when (val result = repository.resendVerification(current.email.trim())) {
                is ApiResult.Success -> _state.value = _state.value.copy(resending = false, info = result.data.message)
                is ApiResult.Failure -> _state.value = _state.value.copy(resending = false, error = result.message)
            }
        }
    }
}

@Composable
fun VerifyEmailScreen(
    repository: AmlkitRepository,
    email: String,
    prefillToken: String?,
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { VerifyEmailViewModel(it) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(email, prefillToken) { viewModel.setInitial(email, prefillToken) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Verify your email", style = MaterialTheme.typography.displaySmall, color = AmlInk)
        Text(
            text = if (state.email.isNotBlank()) {
                "We sent a verification link to ${state.email}. Open it on this device, or " +
                    "paste the link (or just the code) below."
            } else {
                "Enter the email you registered with, then paste the verification link " +
                    "(or just the code) from that email."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = AmlInk3,
            modifier = Modifier.padding(bottom = 20.dp, top = 4.dp),
        )

        if (state.error != null) {
            ErrorBanner(message = state.error!!, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (state.info != null) {
            Text(
                text = state.info!!,
                style = MaterialTheme.typography.bodyMedium,
                color = AmlInk3,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        OutlinedTextField(
            value = state.email, onValueChange = viewModel::onEmailChange,
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        OutlinedTextField(
            value = state.token, onValueChange = viewModel::onTokenChange,
            label = { Text("Verification link or code") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        )

        PillButton(
            text = "Verify",
            onClick = { viewModel.verify(onVerified) },
            enabled = !state.verifying && !state.resending,
            loading = state.verifying,
            modifier = Modifier.fillMaxWidth(),
        )

        TextLink(
            text = "Resend verification email",
            onClick = viewModel::resend,
            modifier = Modifier.padding(top = 16.dp),
        )
        TextLink(text = "Back to sign in", onClick = onBackToLogin, modifier = Modifier.padding(top = 8.dp))
    }
}
