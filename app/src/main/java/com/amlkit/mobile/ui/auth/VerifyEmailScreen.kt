package com.amlkit.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

data class VerifyEmailUiState(
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

/** Pulls the token out of a pasted verification link (`.../verify-email?token=XYZ`),
 * or -- since the email also happens to work if just the token past the `?token=`
 * is copied -- falls back to treating the whole trimmed input as the token itself. */
private fun extractToken(input: String): String {
    val trimmed = input.trim()
    val markerIndex = trimmed.indexOf("token=")
    return if (markerIndex >= 0) trimmed.substring(markerIndex + "token=".length).substringBefore('&') else trimmed
}

class VerifyEmailViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state: StateFlow<VerifyEmailUiState> = _state

    fun onInputChange(v: String) { _state.value = _state.value.copy(input = v, error = null) }

    fun verify(onVerified: () -> Unit) {
        val token = extractToken(_state.value.input)
        if (token.isBlank()) {
            _state.value = _state.value.copy(error = "Paste the verification link or code from your email.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = repository.verifyEmail(token)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(loading = false)
                    onVerified()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }
}

/** Manual alternative to opening the emailed link in a browser -- the app has
 * no way to intercept that link directly (it points at whatever domain the
 * organization's own amlkit server runs on, so Android App Links can't be
 * pre-verified for it), so this lets a user who has the link/token in hand
 * finish verification without leaving the app. */
@Composable
fun VerifyEmailScreen(
    repository: AmlkitRepository,
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { VerifyEmailViewModel(it) }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Verify your email", style = MaterialTheme.typography.displaySmall, color = AmlInk)
        Text(
            text = "Paste the verification link from your email, or just the code at the end of it.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmlInk3,
            modifier = Modifier.padding(bottom = 20.dp, top = 4.dp),
        )

        if (state.error != null) {
            ErrorBanner(message = state.error!!, modifier = Modifier.padding(bottom = 12.dp))
        }

        OutlinedTextField(
            value = state.input, onValueChange = viewModel::onInputChange,
            label = { Text("Verification link or code") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        )

        PillButton(
            text = "Verify",
            onClick = { viewModel.verify(onVerified) },
            enabled = !state.loading,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )

        TextLink(text = "Back to sign in", onClick = onBackToLogin, modifier = Modifier.padding(top = 16.dp))
    }
}
