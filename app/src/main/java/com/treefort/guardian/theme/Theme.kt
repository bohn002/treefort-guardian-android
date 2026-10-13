package com.treefort.guardian.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect

@Composable
fun TreeFortGuardianTheme(content: @Composable () -> Unit) {
    val system = isSystemInDarkTheme()
    SideEffect { if (ThemeConfig.systemDark != system) ThemeConfig.systemDark = system }

    val dark = ThemeConfig.isDark
    MaterialTheme(
        colorScheme = TreeFortTokens.colorScheme(dark),
        typography = TreeFortTokens.typography(plain = ThemeConfig.isBoringTextEnabled),
    ) {
        CompositionLocalProvider(
            LocalTreeFortColors provides TreeFortTokens.colors(dark),
            LocalTextStyle provides Tf.body,
        ) {
            content()
        }
    }
}
