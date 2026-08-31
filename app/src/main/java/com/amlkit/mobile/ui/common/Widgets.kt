package com.amlkit.mobile.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amlkit.mobile.ui.theme.AmlDanger
import com.amlkit.mobile.ui.theme.AmlDangerContainer
import com.amlkit.mobile.ui.theme.AmlGood
import com.amlkit.mobile.ui.theme.AmlGoodContainer
import com.amlkit.mobile.ui.theme.AmlInk
import com.amlkit.mobile.ui.theme.AmlInk2
import com.amlkit.mobile.ui.theme.AmlInk3
import com.amlkit.mobile.ui.theme.AmlInkGradientCorner
import com.amlkit.mobile.ui.theme.AmlLine
import com.amlkit.mobile.ui.theme.AmlLineSoft
import com.amlkit.mobile.ui.theme.AmlSurface
import com.amlkit.mobile.ui.theme.AmlUrgentBase
import com.amlkit.mobile.ui.theme.AmlUrgentCorner
import com.amlkit.mobile.ui.theme.AmlWarn
import com.amlkit.mobile.ui.theme.AmlWarnContainer
import com.amlkit.mobile.ui.theme.AmlWarnSoftBg
import com.amlkit.mobile.ui.theme.AmlWarnSoftFg
import com.amlkit.mobile.ui.theme.AmlkitMonoStyle

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AmlInk)
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = AmlDangerContainer,
    ) {
        Text(
            text = message,
            color = AmlDanger,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Small colored label used for alert/customer/report status everywhere --
 * category and severity vocabularies overlap across screens (sanction /
 * terrorism / proliferation / high / breach / true_positive / ...), so this
 * takes freeform text and a tone rather than an enum tied to one screen. */
enum class PillTone { DANGER, WARNING, SUCCESS, NEUTRAL }

fun PillTone.dotColor(): Color = when (this) {
    PillTone.DANGER -> AmlDanger
    PillTone.WARNING -> AmlWarn
    PillTone.SUCCESS -> AmlGood
    PillTone.NEUTRAL -> AmlInk3
}

@Composable
fun StatusPill(text: String, tone: PillTone, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        PillTone.DANGER -> AmlDangerContainer to AmlDanger
        PillTone.WARNING -> AmlWarnContainer to AmlWarn
        PillTone.SUCCESS -> AmlGoodContainer to AmlGood
        PillTone.NEUTRAL -> AmlLineSoft to AmlInk2
    }
    Surface(shape = RoundedCornerShape(50), color = bg, modifier = modifier) {
        Text(
            text = text.uppercase(),
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** The dot + uppercase colored label used in front of every alert/hit/queue
 * row in the mockups (e.g. a red dot and "SANCTION"), optionally trailed by
 * a right-aligned monospace score. */
@Composable
fun CategoryTag(text: String, tone: PillTone, modifier: Modifier = Modifier, trailing: String? = null) {
    val color = tone.dotColor()
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.size(5.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(7.dp))
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color)
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            Text(text = trailing, style = AmlkitMonoStyle, fontSize = MaterialTheme.typography.bodySmall.fontSize, color = AmlInk2)
        }
    }
}

fun categoryTone(category: String): PillTone = when (category) {
    "sanction", "terrorism", "proliferation" -> PillTone.DANGER
    "pep" -> PillTone.WARNING
    else -> PillTone.NEUTRAL
}

fun alertStatusTone(status: String): PillTone = when (status) {
    "open" -> PillTone.WARNING
    "pending_review" -> PillTone.WARNING
    "true_positive", "escalated" -> PillTone.DANGER
    "false_positive" -> PillTone.SUCCESS
    else -> PillTone.NEUTRAL
}

fun riskTone(rating: String?): PillTone = when (rating) {
    "high" -> PillTone.DANGER
    "medium" -> PillTone.WARNING
    "low" -> PillTone.SUCCESS
    else -> PillTone.NEUTRAL
}

/** Small uppercase section header, e.g. "START HERE" / "WORKSPACE" /
 * "OLDEST OPEN ALERTS" in the mockups -- always ink-3, tracked out. */
@Composable
fun ScreenEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = AmlInk3,
        modifier = modifier,
    )
}

/** The big extrabold page title under an eyebrow, e.g. "Screen a name". */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.displaySmall, color = AmlInk, modifier = modifier)
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier, soft: Boolean = false) {
    Spacer(modifier = modifier.fillMaxWidth().height(1.dp).background(if (soft) AmlLineSoft else AmlLine))
}

/** A plain white card with the mockup's hairline border and generous
 * rounding -- the base container used for grouped content everywhere. */
@Composable
fun AmlCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(AmlSurface)
        .border(BorderStroke(1.dp, AmlLine), RoundedCornerShape(20.dp))
    Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = content,
    )
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AmlCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = AmlInk)
            content()
        }
    }
}

/** Alias so content lambdas can be typed against Column's scope without
 * importing ColumnScope at every call site. */
typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

val screenContentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)

/** The warm radial glow the mockups paint into the top-right corner of every
 * ink surface (`radial-gradient(circle at 100% 0%, ...)` in the CSS source)
 * -- a plain `Brush.radialGradient` can't place its center as a fraction of
 * the element's own size, so this draws it against the real measured size. */
