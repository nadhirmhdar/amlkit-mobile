package com.amlkit.mobile.ui.screening

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.dto.ScreenHitDto
import com.amlkit.mobile.data.dto.ScreenResponse
import com.amlkit.mobile.ui.common.CategoryTag
import com.amlkit.mobile.ui.common.ErrorBanner
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillTone
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.ScreenTitle
import com.amlkit.mobile.ui.common.amlkitViewModel
import com.amlkit.mobile.ui.common.categoryTone
import com.amlkit.mobile.ui.common.filterCountries
import com.amlkit.mobile.ui.common.screenContentPadding
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScreeningUiState(
    val query: String = "",
    val nationality: String = "",
    val birthDate: String = "",
    val loading: Boolean = false,
    val result: ScreenResponse? = null,
    val error: String? = null,
)

class ScreeningViewModel(private val repository: AmlkitRepository) : ViewModel() {
    private val _state = MutableStateFlow(ScreeningUiState())
    val state: StateFlow<ScreeningUiState> = _state

    fun onQueryChange(value: String) { _state.value = _state.value.copy(query = value) }
    fun onNationalityChange(value: String) { _state.value = _state.value.copy(nationality = value) }
    fun onBirthDateChange(value: String) { _state.value = _state.value.copy(birthDate = value) }

    fun runScreen() {
        val current = _state.value
        val name = current.query.trim()
        if (name.isEmpty()) {
            _state.value = _state.value.copy(error = "Enter a name to screen.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = repository.screen(name, country = current.nationality.trim(), birthDate = current.birthDate.trim())) {
                is ApiResult.Success -> _state.value = _state.value.copy(loading = false, result = result.data)
                is ApiResult.Failure -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }
}

@Composable
fun ScreeningScreen(repository: AmlkitRepository) {
    val viewModel = amlkitViewModel(repository) { ScreeningViewModel(it) }
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenContentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                ScreenEyebrow(text = "Ad-hoc check")
                ScreenTitle(text = "Screen a name", modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
                HairlineDivider()
            }
        }
        item {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) {
                Text(
                    text = "NAME — ARABIC OR LATIN SCRIPT",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmlInk3,
                )
                TextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = AmlInk),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = AmlInk,
                        unfocusedIndicatorColor = AmlLine,
                        cursorColor = AmlInk,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InlinePillField(
                        placeholder = "Nationality",
                        value = state.nationality,
                        onValueChange = viewModel::onNationalityChange,
                    )
                    InlineDatePillField(
                        placeholder = "Date of birth",
                        value = state.birthDate,
                        onValueChange = viewModel::onBirthDateChange,
                    )
                }
            }
        }
        item {
            PillButton(
                text = "Screen",
                onClick = viewModel::runScreen,
                enabled = !state.loading,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            )
        }
        if (state.error != null) {
            item { ErrorBanner(message = state.error!!) }
        }
        state.result?.let { result ->
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    if (result.low_confidence) {
                        ErrorBanner(message = "Single-word name: matching confidence is lower. Add a family name if possible.")
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Spacer(
                            modifier = Modifier.size(6.dp).background(
                                if (result.clear) com.amlkit.mobile.ui.theme.AmlGood else com.amlkit.mobile.ui.theme.AmlDanger,
                                CircleShape,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (result.clear) "CLEAR — no hits" else "${result.hits.size} possible match${if (result.hits.size == 1) "" else "es"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmlInk2,
                        )
                    }
                    HairlineDivider(modifier = Modifier.padding(top = 14.dp), soft = true)
                }
            }
            items(result.hits) { hit -> ScreeningHitRow(hit) }
        }
    }
}

@Composable
private fun ScreeningHitRow(hit: ScreenHitDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        CategoryTag(text = hit.category, tone = categoryTone(hit.category), trailing = "%.2f".format(hit.score))
        Text(text = hit.caption, style = MaterialTheme.typography.titleMedium, color = AmlInk, modifier = Modifier.padding(top = 6.dp))
        Text(text = hit.dataset, style = MaterialTheme.typography.bodySmall, color = AmlInk3, modifier = Modifier.padding(top = 3.dp))
        if (hit.matched_name.isNotBlank() && hit.matched_name != hit.caption) {
            Text(text = "Matched name: ${hit.matched_name}", style = MaterialTheme.typography.bodySmall, color = AmlInk3, modifier = Modifier.padding(top = 2.dp))
        }
        hit.obligation?.let { obligation ->
            Row(modifier = Modifier.padding(top = 10.dp)) {
                Spacer(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(4.dp)
                        .background(AmlInk3, CircleShape),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = obligation, style = MaterialTheme.typography.bodySmall, color = AmlInk2)
            }
        }
        if (hit.aliases.isNotEmpty()) {
            Text(
                text = "Aliases: " + hit.aliases.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = AmlInk3,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
    HairlineDivider(soft = true)
}

/** Same rounded-pill look as [InlinePillField], but for "Date of birth":
 * tapping opens a Material date picker instead of the keyboard, so the
 * value is always a well-formed ISO date (yyyy-MM-dd) for birth_date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineDatePillField(placeholder: String, value: String, onValueChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .border(androidx.compose.foundation.BorderStroke(1.dp, AmlLine), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart,
    ) {
        Text(
            text = value.ifEmpty { placeholder },
            style = MaterialTheme.typography.bodySmall,
            color = if (value.isEmpty()) AmlInk3 else AmlInk2,
        )
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochMillisOrNull(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onValueChange(millis.toIsoDate())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Parses an ISO yyyy-MM-dd string (UTC midnight) to epoch millis for
 * [rememberDatePickerState], or null if blank/unparsable. */
private fun String.toEpochMillisOrNull(): Long? = try {
    if (isBlank()) null else java.time.LocalDate.parse(this).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
} catch (e: Exception) {
    null
}

/** Formats epoch millis (UTC midnight, as returned by the date picker) back
 * to the ISO yyyy-MM-dd string the birth_date param expects. */
private fun Long.toIsoDate(): String =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()

/** The mockup's small rounded-pill "Nationality" / "Date of birth" fields
 * next to the name input -- optional context that narrows a screen, wired
 * to the real country/birth_date params ScreenRequest already accepts.
 * "Nationality" additionally suggests matches from [filterCountries] as the
 * user types, so onboarding picks a consistent country name instead of
 * relying entirely on free text. */
@Composable
private fun InlinePillField(placeholder: String, value: String, onValueChange: (String) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val suggestions = remember(value, menuExpanded) {
        if (menuExpanded) filterCountries(value).take(50) else emptyList()
    }
    Box {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                menuExpanded = true
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = AmlInk2),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AmlInk),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .border(androidx.compose.foundation.BorderStroke(1.dp, AmlLine), androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = androidx.compose.ui.Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = MaterialTheme.typography.bodySmall, color = AmlInk3)
                    }
                    inner()
                }
            },
        )
        DropdownMenu(
            expanded = menuExpanded && suggestions.isNotEmpty(),
            onDismissRequest = { menuExpanded = false },
        ) {
            suggestions.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country) },
                    onClick = {
                        onValueChange(country)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}
