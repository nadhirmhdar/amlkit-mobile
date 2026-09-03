package com.amlkit.mobile.ui.common

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** ISO 3166-1 country names, used to power the Nationality picker on
 * customer onboarding and ad-hoc screening -- a fixed list keeps KYC
 * records consistent instead of free-text (typos, "UAE" vs "Emirati",
 * inconsistent casing, ...). */
val Countries: List<String> = listOf(
    "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua and Barbuda",
    "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain",
    "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan",
    "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei", "Bulgaria",
    "Burkina Faso", "Burundi", "Cabo Verde", "Cambodia", "Cameroon", "Canada",
    "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros",
    "Congo (Congo-Brazzaville)", "Costa Rica", "Croatia", "Cuba", "Cyprus",
    "Czechia", "Democratic Republic of the Congo", "Denmark", "Djibouti", "Dominica",
    "Dominican Republic", "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea",
    "Eritrea", "Estonia", "Eswatini", "Ethiopia", "Fiji", "Finland", "France",
    "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada",
    "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras",
    "Hungary", "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland",
    "Israel", "Italy", "Ivory Coast", "Jamaica", "Japan", "Jordan", "Kazakhstan",
    "Kenya", "Kiribati", "Kosovo", "Kuwait", "Kyrgyzstan", "Laos", "Latvia",
    "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania",
    "Luxembourg", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta",
    "Marshall Islands", "Mauritania", "Mauritius", "Mexico", "Micronesia",
    "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique",
    "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand",
    "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia", "Norway",
    "Oman", "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea",
    "Paraguay", "Peru", "Philippines", "Poland", "Portugal", "Qatar", "Romania",
    "Russia", "Rwanda", "Saint Kitts and Nevis", "Saint Lucia",
    "Saint Vincent and the Grenadines", "Samoa", "San Marino",
    "Sao Tome and Principe", "Saudi Arabia", "Senegal", "Serbia", "Seychelles",
    "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands",
    "Somalia", "South Africa", "South Korea", "South Sudan", "Spain",
    "Sri Lanka", "Sudan", "Suriname", "Sweden", "Switzerland", "Syria",
    "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo",
    "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan",
    "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom",
    "United States", "Uruguay", "Uzbekistan", "Vanuatu", "Vatican City",
    "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe",
)

/** Case-insensitive substring match against [Countries], for as-you-type
 * dropdown suggestions -- returns the full list when [query] is blank. */
fun filterCountries(query: String): List<String> {
    if (query.isBlank()) return Countries
    return Countries.filter { it.contains(query, ignoreCase = true) }
}

/** Shared Nationality autocomplete used by both customer onboarding and
 * ad-hoc screening: wraps the caller's own styled text field (passed as
 * [content]) in an [ExposedDropdownMenuBox] backed by [Countries]. This is
 * the one place the suggestion list, menu-anchor wiring, and selection
 * enforcement live, so a future fix (result cap, debouncing, ...) only
 * needs to change here instead of at every call site.
 *
 * Typed text that doesn't exactly match a [Countries] entry is discarded
 * once the menu closes -- [onValueChange] only ever settles on a value
 * that's actually in the list (or blank), which is the whole point of a
 * fixed country list: it rules out the typos/casing/synonym drift
 * ("UAE" vs "Emirati") free text would otherwise let through.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (fieldModifier: Modifier, onTextChange: (String) -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(value, expanded) {
        if (expanded) filterCountries(value).take(50) else emptyList()
    }

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        content(Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true)) {
            onValueChange(it)
            expanded = true
        }
        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = {
                expanded = false
                if (value.isNotBlank() && Countries.none { it.equals(value, ignoreCase = true) }) {
                    onValueChange("")
                }
            },
        ) {
            suggestions.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country) },
                    onClick = {
                        onValueChange(country)
                        expanded = false
                    },
                )
            }
        }
    }
}