fun Modifier.amlCornerGlow(colors: List<Color>): Modifier = this.drawWithCache {
    val brush = Brush.radialGradient(
        colors = colors,
        center = Offset(size.width, 0f),
        radius = size.maxDimension.coerceAtLeast(1f),
    )
    onDrawBehind { drawRect(brush) }
}

// ---------------------------------------------------------------------
// Icon tiles (the small rounded squares in front of a quick-action title)
// ---------------------------------------------------------------------

enum class IconTileTone { INK, OUTLINE }

@Composable
fun IconTile(tone: IconTileTone, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val background = when (tone) {
        IconTileTone.INK -> Modifier
            .background(AmlInk)
            .amlCornerGlow(listOf(AmlInkGradientCorner.copy(alpha = 0.85f), Color.Transparent))
        IconTileTone.OUTLINE -> Modifier
            .background(AmlSurface)
            .border(BorderStroke(1.dp, AmlLine), RoundedCornerShape(12.dp))
    }
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A "Start here" quick-action row: icon tile, title + subtitle, arrow. */
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    AmlCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = AmlInk)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = AmlInk3)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AmlInk3, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------------
// Buttons -- the mockups never use a bare Material filled/outlined button;
// every primary action is a fully rounded pill, ink (or danger) filled
// with a warm gradient corner, uppercase-tracked label.
// ---------------------------------------------------------------------

enum class PillButtonTone { PRIMARY, DANGER, SECONDARY }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PillButtonTone = PillButtonTone.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 50.dp,
) {
    val shape = RoundedCornerShape(50)
    when (tone) {
        PillButtonTone.SECONDARY -> {
            Box(
                modifier = modifier
                    .height(height)
                    .clip(shape)
                    .border(BorderStroke(1.dp, AmlLine), shape)
                    .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                PillButtonLabel(text, loading, AmlInk2)
            }
        }
        else -> {
            val base = if (tone == PillButtonTone.DANGER) AmlUrgentBase else AmlInk
            val glow = if (tone == PillButtonTone.DANGER) AmlUrgentCorner else AmlInkGradientCorner
            Box(
                modifier = modifier
                    .height(height)
                    .clip(shape)
                    .background(base)
                    .amlCornerGlow(listOf(glow.copy(alpha = 0.9f), Color.Transparent))
                    .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                PillButtonLabel(text, loading, Color.White)
            }
        }
    }
}

@Composable
private fun PillButtonLabel(text: String, loading: Boolean, color: Color) {
    if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = color, strokeWidth = 2.dp)
    } else {
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RowScope.PillButtonWeighted(
    text: String,
    onClick: () -> Unit,
    tone: PillButtonTone = PillButtonTone.PRIMARY,
    weight: Float = 1f,
    enabled: Boolean = true,
    height: Dp = 44.dp,
) {
    PillButton(text = text, onClick = onClick, tone = tone, enabled = enabled, height = height, modifier = Modifier.weight(weight))
}

/** A quiet centered text link, e.g. "Escalate to MLRO instead". */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = AmlInk3,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
    )
}

// ---------------------------------------------------------------------
// Alerts callout -- bottom-of-home CTA pill. Tone follows the open-alert
// backlog: urgent (red gradient), some (soft amber), none (plain surface).
// ---------------------------------------------------------------------

@Composable
fun AlertsCallout(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val urgent = count > 20
    val some = count > 0
    val shape = RoundedCornerShape(50)
    val bgModifier = when {
        urgent -> Modifier
            .background(AmlUrgentBase)
            .amlCornerGlow(listOf(AmlUrgentCorner.copy(alpha = 0.9f), Color.Transparent))
        some -> Modifier.background(AmlWarnSoftBg)
        else -> Modifier.background(AmlSurface).border(BorderStroke(1.dp, AmlLine), shape)
    }
    val fg = when {
        urgent -> Color.White
        some -> AmlWarnSoftFg
        else -> AmlInk2
    }
    val dot = when {
        urgent -> Color.White
        some -> AmlWarn
        else -> AmlGood
    }
    val title = if (some) "$count Alert${if (count == 1) "" else "s"} need your action." else "No alerts need your action."
    val subtitle = if (some) "Tap to review the queue" else "Queue is clear"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.size(9.dp).background(dot, CircleShape))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.75f))
        }
        Text(text = "→", color = fg, style = MaterialTheme.typography.titleLarge)
    }
}

// ---------------------------------------------------------------------
// Avatar -- the initials circle in the top bar.
// ---------------------------------------------------------------------

@Composable
fun InitialsAvatar(name: String?, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val initials = name
        ?.trim()
        ?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.joinToString("") { it.first().uppercase() }
        ?: "?"
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(AmlInk),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

// ---------------------------------------------------------------------
// Dialog chrome -- every AlertDialog in the app (disposition, confirm
// review, assign, case-file actions) shares this rounded shape and field
// styling instead of the Material default.
// ---------------------------------------------------------------------

val AmlDialogShape = RoundedCornerShape(24.dp)

@Composable
fun amlDialogFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AmlInk,
    unfocusedBorderColor = AmlLine,
    cursorColor = AmlInk,
    focusedLabelColor = AmlInk2,
    unfocusedLabelColor = AmlInk3,
)

val AmlDialogFieldShape = RoundedCornerShape(12.dp)
