package com.amlkit.mobile.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.AdminResponse
import com.amlkit.mobile.data.dto.DatasetDto
import com.amlkit.mobile.data.dto.OperatorRowDto
import com.amlkit.mobile.ui.common.AmlDialogFieldShape
import com.amlkit.mobile.ui.common.AmlDialogShape
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.FullScreenLoading
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillButtonTone
import com.amlkit.mobile.ui.common.PillTone
import com.amlkit.mobile.ui.common.Resource
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.ScreenTitle
import com.amlkit.mobile.ui.common.SectionCard
import com.amlkit.mobile.ui.common.StatusPill
import com.amlkit.mobile.ui.common.amlDialogFieldColors
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlDanger
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlLine
import com.amlkit.mobile.ui.theme.AmlSurface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow<Resource<AdminResponse>>(Resource.Loading)
    val state: StateFlow<Resource<AdminResponse>> = _state
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private val _datasets = MutableStateFlow<List<DatasetDto>>(emptyList())
    val datasets: StateFlow<List<DatasetDto>> = _datasets

    fun load() {
        _state.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.admin()) {
                is ApiResult.Success -> _state.value = Resource.Content(result.data)
                is ApiResult.Failure -> _state.value = Resource.Error(result.message)
            }
        }
        viewModelScope.launch {
            when (val result = repository.datasets()) {
                is ApiResult.Success -> _datasets.value = result.data.datasets
                is ApiResult.Failure -> Unit // the operator/threshold section above is the important half; a dataset-list fetch failure isn't worth its own error banner
            }
        }
    }

    fun setThreshold(value: Double?) {
        viewModelScope.launch {
            when (val result = repository.setThreshold(value)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _message.value = result.message
            }
        }
    }

    fun createOperator(name: String, email: String, password: String, role: String) {
        viewModelScope.launch {
            when (val result = repository.createOperator(name, email, password, role)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _message.value = result.message
            }
        }
    }

    fun resetPassword(operatorId: Int, newPassword: String) {
        viewModelScope.launch {
            when (val result = repository.resetOperatorPassword(operatorId, newPassword)) {
                is ApiResult.Success -> _message.value = "Password reset. All that operator's sessions were signed out."
                is ApiResult.Failure -> _message.value = result.message
            }
        }
    }

    fun deactivateOperator(operatorId: Int) {
        viewModelScope.launch {
            when (val result = repository.deactivateOperator(operatorId)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _message.value = result.message
            }
        }
    }

    fun refreshSanctions() {
        viewModelScope.launch {
            when (val result = repository.refreshSanctions()) {
                is ApiResult.Success -> {
                    _message.value = "Refreshed: " + result.data.loaded.joinToString("; ")
                    load()
                }
                is ApiResult.Failure -> _message.value = result.message
            }
        }
    }

    fun dismissMessage() { _message.value = null }
}

