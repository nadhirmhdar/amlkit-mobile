package com.amlkit.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.AlertDto
import com.amlkit.mobile.data.dto.DashboardResponse
import com.amlkit.mobile.ui.common.CategoryTag
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.FullScreenLoading
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.common.amlCornerGlow
import com.amlkit.mobile.ui.common.PillTone
import com.amlkit.mobile.ui.common.Resource
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.SectionCard
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.categoryTone
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlGood
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlInkGradientCorner
import com.amlkit.mobile.ui.theme.AmlkitExtraType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class DashboardViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow<Resource<DashboardResponse>>(Resource.Loading)
    val state: StateFlow<Resource<DashboardResponse>> = _state

    fun load() {
        _state.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.dashboard()) {
                is ApiResult.Success -> _state.value = Resource.Content(result.data)
                is ApiResult.Failure -> _state.value = Resource.Error(result.message)
            }
        }
    }
}

@Composable
fun DashboardScreen(
    repository: AmlkitRepository,
    onOpenAlert: (Int) -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { DashboardViewModel(it) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    when (val current = state) {
        is Resource.Loading -> FullScreenLoading(modifier = Modifier.fillMaxSize())
        is Resource.Error -> ErrorBanner(message = current.message, modifier = Modifier.fillMaxSize())
        is Resource.Content -> DashboardContent(current.data, onOpenAlert)
    }
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
private fun DashboardContent(data: DashboardResponse, onOpenAlert: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenContentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                ScreenEyebrow(text = "Workspace")
                Text(text = "Overview", style = MaterialTheme.typography.displaySmall, color = AmlInk, modifier = Modifier.padding(top = 2.dp))
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.size(6.dp).background(if (data.compliant) AmlGood else com.amlkit.mobile.ui.theme.AmlDanger, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (data.compliant) "Lists refreshed within the 24-hour window" else "List refresh breach detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmlInk2,
                    )
                }
            }
        }

        item {
            val oldestDays = daysAgo(data.oldest_open.firstOrNull()?.created_at)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AmlInk)
                    .amlCornerGlow(listOf(AmlInkGradientCorner.copy(alpha = 0.85f), Color.Transparent))
                    .padding(horizontal = 24.dp, vertical = 22.dp),
            ) {
                Text(
                    text = "NEEDS A DECISION",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.55f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = data.counts.alerts_total.toString(), style = AmlkitExtraType.heroNumber, color = Color.White)
                    Text(
                        text = "open alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (oldestDays != null) "Oldest is $oldestDays day${if (oldestDays == 1) "" else "s"} old" else "No open alerts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(text = "Review →", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(top = 22.dp)) {
                listOf(
                    "Screened" to data.counts.screenings,
                    "Customers on file" to data.counts.customers,
                    "High risk customers" to data.high_risk_customers,
                    "Total alerts" to data.counts.alerts_total,
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = AmlInk2)
                        Text(text = value.toString(), style = MaterialTheme.typography.titleLarge, color = AmlInk)
                    }
                    HairlineDivider(soft = true)
                }
            }
        }

        if (data.breaches.isNotEmpty()) {
            item {
                SectionCard(title = "Lists past the 24-hour refresh window", modifier = Modifier.padding(top = 16.dp)) {
                    data.breaches.forEach { d ->
                        Text(
                            "${d.title} — ${d.hours_since_refresh?.let { "${it}h" } ?: "never refreshed"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmlInk2,
                        )
                    }
                }
            }
        }

        if (data.oldest_open.isNotEmpty()) {
            item { ScreenEyebrow(text = "Oldest open alerts", modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)) }
            items(data.oldest_open) { alert -> DashboardAlertRow(alert, onOpenAlert) }
        }

        if (data.due_for_review.isNotEmpty()) {
            item {
                SectionCard(title = "Due for periodic review", modifier = Modifier.padding(top = 16.dp)) {
                    data.due_for_review.forEach { c ->
                        Text(
                            "${c.full_name} (${c.reference}) — next review ${c.next_review ?: "—"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmlInk2,
                        )
                    }
                }
            }
        }

        if (data.open_transaction_alerts.isNotEmpty()) {
            item {
                SectionCard(title = "Open transaction-monitoring alerts", modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                    data.open_transaction_alerts.take(5).forEach { t ->
                        Text(
                            "${t.rule_key} — ${t.customer_name ?: t.reference ?: "customer #${t.customer_id}"} (${t.severity})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmlInk2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardAlertRow(alert: AlertDto, onOpenAlert: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAlert(alert.id) }
            .padding(vertical = 14.dp),
    ) {
        CategoryTag(text = alert.category, tone = categoryTone(alert.category), trailing = "%.2f".format(alert.score))
        Text(
            text = alert.matched_party ?: alert.caption,
            style = MaterialTheme.typography.titleMedium,
            color = AmlInk,
            modifier = Modifier.padding(top = 5.dp),
        )
        Text(
            text = "Matched: ${alert.caption} · ${alert.dataset_title ?: alert.dataset ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = AmlInk3,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
    HairlineDivider(soft = true)
}
