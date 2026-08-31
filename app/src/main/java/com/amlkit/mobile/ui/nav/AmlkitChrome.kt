package com.amlkit.mobile.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amlkit.mobile.ui.common.HairlineDivider
import com.amlkit.mobile.ui.theme.AmlBg
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlSurface

/** Shape a bottom-nav tab's indicator draws with -- the mockups use a small
 * abstract glyph per tab rather than a Material icon: a rounded square for
 * Home/Dashboard/Customers, a circle for Screen, a short dash for More. */
private enum class TabGlyph { SQUARE, CIRCLE, DASH }

private data class BottomTab(val route: String, val label: String, val glyph: TabGlyph)

private val bottomTabsChrome = listOf(
    BottomTab(Routes.HOME, "Home", TabGlyph.SQUARE),
    BottomTab(Routes.DASHBOARD, "Dashboard", TabGlyph.SQUARE),
    BottomTab(Routes.SCREENING, "Screen", TabGlyph.CIRCLE),
    BottomTab(Routes.CUSTOMERS, "Customers", TabGlyph.SQUARE),
    BottomTab(Routes.MORE, "More", TabGlyph.DASH),
)

val bottomTabRoutes: List<String> = bottomTabsChrome.map { it.route }

/** The five-tab bottom bar from the mockups: hairline top border, small
 * dot/square/dash glyphs instead of Material icons, bold ink label on the
 * active tab and regular ink-3 on the rest. */
@Composable
fun AmlkitBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    Column {
        HairlineDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmlSurface)
                .padding(top = 8.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
        ) {
            bottomTabsChrome.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab.route) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TabGlyphIndicator(tab.glyph, selected)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) AmlInk else AmlInk3,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabGlyphIndicator(glyph: TabGlyph, selected: Boolean) {
    when (glyph) {
        TabGlyph.DASH -> Spacer(
            modifier = Modifier
                .size(width = 9.dp, height = 2.dp)
                .background(if (selected) AmlInk else AmlInk3),
        )
        TabGlyph.CIRCLE -> Spacer(
            modifier = Modifier
                .size(9.dp)
                .then(
                    if (selected) Modifier.background(AmlInk, CircleShape)
                    else Modifier.border(BorderStroke(1.5.dp, AmlInk3), CircleShape),
                ),
        )
        TabGlyph.SQUARE -> Spacer(
            modifier = Modifier
                .size(9.dp)
                .then(
                    if (selected) Modifier.background(AmlInk, RoundedCornerShape(2.dp))
                    else Modifier.border(BorderStroke(1.5.dp, AmlInk3), RoundedCornerShape(2.dp)),
                ),
        )
    }
}

/** The minimal top strip on every pushed sub-page: a back chevron and a
 * small uppercase ink-3 label, no elevation, background matching the page.
 * The mockups always keep the *actual* page title inside the scrolling
 * content below this strip rather than in a system app bar. */
@Composable
fun AmlkitSubHeader(label: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmlBg)
            .height(48.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "←",
            color = AmlInk2,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clickable(onClick = onBack).padding(end = 16.dp),
        )
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = AmlInk3)
    }
}
