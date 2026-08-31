package com.amlkit.mobile.ui.alerts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.amlkit.mobile.data.dto.ReviewDto
import com.amlkit.mobile.ui.common.CategoryTag
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.FullScreenLoading
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillButtonTone
import com.amlkit.mobile.ui.common.PillButtonWeighted
import com.amlkit.mobile.ui.common.SectionCard
import com.amlkit.mobile.ui.common.StatusPill
import com.amlkit.mobile.ui.common.TextLink
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.Resource
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.alertStatusTone
import com.amlkit.mobile.ui.common.categoryTone
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.nav.AmlkitSubHeader
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlSurface
import com.amlkit.mobile.ui.theme.AmlkitMonoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

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
fun AlertsScreen(repository: AmlkitRepository, onBack: () -> Unit) {
    val viewModel = amlkitViewModel(repository) { AlertsViewModel(it) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    var selectedAlert by remember { mutableStateOf<AlertDto?>(null) }
    var assignTarget by remember { mutableStateOf<AlertDto?>(null) }

    BackHandler(enabled = selectedAlert != null) { selectedAlert = null }

    Column(modifier = Modifier.fillMaxSize()) {
        val current = selectedAlert
        if (current == null) {
            AmlkitSubHeader(label = "Home", onBack = onBack)
            AlertsQueue(
                state = state,
                onSelectStatus = viewModel::setStatus,
                onOpenAlert = { alert -> selectedAlert = alert },
                onAssign = { alert -> assignTarget = alert },
            )
        } else {
            AmlkitSubHeader(label = "Alerts", onBack = { selectedAlert = null })
            AlertDetail(
                alert = current,
                reasonCodes = state.reasonCodes,
                onDisposition = { status, reasonCode, narrative ->
                    viewModel.disposition(current.id, status, reasonCode, narrative)
                    selectedAlert = null
                },
                onConfirm = { agree, narrative ->
                    viewModel.confirm(current.id, agree, narrative)
                    selectedAlert = null
                },
            )
        }
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
private fun AlertsQueue(
    state: AlertsUiState,
    onSelectStatus: (String) -> Unit,
    onOpenAlert: (AlertDto) -> Unit,
    onAssign: (AlertDto) -> Unit,
) {
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
                StatusTab(label = label, selected = state.status == value, onClick = { onSelectStatus(value) })
            }
        }
        HairlineDivider()
    }

    if (state.actionMessage != null) {
        Text(
            text = state.actionMessage,
            color = com.amlkit.mobile.ui.theme.AmlGood,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    if (state.actionError != null) {
        ErrorBanner(message = state.actionError)
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
                            onOpen = { onOpenAlert(alert) },
                            onAssign = { onAssign(alert) },
                        )
                    }
                }
            }
        }
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
private fun AlertRow(alert: AlertDto, onOpen: () -> Unit, onAssign: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 16.dp)) {
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
            PillButtonWeighted(
                text = "Assign",
                onClick = onAssign,
                tone = PillButtonTone.SECONDARY,
            )
            PillButtonWeighted(
                text = if (alert.status == "pending_review") "Confirm review" else "View",
                onClick = onOpen,
            )
        }
    }
    HairlineDivider(soft = true)
}

