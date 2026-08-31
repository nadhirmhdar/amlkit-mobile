package com.amlkit.mobile.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.ApiResult
import com.amlkit.mobile.data.AuthTokenStore
import com.amlkit.mobile.ui.common.AlertsCallout
import com.amlkit.mobile.ui.common.GrovisorLogo
import com.amlkit.mobile.ui.common.IconTile
import com.amlkit.mobile.ui.common.IconTileTone
import com.amlkit.mobile.ui.common.InitialsAvatar
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.QuickActionCard
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.theme.AmlGood
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    repository: AmlkitRepository,
    tokenStore: AuthTokenStore,
    onScreenName: () -> Unit,
    onNewCustomer: () -> Unit,
    onAbout: () -> Unit,
    onViewAlerts: () -> Unit,
) {
    val operatorName by tokenStore.operatorName.collectAsState()
    val firstName = operatorName?.trim()?.split(" ")?.firstOrNull() ?: "there"

    val greeting = remember { currentGreeting(firstName, returning = true) }
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
    }

    var alertCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        when (val result = repository.dashboard()) {
            is ApiResult.Success -> alertCount = result.data.counts.alerts_total
            is ApiResult.Failure -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GrovisorLogo()
            InitialsAvatar(name = operatorName)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 18.dp)) {
                    ScreenEyebrow(text = today)
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.displaySmall,
                        color = AmlInk,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    Text(
                        text = "amlkit is sanctions screening and customer due diligence for UAE SMEs, at an SME price.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AmlInk,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "Screen a name in seconds, keep each customer file examiner-ready, and see what every result obliges you to do next. No compliance training needed to run it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmlInk2,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.size(6.dp).background(AmlGood, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lists refreshed within the 24-hour window",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmlInk3,
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(top = 18.dp),
                        color = com.amlkit.mobile.ui.theme.AmlLine,
                    )
                }
            }
            item { ScreenEyebrow(text = "Start here", modifier = Modifier.padding(bottom = 14.dp)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                    QuickActionCard(
                        title = "Screen a name",
                        subtitle = "One-off check, Arabic or Latin",
                        onClick = onScreenName,
                        icon = {
                            IconTile(tone = IconTileTone.INK) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(color = androidx.compose.ui.graphics.Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                                }
                            }
                        },
                    )
                    QuickActionCard(
                        title = "Onboard a customer",
                        subtitle = "CDD file, owners, risk rating",
                        onClick = onNewCustomer,
                        icon = {
                            IconTile(tone = IconTileTone.OUTLINE) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                                    drawRoundRect(
                                        color = AmlInk2,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                                    )
                                }
                            }
                        },
                    )
                    QuickActionCard(
                        title = "About us",
                        subtitle = "amlkit, by Grovisor",
                        onClick = onAbout,
                        icon = {
                            IconTile(tone = IconTileTone.OUTLINE) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Spacer(modifier = Modifier.size(width = 13.dp, height = 2.dp).background(AmlInk2))
                                    Spacer(modifier = Modifier.size(width = 13.dp, height = 2.dp).background(AmlInk2))
                                    Spacer(modifier = Modifier.size(width = 8.dp, height = 2.dp).background(AmlInk2))
                                }
                            }
                        },
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            AlertsCallout(count = alertCount, onClick = onViewAlerts)
        }
    }
}

/** Ported from the design mockup's greeting() -- a claude.ai-style rotating
 * pool of short openers, filtered by clock hour and weekday, always ending
 * in the operator's first name. Keep this in lockstep with any future
 * change to Claude's own greeting behaviour (see project CLAUDE.md). */
fun currentGreeting(name: String, returning: Boolean, now: LocalTime = LocalTime.now(), day: DayOfWeek = LocalDate.now().dayOfWeek): String {
    val hour = now.hour
    val pool = mutableListOf<String>()
    when {
        hour < 5 -> pool.add("Working late")
        hour < 12 -> pool.addAll(listOf("Good morning", "Morning"))
        hour < 17 -> pool.addAll(listOf("Good afternoon", "Afternoon"))
        hour < 22 -> pool.addAll(listOf("Good evening", "Evening"))
        else -> pool.addAll(listOf("Working late", "Good evening"))
    }
    if (day == DayOfWeek.MONDAY && hour < 12) pool.add("Happy Monday")
    if (day == DayOfWeek.FRIDAY) pool.add("Happy Friday")
    if (day == DayOfWeek.SUNDAY) pool.add("Happy Sunday")
    if (day == DayOfWeek.SATURDAY) pool.add("Happy Saturday")
    if (returning) pool.addAll(listOf("Welcome back", "Let's get to it"))

    val date = LocalDate.now()
    val seed = date.year * 1000 + date.dayOfYear * 4 + hour / 6
    return "${pool[seed % pool.size]}, $name"
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "amlkit", style = MaterialTheme.typography.displaySmall, color = AmlInk)
        Text(
            text = "amlkit is sanctions screening and customer due diligence for UAE SMEs, at an SME price -- built by Grovisor Business Consultants LLC.",
            style = MaterialTheme.typography.bodyLarge,
            color = AmlInk2,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Screen a name in seconds, keep each customer file examiner-ready, and see what every result obliges you to do next. No compliance training needed to run it.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmlInk2,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "A product by Grovisor Business Consultants LLC.",
            style = MaterialTheme.typography.bodySmall,
            color = AmlInk3,
        )
        Spacer(modifier = Modifier.weight(1f))
        PillButton(text = "Back", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
