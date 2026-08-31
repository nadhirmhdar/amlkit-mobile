package com.amlkit.mobile.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.CustomerDetailResponse
import com.amlkit.mobile.data.dto.SignatureRequest
import com.amlkit.mobile.data.dto.TransactionRequest
import com.amlkit.mobile.data.dto.UboAddRequest
import com.amlkit.mobile.ui.common.AmlDialogFieldShape
import com.amlkit.mobile.ui.common.AmlDialogShape
import com.amlkit.mobile.ui.common.CategoryTag
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.FullScreenLoading
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillButtonTone
import com.amlkit.mobile.ui.common.Resource
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.StatusPill
import com.amlkit.mobile.ui.common.alertStatusTone
import com.amlkit.mobile.ui.common.amlDialogFieldColors
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.categoryTone
import com.amlkit.mobile.ui.common.dotColor
import com.amlkit.mobile.ui.common.riskTone
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlDanger
import com.amlkit.mobile.ui.theme.AmlGood
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlWarn
import com.amlkit.mobile.ui.theme.AmlkitMonoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CustomerDetailViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow<Resource<CustomerDetailResponse>>(Resource.Loading)
    val state: StateFlow<Resource<CustomerDetailResponse>> = _state

    private var customerId: Int = -1
    private var actionInFlight = false

    fun load(id: Int) {
        customerId = id
        _state.value = Resource.Loading
        viewModelScope.launch {
            fetch()
        }
    }

    private suspend fun fetch() {
        when (val result = repository.customerDetail(customerId)) {
            is ApiResult.Success -> _state.value = Resource.Content(result.data)
            is ApiResult.Failure -> _state.value = Resource.Error(result.message)
        }
    }

    fun addNote(body: String, onDone: (String?) -> Unit) = runAction(onDone) { repository.addNote(customerId, body) }

    fun addUbo(name: String, pct: Double?, controlType: String, onDone: (String?) -> Unit) =
        runAction(onDone) { repository.addUbo(customerId, UboAddRequest(name, pct, controlType)) }

    fun addTransaction(req: TransactionRequest, onDone: (String?) -> Unit) =
        runAction(onDone) { repository.addTransaction(customerId, req) }

    fun addSignature(req: SignatureRequest, onDone: (String?) -> Unit) =
        runAction(onDone) { repository.addSignature(customerId, req) }

    fun closeRelationship(onDone: (String?) -> Unit) = runAction(onDone) { repository.closeCustomer(customerId) }

    private fun <T> runAction(onDone: (String?) -> Unit, call: suspend () -> ApiResult<T>) {
        if (actionInFlight) return
        actionInFlight = true
        viewModelScope.launch {
            when (val result = call()) {
                is ApiResult.Success -> { fetch(); onDone(null) }
                is ApiResult.Failure -> onDone(result.message)
            }
            actionInFlight = false
        }
    }
}

