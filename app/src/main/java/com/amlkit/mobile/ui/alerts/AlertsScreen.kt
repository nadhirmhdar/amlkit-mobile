package com.amlkit.mobile.ui.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.AlertDto
import com.amlkit.mobile.ui.common.CategoryTag
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.FullScreenLoading
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillButtonTone
import com.amlkit.mobile.ui.common.PillButtonWeighted
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.Resource
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.categoryTone
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlLine
import com.amlkit.mobile.ui.theme.AmlSurface
import com.amlkit.mobile.ui.theme.AmlkitMonoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AlertsUiState(
    val status: String = "open",
    val alerts: Resource<List<AlertDto>> = Resource.Loading,
    val reasonCodes: Map<String, String> = emptyMap(),
    val actionError: String? = null,
    val actionMessage: String? = null,
)

class AlertsViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state

    init {
        viewModelScope.launch {
            when (val result = repository.reasonCodes()) {
                is ApiResult.Success -> _state.value = _state.value.copy(reasonCodes = result.data.reason_codes)
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun setStatus(status: String) {
        _state.value = _state.value.copy(status = status)
        load()
    }

    fun load() {
        _state.value = _state.value.copy(alerts = Resource.Loading)
        viewModelScope.launch {
            when (val result = repository.alerts(_state.value.status)) {
                is ApiResult.Success -> _state.value = _state.value.copy(alerts = Resource.Content(result.data.alerts))
                is ApiResult.Failure -> _state.value = _state.value.copy(alerts = Resource.Error(result.message))
            }
        }
    }

    fun disposition(alertId: Int, status: String, reasonCode: String, narrative: String) {
        viewModelScope.launch {
            when (val result = repository.dispositionAlert(alertId, status, reasonCode, narrative)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(actionMessage = result.data.message, actionError = null)
                    load()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(actionError = result.message)
            }
        }
    }

    fun confirm(alertId: Int, agree: Boolean, narrative: String) {
        viewModelScope.launch {
            when (val result = repository.confirmAlert(alertId, agree, narrative)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(actionMessage = result.data.message, actionError = null)
                    load()
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(actionError = result.message)
            }
        }
    }

    fun assign(alertId: Int, operator: String?) {
        viewModelScope.launch {
            when (val result = repository.assignAlert(alertId, operator)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _state.value = _state.value.copy(actionError = result.message)
            }
        }
    }

    fun dismissBanner() { _state.value = _state.value.copy(actionMessage = null, actionError = null) }
}

@Composable
fun AlertsScreen(repository: AmlkitRepository) {
    val viewModel = amlkitViewModel(repository) { AlertsViewModel(it) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var dispositionTarget by remember { mutableStateOf<AlertDto?>(null) }
    var confirmTarget by remember { mutableStateOf<AlertDto?>(null) }
    var assignTarget by remember { mutableStateOf<AlertDto?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    ScreenEyebrow(text = "Queue")
                    Text(text = "Alerts", style = MaterialTheme.typography.displaySmall, color = AmlInk, modifier = Modifier.padding(top = 2.dp))
                }
                val openCount = (state.alerts as? Resource.Content)?.data?.size ?: 0
                Text(
                    text = "$openCount open",
                    style = AmlkitMonoStyle,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = AmlInk3,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState())) {
                listOf(
                    "open" to "Open",
                    "pending_review" to "Pending review",
                    "escalated" to "Escalated",
                    "false_positive" to "Cleared",
                    "all" to "All",
                ).forEach { (value, label) ->
                    StatusTab(label = label, selected = state.status == value, onClick = { viewModel.setStatus(value) })
                }
            }
            HairlineDivider()
        }

        if (state.actionMessage != null) {
            Text(
                text = state.actionMessage!!,
                color = com.amlkit.mobile.ui.theme.AmlGood,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        if (state.actionError != null) {
            ErrorBanner(message = state.actionError!!)
        }

        when (val resource = state.alerts) {
            is Resource.Loading -> FullScreenLoading(modifier = Modifier.fillMaxSize())
            is Resource.Error -> ErrorBanner(message = resource.message, modifier = Modifier.fillMaxSize())
            is Resource.Content -> {
                if (resource.data.isEmpty()) {
                    Text(
                        text = "No alerts in this view.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmlInk3,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = screenContentPadding,
                    ) {
                        items(resource.data, key = { it.id }) { alert ->
                            AlertRow(
                                alert = alert,
                                onDisposition = { dispositionTarget = alert },
                                onConfirm = { confirmTarget = alert },
                                onAssign = { assignTarget = alert },
                            )
                        }
                    }
                }
            }
        }
    }

    dispositionTarget?.let { alert ->
        DispositionDialog(
            reasonCodes = state.reasonCodes,
            onDismiss = { dispositionTarget = null },
            onConfirm = { status, code, narrative ->
                viewModel.disposition(alert.id, status, code, narrative)
                dispositionTarget = null
            },
        )
    }
    confirmTarget?.let { alert ->
        ConfirmReviewDialog(
            onDismiss = { confirmTarget = null },
            onConfirm = { agree, narrative ->
                viewModel.confirm(alert.id, agree, narrative)
                confirmTarget = null
            },
        )
    }
    assignTarget?.let { alert ->
        AssignDialog(
            initial = alert.assigned_to ?: "",
            onDismiss = { assignTarget = null },
            onConfirm = { operator ->
                viewModel.assign(alert.id, operator.ifBlank { null })
                assignTarget = null
            },
        )
    }
}

@Composable
private fun StatusTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(end = 22.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) AmlInk else AmlInk3,
            modifier = Modifier.padding(bottom = 11.dp),
        )
        Spacer(
            modifier = Modifier
                .height(2.dp)
                .then(if (selected) Modifier.fillMaxWidth() else Modifier)
                .background(if (selected) AmlInk else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
private fun AlertRow(
    alert: AlertDto,
    onDisposition: () -> Unit,
    onConfirm: () -> Unit,
    onAssign: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        CategoryTag(text = alert.category, tone = categoryTone(alert.category), trailing = "%.2f".format(alert.score))
        Text(
            text = alert.matched_party ?: alert.caption,
            style = MaterialTheme.typography.titleMedium,
            color = AmlInk,
            modifier = Modifier.padding(top = 6.dp),
        )
        val meta = buildString {
            append(alert.customer_name?.let { "${it}" } ?: alert.dataset_title ?: alert.dataset ?: "")
            if (alert.reference != null) append(" · ${alert.reference}")
        }
        if (meta.isNotBlank()) {
            Text(text = meta, style = MaterialTheme.typography.bodySmall, color = AmlInk3, modifier = Modifier.padding(top = 3.dp))
        }
        Text(
            text = "Assigned: ${alert.assigned_to ?: "unassigned"}",
            style = MaterialTheme.typography.bodySmall,
            color = AmlInk3,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButtonWeighted(text = "Assign", onClick = onAssign, tone = PillButtonTone.SECONDARY)
            when (alert.status) {
                "pending_review" -> PillButtonWeighted(text = "Confirm review", onClick = onConfirm)
                "open" -> PillButtonWeighted(text = "Disposition", onClick = onDisposition)
            }
        }
    }
    HairlineDivider(soft = true)
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) AmlInk else androidx.compose.ui.graphics.Color.Transparent, shape)
            .then(if (!selected) Modifier.border(BorderStroke(1.dp, AmlLine), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) androidx.compose.ui.graphics.Color.White else AmlInk2,
        )
    }
}

