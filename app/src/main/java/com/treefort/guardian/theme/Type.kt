package com.treefort.guardian.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

enum class ThemeMode { System, Light, Dark }

/** Global, reactive appearance settings. Persisted by [com.treefort.guardian.AppPrefs]. */
object ThemeConfig {
    /** Plain Text mode (formerly "boring text"): every role renders in TreeFort Book. */
    var isBoringTextEnabled by mutableStateOf(false)
    var mode by mutableStateOf(ThemeMode.System)

    /** Mirrors the OS night mode. Seeded in MainActivity before first composition, kept current by the theme. */
    var systemDark by mutableStateOf(false)

    val isDark: Boolean
        get() = when (mode) {
            ThemeMode.System -> systemDark
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
}

/**
 * The DESIGN.md type scale as text styles, switching to the Plain Text set when that mode is on.
 * Use these instead of raw sizes. `label`, `labelSm` and `micro` are uppercase styles: pass the
 * string through [upper] (TreeFortTokens.UppercaseStyles).
 */
object Tf {
    private val plain get() = ThemeConfig.isBoringTextEnabled
    val displayXl: TextStyle get() = if (plain) TreeFortTokens.PlainType.DisplayXl else TreeFortTokens.Type.DisplayXl
    val display: TextStyle get() = if (plain) TreeFortTokens.PlainType.Display else TreeFortTokens.Type.Display
    val titleLg: TextStyle get() = if (plain) TreeFortTokens.PlainType.TitleLg else TreeFortTokens.Type.TitleLg
    val title: TextStyle get() = if (plain) TreeFortTokens.PlainType.Title else TreeFortTokens.Type.Title
    val titleSm: TextStyle get() = if (plain) TreeFortTokens.PlainType.TitleSm else TreeFortTokens.Type.TitleSm
    val button: TextStyle get() = if (plain) TreeFortTokens.PlainType.Button else TreeFortTokens.Type.Button
    val bodyLg: TextStyle get() = if (plain) TreeFortTokens.PlainType.BodyLg else TreeFortTokens.Type.BodyLg
    val body: TextStyle get() = if (plain) TreeFortTokens.PlainType.Body else TreeFortTokens.Type.Body
    val bodyStrong: TextStyle get() = if (plain) TreeFortTokens.PlainType.BodyStrong else TreeFortTokens.Type.BodyStrong
    val bodySm: TextStyle get() = if (plain) TreeFortTokens.PlainType.BodySm else TreeFortTokens.Type.BodySm
    val caption: TextStyle get() = if (plain) TreeFortTokens.PlainType.Caption else TreeFortTokens.Type.Caption
    val label: TextStyle get() = if (plain) TreeFortTokens.PlainType.Label else TreeFortTokens.Type.Label
    val labelSm: TextStyle get() = if (plain) TreeFortTokens.PlainType.LabelSm else TreeFortTokens.Type.LabelSm
    val micro: TextStyle get() = if (plain) TreeFortTokens.PlainType.Micro else TreeFortTokens.Type.Micro
    val numericXl: TextStyle get() = if (plain) TreeFortTokens.PlainType.NumericXl else TreeFortTokens.Type.NumericXl
    val numeric: TextStyle get() = if (plain) TreeFortTokens.PlainType.Numeric else TreeFortTokens.Type.Numeric
    val numericSm: TextStyle get() = if (plain) TreeFortTokens.PlainType.NumericSm else TreeFortTokens.Type.NumericSm
    val brand: TextStyle get() = if (plain) TreeFortTokens.PlainType.Brand else TreeFortTokens.Type.Brand

    /** Display face (Chunky, or Book bold in Plain Text mode) for call sites that only need the family. */
    val displayFamily: FontFamily get() = if (plain) TreeFortTokens.Fonts.Book else TreeFortTokens.Fonts.Chunky
}

/** Uppercase at render time for the label styles; source strings stay in sentence case. */
fun upper(text: String): String = text.uppercase()
