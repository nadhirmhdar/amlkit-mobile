package com.amlkit.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// The approved amlkit design is a single deliberate light palette (see
// AMLKit Android.dc.html) -- not a Material "seed color" to be reshaded by
// dynamic color or a dark-theme flip. Every screen is themed against these
// exact tokens so the shipped app matches the mockups pixel-for-pixel on
// color, rather than the generic Material3 baseline it used before.
private val AmlkitColors = lightColorScheme(
    primary = AmlAccent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AmlAccentContainer,
    onPrimaryContainer = AmlAccent,
    secondary = AmlInk,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = AmlBg,
    onBackground = AmlInk,
    surface = AmlSurface,
    onSurface = AmlInk,
    surfaceVariant = AmlLineSoft,
    onSurfaceVariant = AmlInk2,
    outline = AmlLine,
    outlineVariant = AmlLineSoft,
    error = AmlDanger,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = AmlDangerContainer,
    onErrorContainer = AmlDanger,
)

@Composable
fun AmlkitTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        if (activity != null) {
            val window = activity.window
            window.statusBarColor = AmlBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = AmlkitColors,
        typography = AmlkitTypography,
        content = content,
    )
}