@Composable
private fun DispositionDialog(
    reasonCodes: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirm: (status: String, reasonCode: String, narrative: String) -> Unit,
) {
    var status by remember { mutableStateOf("false_positive") }
    var reasonCode by remember { mutableStateOf(reasonCodes.keys.firstOrNull() ?: "") }
    var reasonMenuOpen by remember { mutableStateOf(false) }
    var narrative by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmlSurface,
        shape = com.amlkit.mobile.ui.common.AmlDialogShape,
        title = { Text("Disposition alert", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("false_positive", "true_positive", "escalated").forEach { option ->
                        ChoiceChip(text = option.replace("_", " "), selected = status == option, onClick = { status = option })
                    }
                }
                Box(modifier = Modifier.padding(top = 14.dp).clickable { reasonMenuOpen = true }) {
                    Text(
                        text = "Reason: ${reasonCodes[reasonCode] ?: reasonCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmlInk2,
                    )
                }
                DropdownMenu(expanded = reasonMenuOpen, onDismissRequest = { reasonMenuOpen = false }) {
                    reasonCodes.forEach { (code, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { reasonCode = code; reasonMenuOpen = false })
                    }
                }
                OutlinedTextField(
                    value = narrative, onValueChange = { narrative = it },
                    label = { Text("Narrative (required for true positive / escalated)") },
                    colors = com.amlkit.mobile.ui.common.amlDialogFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
                )
            }
        },
        confirmButton = { PillButton(text = "Submit", onClick = { onConfirm(status, reasonCode, narrative) }, height = 44.dp) },
        dismissButton = { PillButton(text = "Cancel", onClick = onDismiss, tone = PillButtonTone.SECONDARY, height = 44.dp) },
    )
}

@Composable
private fun ConfirmReviewDialog(onDismiss: () -> Unit, onConfirm: (agree: Boolean, narrative: String) -> Unit) {
    var narrative by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmlSurface,
        shape = com.amlkit.mobile.ui.common.AmlDialogShape,
        title = { Text("Independent review", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Confirming requires a different operator than the one who proposed the disposition.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmlInk2,
                )
                OutlinedTextField(
                    value = narrative, onValueChange = { narrative = it },
                    label = { Text("Narrative (optional)") },
                    colors = com.amlkit.mobile.ui.common.amlDialogFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
                )
            }
        },
        confirmButton = { PillButton(text = "Agree & confirm", onClick = { onConfirm(true, narrative) }, height = 44.dp) },
        dismissButton = { PillButton(text = "Override & escalate", onClick = { onConfirm(false, narrative) }, tone = PillButtonTone.DANGER, height = 44.dp) },
    )
}

@Composable
private fun AssignDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var operator by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmlSurface,
        shape = com.amlkit.mobile.ui.common.AmlDialogShape,
        title = { Text("Assign alert", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = operator, onValueChange = { operator = it },
                label = { Text("Operator name (blank to clear)") },
                colors = com.amlkit.mobile.ui.common.amlDialogFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { PillButton(text = "Save", onClick = { onConfirm(operator) }, height = 44.dp) },
        dismissButton = { PillButton(text = "Cancel", onClick = onDismiss, tone = PillButtonTone.SECONDARY, height = 44.dp) },
    )
}
