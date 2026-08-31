package com.amlkit.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// The mockups set the brand typeface to Manrope, loaded from Google Fonts.
// This app never calls out to any host except its own configured amlkit
// server (see AndroidManifest.xml), so we don't pull a downloadable font at
// runtime -- the system sans-serif (Roboto) stands in, tuned with the same
// weights, sizes and tracking as the mockups so the type rhythm matches
// even though the letterforms differ slightly.
private val AmlFont = FontFamily.Default

// Numeric/reference values (scores, IDs, dates like "0.94") use the
// mockup's `var(--mono)` token -- a monospace face for tabular alignment.
val AmlkitMonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
)

// One-off display styles the Material3 Typography slots don't have room
// for (hero numerals, the login wordmark) -- used directly by the screens
// that need them.
object AmlkitExtraType {
    val heroNumber = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 60.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.04).em,
    )
    val wordmark = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).em,
    )
}

val AmlkitTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.01).em,
    ),
    titleLarge = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.5.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.01).em,
    ),
    titleMedium = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    ),
    bodyMedium = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.1.em,
    ),
    labelMedium = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.06.em,
    ),
    labelSmall = TextStyle(
        fontFamily = AmlFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.02.em,
    ),
)
