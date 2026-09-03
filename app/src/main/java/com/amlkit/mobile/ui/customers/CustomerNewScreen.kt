package com.amlkit.mobile.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.amlkit.mobile.data.dto.CustomerCreateRequest
import com.amlkit.mobile.data.dto.UboIn
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.ScreenTitle
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.filterCountries
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DraftUbo(val personName: String = "", val ownershipPct: String = "", val controlType: String = "ownership")

data class CustomerNewUiState(
    val reference: String = "",
    val fullName: String = "",
    val customerType: String = "natural",
    val nationality: String = "",
    val sector: String = "other",
    val ubos: List<DraftUbo> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val blockedWarning: String? = null,
)

class CustomerNewViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(CustomerNewUiState())
    val state: StateFlow<CustomerNewUiState> = _state

    fun onReferenceChange(v: String) { _state.value = _state.value.copy(reference = v) }
    fun onFullNameChange(v: String) { _state.value = _state.value.copy(fullName = v) }
    fun onCustomerTypeChange(v: String) { _state.value = _state.value.copy(customerType = v) }
    fun onNationalityChange(v: String) { _state.value = _state.value.copy(nationality = v) }
    fun onSectorChange(v: String) { _state.value = _state.value.copy(sector = v) }

    fun addUbo() { _state.value = _state.value.copy(ubos = _state.value.ubos + DraftUbo()) }
    fun removeUbo(index: Int) {
        _state.value = _state.value.copy(ubos = _state.value.ubos.toMutableList().apply { removeAt(index) })
    }
    fun updateUbo(index: Int, ubo: DraftUbo) {
        _state.value = _state.value.copy(ubos = _state.value.ubos.toMutableList().apply { set(index, ubo) })
    }

    fun submit(onCreated: (Int) -> Unit) {
        val s = _state.value
        if (s.reference.isBlank() || s.fullName.isBlank()) {
            _state.value = s.copy(error = "Reference and full name are required.")
            return
        }
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            val body = CustomerCreateRequest(
                reference = s.reference.trim(),
                full_name = s.fullName.trim(),
                customer_type = s.customerType,
                nationality = s.nationality.trim(),
                sector = s.sector,
                ubos = s.ubos.filter { it.personName.isNotBlank() }.map {
                    UboIn(it.personName.trim(), it.ownershipPct.toDoubleOrNull(), it.controlType)
                },
            )
            when (val result = repository.createCustomer(body)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(loading = false)
                    if (result.data.blocked) {
                        _state.value = _state.value.copy(
                            blockedWarning = "MATCH FOUND during onboarding screening — review the alert before proceeding.",
                        )
                    }
                    onCreated(result.data.customer_id)
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }
}

@Composable
fun CustomerNewScreen(repository: AmlkitRepository, onCreated: (Int) -> Unit) {
    val viewModel = amlkitViewModel(repository) { CustomerNewViewModel(it) }
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenContentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(text = "Onboard a customer") }
        if (state.error != null) {
            item { ErrorBanner(message = state.error!!) }
        }
        item {
            OutlinedTextField(
                value = state.reference, onValueChange = viewModel::onReferenceChange,
                label = { Text("Reference") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.fullName, onValueChange = viewModel::onFullNameChange,
                label = { Text("Full legal name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            var nationalityMenuExpanded by remember { mutableStateOf(false) }
            val nationalitySuggestions = remember(state.nationality, nationalityMenuExpanded) {
                if (nationalityMenuExpanded) filterCountries(state.nationality).take(50) else emptyList()
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.nationality,
                    onValueChange = {
                        viewModel.onNationalityChange(it)
                        nationalityMenuExpanded = true
                    },
                    label = { Text("Nationality (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = nationalityMenuExpanded && nationalitySuggestions.isNotEmpty(),
                    onDismissRequest = { nationalityMenuExpanded = false },
                ) {
                    nationalitySuggestions.forEach { country ->
                        DropdownMenuItem(
                            text = { Text(country) },
                            onClick = {
                                viewModel.onNationalityChange(country)
                                nationalityMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.sector, onValueChange = viewModel::onSectorChange,
                label = { Text("Sector") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Beneficial owners (25%+ or senior official)", style = MaterialTheme.typography.titleSmall, color = AmlInk)
                Text(text = "+ Add", style = MaterialTheme.typography.labelLarge, color = AmlInk2, modifier = Modifier.clickable(onClick = viewModel::addUbo))
            }
        }
        itemsIndexed(state.ubos) { index, ubo ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ubo.personName,
                    onValueChange = { viewModel.updateUbo(index, ubo.copy(personName = it)) },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = ubo.ownershipPct,
                    onValueChange = { viewModel.updateUbo(index, ubo.copy(ownershipPct = it)) },
                    label = { Text("% owned") },
                    modifier = Modifier.weight(0.5f),
                )
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmlInk2,
                    modifier = Modifier.clickable { viewModel.removeUbo(index) }.padding(top = 18.dp),
                )
            }
        }

        item {
            PillButton(text = "Onboard & screen", onClick = { viewModel.submit(onCreated) }, enabled = !state.loading, loading = state.loading, modifier = Modifier.fillMaxWidth())
        }

        state.blockedWarning?.let { warning -> item { ErrorBanner(message = warning) } }
    }
}