@Composable
fun CustomerDetailScreen(
    repository: AmlkitRepository,
    customerId: Int,
    onOpenEvidence: () -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { CustomerDetailViewModel(it) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(customerId) { viewModel.load(customerId) }

    when (val current = state) {
        is Resource.Loading -> FullScreenLoading(modifier = Modifier.fillMaxSize())
        is Resource.Error -> ErrorBanner(message = current.message, modifier = Modifier.fillMaxSize())
        is Resource.Content -> CustomerDetailContent(current.data, viewModel, onOpenEvidence)
    }
}

@Composable
private fun SectionHeading(text: String) {
    ScreenEyebrow(text = text, modifier = Modifier.padding(top = 22.dp, bottom = 4.dp))
}

@Composable
private fun CustomerDetailContent(
    data: CustomerDetailResponse,
    viewModel: CustomerDetailViewModel,
    onOpenEvidence: () -> Unit,
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var showUboDialog by remember { mutableStateOf(false) }
    var showTxnDialog by remember { mutableStateOf(false) }
    var showSignDialog by remember { mutableStateOf(false) }
    var showCloseConfirm by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenContentPadding,
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = data.customer.full_name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AmlInk,
                )
                Text(
                    text = data.customer.reference,
                    style = AmlkitMonoStyle,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = AmlInk3,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val tone = riskTone(data.risk?.rating)
                    Spacer(modifier = Modifier.size(5.dp).background(tone.dotColor(), CircleShape))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "${data.risk?.rating ?: "unrated"} risk".uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = tone.dotColor(),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Status: ${data.customer.status}${data.risk?.next_review?.let { " · review due $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmlInk3,
                    )
                }
                HairlineDivider(modifier = Modifier.padding(top = 16.dp))
            }
        }
        if (actionError != null) {
            item { ErrorBanner(message = actionError!!) }
        }

        item { SectionHeading("Risk assessment") }
        item {
            Column {
                LabelValueRow("Score", data.risk?.score?.toString() ?: "—")
                LabelValueRow("Enhanced due diligence", if (data.risk?.requires_edd == 1) "Required" else "Not required")
                LabelValueRow("Next review", data.risk?.next_review ?: "—")
            }
        }

        if (data.alerts.isNotEmpty()) {
            item { SectionHeading("Alerts (${data.alerts.size})") }
            items(data.alerts) { alert ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    CategoryTag(text = alert.category, tone = categoryTone(alert.category), trailing = "%.2f".format(alert.score))
                    Text(
                        text = alert.matched_party ?: alert.caption,
                        style = MaterialTheme.typography.titleMedium,
                        color = AmlInk,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    StatusPill(text = alert.status, tone = alertStatusTone(alert.status), modifier = Modifier.padding(top = 6.dp))
                }
                HairlineDivider(soft = true)
            }
            item {
                Text(
                    "Disposition alerts from the Alerts tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmlInk3,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScreenEyebrow(text = "Beneficial owners")
                Text(text = "+ Add", style = MaterialTheme.typography.labelLarge, color = AmlInk2, modifier = Modifier.clickableAdd { showUboDialog = true })
            }
        }
        items(data.ubos) { ubo ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = ubo.person_name, style = MaterialTheme.typography.titleMedium, color = AmlInk)
                    Text(
                        text = "${ubo.control_type} · ${if (ubo.is_ubo == 1) "UBO" else "recorded owner"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmlInk3,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = ubo.ownership_pct?.let { "$it%" } ?: "—",
                    style = AmlkitMonoStyle,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = AmlInk2,
                )
            }
            HairlineDivider(soft = true)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScreenEyebrow(text = "Recent activity")
                Text(text = "+ Add", style = MaterialTheme.typography.labelLarge, color = AmlInk2, modifier = Modifier.clickableAdd { showTxnDialog = true })
            }
        }
        items(data.transactions.take(10)) { txn ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.size(5.dp).background(if (txn.direction == "in") AmlGood else AmlWarn, CircleShape))
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${txn.method} ${txn.direction}", style = MaterialTheme.typography.titleSmall, color = AmlInk)
                    Text(text = txn.occurred_at, style = MaterialTheme.typography.bodySmall, color = AmlInk3, modifier = Modifier.padding(top = 2.dp))
                }
                Text(text = "${txn.amount} ${txn.currency}", style = AmlkitMonoStyle, fontSize = MaterialTheme.typography.bodyMedium.fontSize, color = AmlInk2)
            }
            HairlineDivider(soft = true)
        }
        if (data.transaction_alerts.isNotEmpty()) {
            item { SectionHeading("Transaction-monitoring alerts") }
            item {
                Column {
                    data.transaction_alerts.forEach { t ->
                        StatusPill(text = "${t.rule_key} (${t.severity})", tone = com.amlkit.mobile.ui.common.PillTone.WARNING, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScreenEyebrow(text = "Case notes")
                Text(text = "+ Add", style = MaterialTheme.typography.labelLarge, color = AmlInk2, modifier = Modifier.clickableAdd { showNoteDialog = true })
            }
        }
        items(data.notes) { note ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(text = note.body, style = MaterialTheme.typography.bodyMedium, color = AmlInk2)
                Text(text = "${note.author} · ${note.created_at}", style = MaterialTheme.typography.bodySmall, color = AmlInk3, modifier = Modifier.padding(top = 4.dp))
            }
            HairlineDivider(soft = true)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScreenEyebrow(text = "Signatures")
                Text(text = "+ Add", style = MaterialTheme.typography.labelLarge, color = AmlInk2, modifier = Modifier.clickableAdd { showSignDialog = true })
            }
        }
        items(data.signatures) { sig ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(text = sig.purpose, style = MaterialTheme.typography.titleSmall, color = AmlInk)
                Text(
                    text = "Signed by ${sig.signer_name} (${sig.signer_role}) on ${sig.signed_at}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmlInk3,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            HairlineDivider(soft = true)
        }

        item {
            Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(text = "View evidence pack", onClick = onOpenEvidence, tone = PillButtonTone.SECONDARY, modifier = Modifier.fillMaxWidth())
                if (data.customer.status == "active") {
                    PillButton(text = "Close relationship", onClick = { showCloseConfirm = true }, tone = PillButtonTone.DANGER, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showNoteDialog) {
        TextInputDialog(
            title = "Add case note", label = "Note",
            onDismiss = { showNoteDialog = false },
            onConfirm = { text ->
                viewModel.addNote(text) { err -> actionError = err }
                showNoteDialog = false
            },
        )
    }
    if (showUboDialog) {
        UboDialog(onDismiss = { showUboDialog = false }, onConfirm = { name, pct, type ->
            viewModel.addUbo(name, pct, type) { err -> actionError = err }
            showUboDialog = false
        })
    }
    if (showTxnDialog) {
        TransactionDialog(onDismiss = { showTxnDialog = false }, onConfirm = { req ->
            viewModel.addTransaction(req) { err -> actionError = err }
            showTxnDialog = false
        })
    }
    if (showSignDialog) {
        SignatureDialog(onDismiss = { showSignDialog = false }, onConfirm = { req ->
            viewModel.addSignature(req) { err -> actionError = err }
            showSignDialog = false
        })
    }
    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            containerColor = com.amlkit.mobile.ui.theme.AmlSurface,
            shape = AmlDialogShape,
            title = { Text("Close this relationship?", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
            text = { Text("Records are retained for 5 years per Cabinet Res. 134/2025.", color = AmlInk2, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                PillButton(text = "Close relationship", tone = PillButtonTone.DANGER, height = 44.dp, onClick = {
                    viewModel.closeRelationship { err -> actionError = err }
                    showCloseConfirm = false
                })
            },
            dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = { showCloseConfirm = false }) },
        )
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = AmlInk2)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = AmlInk)
    }
    HairlineDivider(soft = true)
}

