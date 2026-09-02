package com.amlkit.mobile.ui.customers

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.CustomerDetailResponse
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.FullScreenLoading
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillButtonTone
import com.amlkit.mobile.ui.common.Resource
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.ScreenTitle
import com.amlkit.mobile.ui.common.SectionCard
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlInk3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Evidence pack: the same case-file data the web app's printable evidence
 * page renders, shown as a scrollable read-only summary. There is no
 * generated-PDF export here -- see the "Not built yet" list in the repo
 * README -- but the same plain-text-via-share-sheet pattern the goAML
 * export already uses (ReportDetailScreen.shareXml) lets an operator get
 * this off the device to an auditor without waiting on a PDF renderer. */
class EvidenceViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow<Resource<CustomerDetailResponse>>(Resource.Loading)
    val state: StateFlow<Resource<CustomerDetailResponse>> = _state

    fun load(customerId: Int) {
        _state.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.customerEvidence(customerId)) {
                is ApiResult.Success -> _state.value = Resource.Content(result.data)
                is ApiResult.Failure -> _state.value = Resource.Error(result.message)
            }
        }
    }
}

@Composable
fun EvidenceScreen(repository: AmlkitRepository, customerId: Int) {
    val viewModel = amlkitViewModel(repository) { EvidenceViewModel(it) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(customerId) { viewModel.load(customerId) }

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
                    ScreenEyebrow(text = "Evidence pack")
                    ScreenTitle(text = data.customer.full_name, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
                    Text(text = "Generated ${data.generated_at ?: ""}", style = MaterialTheme.typography.bodySmall, color = AmlInk3)
                }
                item {
                    SectionCard(title = "Customer") {
                        Text("Reference: ${data.customer.reference}")
                        Text("Type: ${data.customer.customer_type}")
                        Text("Onboarded: ${data.customer.onboarded_at}")
                        Text("Status: ${data.customer.status}")
                    }
                }
                item {
                    SectionCard(title = "Risk") {
                        Text("Rating: ${data.risk?.rating ?: "—"}   Score: ${data.risk?.score ?: "—"}")
                    }
                }
                if (data.screenings.isNotEmpty()) {
                    item { Text(text = "Screening history", style = MaterialTheme.typography.titleMedium) }
                    items(data.screenings) { s ->
                        Text("${s.run_at} — ${s.trigger} — ${s.hits} hit(s) of ${s.candidates} candidates")
                    }
                }
                if (data.alerts.isNotEmpty()) {
                    item { Text(text = "Alerts & dispositions", style = MaterialTheme.typography.titleMedium) }
                    items(data.alerts) { a ->
                        SectionCard(title = a.caption) {
                            Text("Status: ${a.status}   Reason: ${a.reason_code ?: "—"}")
                            Text("Independent review: ${a.independent_review ?: "n/a"}")
                        }
                    }
                }
                if (data.audit.isNotEmpty()) {
                    item { Text(text = "Audit trail", style = MaterialTheme.typography.titleMedium) }
                    items(data.audit.take(50)) { entry ->
                        Text("${entry.created_at} — ${entry.actor} — ${entry.action}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    PillButton(
                        text = "Share evidence pack",
                        tone = PillButtonTone.SECONDARY,
                        onClick = { shareEvidenceText(context, buildEvidenceText(data)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun buildEvidenceText(data: CustomerDetailResponse): String = buildString {
    appendLine("Evidence pack — ${data.customer.full_name}")
    appendLine("Generated ${data.generated_at ?: ""}")
    appendLine()
    appendLine("Customer")
    appendLine("Reference: ${data.customer.reference}")
    appendLine("Type: ${data.customer.customer_type}")
    appendLine("Onboarded: ${data.customer.onboarded_at}")
    appendLine("Status: ${data.customer.status}")
    appendLine()
    appendLine("Risk")
    appendLine("Rating: ${data.risk?.rating ?: "—"}   Score: ${data.risk?.score ?: "—"}")
    if (data.screenings.isNotEmpty()) {
        appendLine()
        appendLine("Screening history")
        data.screenings.forEach { s ->
            appendLine("${s.run_at} — ${s.trigger} — ${s.hits} hit(s) of ${s.candidates} candidates")
        }
    }
    if (data.alerts.isNotEmpty()) {
        appendLine()
        appendLine("Alerts & dispositions")
        data.alerts.forEach { a ->
            appendLine("${a.caption} — Status: ${a.status}   Reason: ${a.reason_code ?: "—"}")
            appendLine("  Independent review: ${a.independent_review ?: "n/a"}")
        }
    }
    if (data.audit.isNotEmpty()) {
        appendLine()
        appendLine("Audit trail")
        data.audit.take(50).forEach { entry ->
            appendLine("${entry.created_at} — ${entry.actor} — ${entry.action}")
        }
    }
}

private fun shareEvidenceText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share evidence pack"))
}
