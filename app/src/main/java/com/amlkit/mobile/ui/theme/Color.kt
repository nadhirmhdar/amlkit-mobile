package com.amlkit.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// amlkit visual language -- ported 1:1 from the approved design mockups
// (AMLKit Android.dc.html). Source values there are specified in OKLCH;
// these are their sRGB conversions so Compose can consume them directly.
// Keep this file in lockstep with the mockup's `--token` custom properties
// if the design is ever revised.

val AmlBg = Color(0xFFF8FAFB)
val AmlSurface = Color(0xFFFCFDFE)
val AmlLine = Color(0xFFDFE1E4)
val AmlLineSoft = Color(0xFFEBEDEF)

val AmlInk = Color(0xFF101419)
val AmlInk2 = Color(0xFF53595F)
val AmlInk3 = Color(0xFF888D92)

val AmlGood = Color(0xFF287C42)
val AmlWarn = Color(0xFFB77610)
val AmlAccent = Color(0xFF006BBB)
val AmlAccentSoft = Color(0xFFDAEEFF)
val AmlDanger = Color(0xFFB63B39)
val AmlViolet = Color(0xFF6F5DB9)

// Light background tints used behind status pills / category chips.
val AmlDangerContainer = Color(0xFFFFE6E3)
val AmlWarnContainer = Color(0xFFFEEBD6)
val AmlGoodContainer = Color(0xFFD5F5DA)
val AmlVioletContainer = Color(0xFFEEECFF)
val AmlAccentContainer = Color(0xFFDAEEFF)

// The dark "ink" tiles/buttons in the mockups (primary CTA, screen-a-name
// icon tile, sign-in button) carry a warm radial-gradient corner over the
// ink base rather than a flat fill.
val AmlInkGradientCorner = Color(0xFF723311)

// The bottom-of-home "N alerts need your action" callout has three states
// depending on backlog size: urgent (red gradient), some (soft amber), none
// (plain surface). These are its bespoke tones, distinct from the generic
// danger/warn tokens used for pills elsewhere.
val AmlUrgentBase = Color(0xFF94151D)
val AmlUrgentCorner = Color(0xFFA82418)
val AmlWarnSoftBg = Color(0xFFFFEECD)
val AmlWarnSoftFg = Color(0xFF643400)

val AmlMono = androidx.compose.ui.text.font.FontFamily.Monospace