private fun Modifier.clickableAdd(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
private fun TextInputDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.amlkit.mobile.ui.theme.AmlSurface,
        shape = AmlDialogShape,
        title = { Text(title, color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text(label) },
                colors = amlDialogFieldColors(),
                shape = AmlDialogFieldShape,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { PillButton(text = "Save", height = 44.dp, onClick = { if (text.isNotBlank()) onConfirm(text) }) },
        dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = onDismiss) },
    )
}

@Composable
private fun UboDialog(onDismiss: () -> Unit, onConfirm: (String, Double?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pct by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.amlkit.mobile.ui.theme.AmlSurface,
        shape = AmlDialogShape,
        title = { Text("Add beneficial owner", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pct, onValueChange = { pct = it }, label = { Text("% owned") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            PillButton(text = "Add & screen", height = 44.dp, onClick = { if (name.isNotBlank()) onConfirm(name, pct.toDoubleOrNull(), "ownership") })
        },
        dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = onDismiss) },
    )
}

@Composable
private fun TransactionDialog(onDismiss: () -> Unit, onConfirm: (TransactionRequest) -> Unit) {
    var direction by remember { mutableStateOf("in") }
    var method by remember { mutableStateOf("cash") }
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.amlkit.mobile.ui.theme.AmlSurface,
        shape = AmlDialogShape,
        title = { Text("Record transaction", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = direction, onValueChange = { direction = it }, label = { Text("Direction (in/out)") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method (cash/wire/...)") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (AED)") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            PillButton(text = "Record", height = 44.dp, onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt != null) onConfirm(TransactionRequest(direction, method, amt, "AED", amt))
            })
        },
        dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = onDismiss) },
    )
}

@Composable
private fun SignatureDialog(onDismiss: () -> Unit, onConfirm: (SignatureRequest) -> Unit) {
    var purpose by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf("") }
    var signerName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.amlkit.mobile.ui.theme.AmlSurface,
        shape = AmlDialogShape,
        title = { Text("Record signature", color = AmlInk, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = purpose, onValueChange = { purpose = it }, label = { Text("Purpose") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = statement, onValueChange = { statement = it }, label = { Text("Statement") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = signerName, onValueChange = { signerName = it }, label = { Text("Signer name") }, colors = amlDialogFieldColors(), shape = AmlDialogFieldShape, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            PillButton(text = "Sign", height = 44.dp, onClick = {
                if (purpose.isNotBlank() && statement.isNotBlank() && signerName.isNotBlank()) {
                    onConfirm(SignatureRequest(purpose, statement, signerName))
                }
            })
        },
        dismissButton = { PillButton(text = "Cancel", tone = PillButtonTone.SECONDARY, height = 44.dp, onClick = onDismiss) },
    )
}
