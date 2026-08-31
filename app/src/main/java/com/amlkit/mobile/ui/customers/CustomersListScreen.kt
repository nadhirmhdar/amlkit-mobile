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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.CustomerListItemDto
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.FullScreenLoading
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.common.PillTone
import com.amlkit.mobile.ui.common.Resource
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.StatusPill
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.dotColor
import com.amlkit.mobile.ui.common.riskTone
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlkitMonoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CustomersListViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow<Resource<List<CustomerListItemDto>>>(Resource.Loading)
    val state: StateFlow<Resource<List<CustomerListItemDto>>> = _state

    fun load() {
        _state.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.customers()) {
                is ApiResult.Success -> _state.value = Resource.Content(result.data.customers)
                is ApiResult.Failure -> _state.value = Resource.Error(result.message)
            }
        }
    }
}

@Composable
fun CustomersListScreen(
    repository: AmlkitRepository,
    onOpenCustomer: (Int) -> Unit,
    onNewCustomer: () -> Unit,
) {
    val viewModel = amlkitViewModel(repository) { CustomersListViewModel(it) }
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                ScreenEyebrow(text = "Book of business")
                Text(text = "Customers", style = MaterialTheme.typography.displaySmall, color = AmlInk, modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                text = "+ Onboard",
                style = MaterialTheme.typography.labelLarge,
                color = AmlInk,
                modifier = Modifier.clickable(onClick = onNewCustomer),
            )
        }
        HairlineDivider()

        when (val current = state) {
            is Resource.Loading -> FullScreenLoading(modifier = Modifier.fillMaxSize())
            is Resource.Error -> ErrorBanner(message = current.message)
            is Resource.Content -> {
                if (current.data.isEmpty()) {
                    Text(
                        text = "No customers yet. Tap + Onboard to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmlInk3,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = screenContentPadding,
                    ) {
                        items(current.data, key = { it.id }) { customer ->
                            CustomerRow(customer, onClick = { onOpenCustomer(customer.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(customer: CustomerListItemDto, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val tone = riskTone(customer.rating)
            Spacer(
                modifier = Modifier
                    .padding(end = 7.dp)
                    .size(5.dp)
                    .background(tone.dotColor(), androidx.compose.foundation.shape.CircleShape),
            )
            Text(
                text = "${customer.rating ?: "unrated"} risk".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = tone.dotColor(),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(text = customer.reference, style = AmlkitMonoStyle, fontSize = MaterialTheme.typography.bodySmall.fontSize, color = AmlInk3)
        }
        Text(text = customer.full_name, style = MaterialTheme.typography.titleMedium, color = AmlInk, modifier = Modifier.padding(top = 6.dp))
        Text(
            text = "${customer.customer_type} · ${customer.status}",
            style = MaterialTheme.typography.bodySmall,
            color = AmlInk3,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (customer.open_alerts > 0 || customer.review_overdue) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (customer.open_alerts > 0) {
                    StatusPill(text = "${customer.open_alerts} open alert(s)", tone = PillTone.DANGER)
                }
                if (customer.review_overdue) {
                    StatusPill(text = "review overdue", tone = PillTone.WARNING)
                }
            }
        }
    }
    HairlineDivider(soft = true)
}