private fun daysAgo(iso: String?): Int? {
    if (iso == null) return null
    return try {
        Duration.between(Instant.parse(iso), Instant.now()).toDays().toInt().coerceAtLeast(0)
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelMedium, color = AmlInk3)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = AmlInk, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The full "Alert · disposition" page from the mockup -- replaces the old
 * compact AlertDialog with a real page: stats, obligation, the evidence
 * behind the match, a case note, and status-appropriate actions at the
 * bottom (open alerts get dismiss/confirm/escalate; alerts already sent for
 * independent review get agree/override; anything already closed is shown
 * read-only with its disposition history). */
@Composable
private fun AlertDetail(
    alert: AlertDto,
    reasonCodes: Map<String, String>,
    onDisposition: (status: String, reasonCode: String, narrative: String) -> Unit,
    onConfirm: (agree: Boolean, narrative: String) -> Unit,
) {
    var narrative by remember(alert.id) { mutableStateOf("") }
    var reasonCode by remember(alert.id) { mutableStateOf(reasonCodes.keys.firstOrNull() ?: "") }
    var reasonMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        ScreenEyebrow(text = "Alert · disposition")
        CategoryTag(
            text = alert.category,
            tone = categoryTone(alert.category),
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = alert.matched_party ?: alert.caption,
            style = MaterialTheme.typography.displaySmall,
            color = AmlInk,
            modifier = Modifier.padding(top = 6.dp),
        )
        val meta = alert.customer_name ?: alert.dataset_title ?: alert.dataset
        if (meta != null) {
            Text(text = meta, style = MaterialTheme.typography.bodyMedium, color = AmlInk3, modifier = Modifier.padding(top = 3.dp))
        }
        StatusPill(text = alert.status.replace("_", " "), tone = alertStatusTone(alert.status), modifier = Modifier.padding(top = 10.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            StatItem(label = "Score", value = "%.2f".format(alert.score), modifier = Modifier.weight(1f))
            val raised = daysAgo(alert.created_at)
            StatItem(label = "Raised", value = if (raised == null) "—" else if (raised == 0) "Today" else "${raised}d ago", modifier = Modifier.weight(1f))
            StatItem(label = "Assigned", value = alert.assigned_to ?: "Unassigned", modifier = Modifier.weight(1f))
        }

        if (alert.obligation != null) {
            SectionCard(title = "Obligation", modifier = Modifier.padding(top = 20.dp)) {
                Text(text = alert.obligation, style = MaterialTheme.typography.bodyMedium, color = AmlInk2)
            }
        }

        SectionCard(title = "Why this matched", modifier = Modifier.padding(top = 16.dp)) {
            EvidenceRow(label = "List", value = alert.dataset_title ?: alert.dataset ?: "—")
            if (alert.aliases.isNotEmpty()) {
                EvidenceRow(label = "Aliases", value = alert.aliases.joinToString(", ") { it.name })
            }
            if (alert.topics.isNotEmpty()) {
                EvidenceRow(label = "Topics", value = alert.topics.joinToString(", "))
            }
            if (alert.programs.isNotEmpty()) {
                EvidenceRow(label = "Programs", value = alert.programs.joinToString(", "))
            }
            if (alert.countries.isNotEmpty()) {
                EvidenceRow(label = "Countries", value = alert.countries.joinToString(", "))
            }
            if (alert.via_ubo) {
                EvidenceRow(label = "Via UBO", value = alert.ubo_name ?: "Beneficial owner")
            }
        }

        when (alert.status) {
            "open" -> {
                SectionCard(title = "Case note", modifier = Modifier.padding(top = 16.dp)) {
                    Box(modifier = Modifier.padding(top = 2.dp).clickable { reasonMenuOpen = true }) {
                        Text(
                            text = "Reason: ${reasonCodes[reasonCode] ?: reasonCode.ifBlank { "Select a reason" }}",
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
                        value = narrative,
                        onValueChange = { narrative = it },
                        label = { Text("Narrative (required for true positive / escalated)") },
                        colors = com.amlkit.mobile.ui.common.amlDialogFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButtonWeighted(
                        text = "Dismiss",
                        tone = PillButtonTone.SECONDARY,
                        onClick = { onDisposition("false_positive", reasonCode, narrative) },
                    )
                    PillButtonWeighted(
                        text = "Confirm match",
                        tone = PillButtonTone.DANGER,
                        onClick = { onDisposition("true_positive", reasonCode, narrative) },
                    )
                }
                TextLink(
                    text = "Escalate to MLRO instead",
                    onClick = { onDisposition("escalated", reasonCode, narrative) },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            "pending_review" -> {
                SectionCard(title = "Independent review", modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Confirming requires a different operator than the one who proposed the disposition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmlInk2,
                    )
                    OutlinedTextField(
                        value = narrative,
                        onValueChange = { narrative = it },
                        label = { Text("Narrative (optional)") },
                        colors = com.amlkit.mobile.ui.common.amlDialogFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButtonWeighted(text = "Agree & confirm", onClick = { onConfirm(true, narrative) })
                    PillButtonWeighted(text = "Override & escalate", tone = PillButtonTone.DANGER, onClick = { onConfirm(false, narrative) })
                }
            }
            else -> {
                SectionCard(title = "Disposition history", modifier = Modifier.padding(top = 16.dp)) {
                    if (alert.dispositioned_by != null) {
                        EvidenceRow(label = "Dispositioned by", value = alert.dispositioned_by)
                    }
                    if (alert.dispositioned_at != null) {
                        EvidenceRow(label = "At", value = alert.dispositioned_at)
                    }
                    if (alert.reviews.isEmpty()) {
                        Text(text = "No independent review recorded yet.", style = MaterialTheme.typography.bodyMedium, color = AmlInk3)
                    } else {
                        alert.reviews.forEach { review -> ReviewRow(review) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EvidenceRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelMedium, color = AmlInk3)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = AmlInk2, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ReviewRow(review: ReviewDto) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = "${review.operator} · ${review.action}${review.reason_label?.let { " ($it)" } ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = AmlInk,
        )
        if (!review.narrative.isNullOrBlank()) {
            Text(text = review.narrative, style = MaterialTheme.typography.bodySmall, color = AmlInk3, modifier = Modifier.padding(top = 2.dp))
        }
    }
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
