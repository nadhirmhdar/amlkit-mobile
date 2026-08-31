package com.amlkit.mobile.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amlkit.mobile.data.AuthTokenStore
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.common.PillButton
import com.amlkit.mobile.ui.common.PillButtonTone
import com.amlkit.mobile.ui.common.ScreenEyebrow
import com.amlkit.mobile.ui.common.ScreenTitle
import com.amlkit.mobile.ui.theme.AmlDanger
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk3

private data class MoreItem(val title: String, val subtitle: String, val onClick: () -> Unit, val danger: Boolean = false)

@Composable
fun MoreScreen(
    tokenStore: AuthTokenStore,
    onAlerts: () -> Unit,
    onAudit: () -> Unit,
    onAdmin: () -> Unit,
    onReports: () -> Unit,
    onLogout: () -> Unit,
) {
    val role by tokenStore.operatorRole.collectAsState()
    val operatorName by tokenStore.operatorName.collectAsState()
    val items = buildList {
        add(MoreItem("Alerts", "Screening queue and disposition", onAlerts))
        add(MoreItem("Reports", "STR/SAR reports to the UAE FIU", onReports))
        add(MoreItem("Audit trail", "Every action taken on this organization's data", onAudit))
        if (role == "mlro") {
            add(MoreItem("Admin", "Operators, alert threshold, sanctions refresh", onAdmin))
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = com.amlkit.mobile.ui.common.screenContentPadding) {
        item {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                ScreenEyebrow(text = operatorName ?: "Account")
                ScreenTitle(text = "More", modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
                HairlineDivider()
            }
        }
        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleLarge, color = AmlInk)
                    Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = AmlInk3)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AmlInk3, modifier = Modifier.size(18.dp))
            }
            HairlineDivider(soft = true)
        }
        item {
            PillButton(
                text = "Sign out",
                onClick = onLogout,
                tone = PillButtonTone.SECONDARY,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
    }
}