@Composable
fun AdminScreen(repository: AmlkitRepository) {
    val viewModel = amlkitViewModel(repository) { AdminViewModel(it) }
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val datasets by viewModel.datasets.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var showCreateOperator by remember { mutableStateOf(false) }
    var thresholdText by remember { mutableStateOf("") }
    var resetPasswordTarget by remember { mutableStateOf<OperatorRowDto?>(null) }
    var deactivateTarget by remember { mutableStateOf<OperatorRowDto?>(null) }

    when (val current = state) {
        is Resource.Loading -> FullScreenLoading(modifier = Modifier.fillMaxSize())
        is Resource.Error -> ErrorBanner(message = current.message, modifier = Modifier.fillMaxSize())
        is Resource.Content -> {
            val data = current.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = screenContentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column {
                        ScreenEyebrow(text = "More")
                        ScreenTitle(text = data.org.name, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
                    }
                }
                if (message != null) item { ErrorBanner(message = message!!) }

                item {
                    SectionCard(title = "Alert threshold") {
                        Text(text = "Current: ${data.threshold ?: "engine default (${data.default_threshold})"}")
                        OutlinedTextField(
                            value = thresholdText, onValueChange = { thresholdText = it },
                            label = { Text("0.0 – 1.0") }, modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillButton(text = "Save", onClick = { viewModel.setThreshold(thresholdText.toDoubleOrNull()) }, height = 44.dp, modifier = Modifier.weight(1f))
                            PillButton(text = "Reset", onClick = { thresholdText = ""; viewModel.setThreshold(null) }, tone = PillButtonTone.SECONDARY, height = 44.dp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    SectionCard(title = "Sanctions & PEP lists") {
                        data.sanctions.forEach { d ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = d.title, modifier = Modifier.fillMaxWidth())
                                StatusPill(
                                    text = if (d.breach) "BREACH" else "OK",
                                    tone = if (d.breach) PillTone.DANGER else PillTone.SUCCESS,
                                )
                            }
                        }
                        PillButton(text = "Refresh now", onClick = viewModel::refreshSanctions, tone = PillButtonTone.SECONDARY, height = 44.dp)
                    }
                }

                if (datasets.isNotEmpty()) {
                    item {
                        SectionCard(title = "Data sources") {
                            datasets.forEach { ds -> DatasetRow(ds) }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Operators", style = MaterialTheme.typography.titleLarge, color = AmlInk)
                        Text(
                            text = "+ Add",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmlInk,
                            modifier = Modifier.clickable { showCreateOperator = true },
                        )
                    }
                }
                items(data.operators) { op ->
                    SectionCard(title = op.name) {
                        Text(text = "${op.email} · ${op.role} · ${if (op.is_active == 1) "active" else "deactivated"}")
                        if (op.is_active == 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillButton(
                                    text = "Reset password",
                                    onClick = { resetPasswordTarget = op },
                                    tone = PillButtonTone.SECONDARY,
                                    height = 40.dp,
                                    modifier = Modifier.weight(1f),
                                )
                                PillButton(
                                    text = "Deactivate",
                                    onClick = { deactivateTarget = op },
                                    tone = PillButtonTone.DANGER,
                                    height = 40.dp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateOperator) {
        CreateOperatorDialog(
            onDismiss = { showCreateOperator = false },
            onConfirm = { name, email, password, role ->
                viewModel.createOperator(name, email, password, role)
                showCreateOperator = false
            },
        )
    }

    resetPasswordTarget?.let { op ->
        ResetPasswordDialog(
            operatorName = op.name,
            onDismiss = { resetPasswordTarget = null },
            onConfirm = { newPassword ->
                viewModel.resetPassword(op.id, newPassword)
                resetPasswordTarget = null
            },
        )
    }

    deactivateTarget?.let { op ->
        AlertDialog(
            onDismissRequest = { deactivateTarget = null },
            containerColor = AmlSurface,
            shape = AmlDialogShape,
            title = { Text("Deactivate ${op.name}?", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "They will be signed out of every session immediately and won't be able to sign back in.",
                    color = AmlInk2,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                PillButton(
                    text = "Deactivate",
                    tone = PillButtonTone.DANGER,
                    height = 44.dp,
                    onClick = {
                        viewModel.deactivateOperator(op.id)
                        deactivateTarget = null
                    },
                )
            },
            dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = { deactivateTarget = null }) },
        )
    }
}

/** Publisher and licence for one screening data source -- surfaced so an
 * MLRO can see where a match came from and whether its licence carries
 * usage restrictions (a NON-COMMERCIAL licence is a real compliance fact,
 * not a technicality, so it's flagged the same way the web app flags it). */
@Composable
private fun DatasetRow(dataset: DatasetDto) {
    val nonCommercial = dataset.licence?.contains("NON-COMMERCIAL", ignoreCase = true) == true
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = dataset.title, style = MaterialTheme.typography.bodyMedium, color = AmlInk, modifier = Modifier.weight(1f))
            Text(
                text = dataset.entity_count?.let { "%,d".format(it) } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = AmlInk3,
            )
        }
        Text(
            text = "${dataset.publisher ?: "Unknown publisher"} · ${dataset.licence ?: "licence unknown"}",
            style = MaterialTheme.typography.bodySmall,
            color = if (nonCommercial) AmlDanger else AmlInk3,
        )
    }
}

@Composable
private fun CreateOperatorDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("officer") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmlSurface,
        shape = AmlDialogShape,
        title = { Text("Add operator", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (min. 10 chars)") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                Column {
                    Text(text = "ROLE", style = MaterialTheme.typography.labelLarge, color = AmlInk3, modifier = Modifier.padding(bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoleChip(text = "Officer", selected = role == "officer", onClick = { role = "officer" })
                        RoleChip(text = "MLRO", selected = role == "mlro", onClick = { role = "mlro" })
                    }
                }
            }
        },
        confirmButton = {
            PillButton(text = "Create", height = 44.dp, onClick = {
                if (name.isNotBlank() && email.isNotBlank() && password.length >= 10) onConfirm(name, email, password, role)
            })
        },
        dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = onDismiss) },
    )
}

/** Two mutually-exclusive choices, not free text -- the backend only ever
 * accepts "officer" or "mlro" for a role, so the picker shouldn't allow
 * anything else to be typed in the first place. */
@Composable
private fun RoleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) AmlInk else Color.Transparent, shape)
            .then(if (!selected) Modifier.border(BorderStroke(1.dp, AmlLine), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else AmlInk2,
        )
    }
}

@Composable
private fun ResetPasswordDialog(operatorName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmlSurface,
        shape = AmlDialogShape,
        title = { Text("Reset password for $operatorName", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This immediately signs them out of every active session.",
                    color = AmlInk2,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("New password (min. 10 chars)") },
                    colors = amlDialogFieldColors(), shape = AmlDialogFieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            PillButton(text = "Set password", height = 44.dp, onClick = {
                if (newPassword.length >= 10) onConfirm(newPassword)
            })
        },
        dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = onDismiss) },
    )
}
