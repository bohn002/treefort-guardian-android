package com.treefort.guardian.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TreeFortColorScheme = lightColorScheme(
    primary = GuardianBlue,
    secondary = TreeFortGreen,
    tertiary = SunburstAmber,
    background = CanvasBg,
    surface = CardBg,
    onPrimary = CardBg,
    onSecondary = Ink,
    onTertiary = Ink,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun TreeFortGuardianTheme(
    content: @Composable () -> Unit
) {
    val defaultFontFamily = if (ThemeConfig.isBoringTextEnabled) FontFamilyTreeFortBook else FontFamilyTreeFortClubhouse
    MaterialTheme(
        colorScheme = TreeFortColorScheme,
        typography = Typography
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides androidx.compose.ui.text.TextStyle(
                fontFamily = defaultFontFamily
            )
        ) {
            content()
        }
    }
}
