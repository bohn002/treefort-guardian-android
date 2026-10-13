package com.treefort.guardian.theme

import androidx.compose.ui.graphics.Color

// All values come from the generated TreeFortTokens.kt (DESIGN.md). Nothing here defines a hex literal.
// Identity colors (brand palette, same in light and dark):

private val P = TreeFortTokens.Palette

val ColorInk = P.Ink
val ColorPlastic = P.Plastic
val ColorPlasticDark = P.PlasticDark
val ColorSlateMid = P.SlateMid
val ColorPrimaryBlue = P.PrimaryBlue
val ColorBluePowder = P.BluePowder
val ColorBlueDeep = P.BlueDeep
val ColorWarningYellow = P.WarningYellow
val ColorYellowPale = P.YellowPale
val ColorYellowAmber = P.YellowAmber
val ColorSafeGreen = P.SafeGreen
val ColorGreenMint = P.GreenMint
val ColorGreenForest = P.GreenForest
val ColorDangerRed = P.DangerRed
val ColorRedCrimson = P.RedCrimson
val ColorRetroGrape = P.RetroGrape
val ColorRustyOrange = P.RustyOrange
val ColorEarthBrown = P.EarthBrown
val ColorWhite = P.White

// Semantic colors follow light/dark mode. They read ThemeConfig.isDark (snapshot state), so any
// composable that uses them recomposes when the mode changes.
private val sem: TreeFortColors get() = TreeFortTokens.colors(ThemeConfig.isDark)

val Ink: Color get() = sem.text
val BorderInk: Color get() = sem.border
val CanvasBg: Color get() = sem.canvas
val CardBg: Color get() = sem.surface
val RaisedBg: Color get() = sem.surfaceRaised
val PillBg: Color get() = sem.surfaceSunken
val MutedText: Color get() = sem.textSecondary
val DisabledText: Color get() = sem.textDisabled
val DisabledFill: Color get() = sem.fillDisabled
val DividerColor: Color get() = sem.divider
val OnAccent: Color get() = sem.onAccent
val LinkColor: Color get() = sem.link

val SuccessPale: Color get() = sem.successPale
val SuccessFg: Color get() = sem.successFg
val WarningPale: Color get() = sem.warningPale
val WarningFg: Color get() = sem.warningFg
val DangerPale: Color get() = sem.dangerPale
val DangerFg: Color get() = sem.dangerFg
val InfoPale: Color get() = sem.infoPale
val InfoFg: Color get() = sem.infoFg
val PrimarySoft: Color get() = sem.primarySoft
val PrimaryFg: Color get() = sem.primaryFg

// Brand fills used as button and chip backgrounds.
val TreeFortGreen = ColorSafeGreen
val GuardianBlue = ColorPrimaryBlue
val SunburstAmber = ColorWarningYellow
val AlertRed = ColorDangerRed
val ForestGreen = ColorGreenForest
