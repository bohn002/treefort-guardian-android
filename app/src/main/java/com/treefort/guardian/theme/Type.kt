package com.treefort.guardian.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.treefort.guardian.R

// Global reactive Boring Text state
object ThemeConfig {
    var isBoringTextEnabled by mutableStateOf(false)
}

// TreeFort Custom Typography Families loaded from res/font/
val FontFamilyTreeFortClubhouse = FontFamily(
    Font(R.font.treefort_clubhouse, FontWeight.Normal)
)

val FontFamilyTreeFortBook = FontFamily(
    Font(R.font.treefort_book, FontWeight.Normal),
    Font(R.font.treefort_book_bold, FontWeight.Bold)
)

val FontFamilyTreeFortChunky = FontFamily(
    Font(R.font.treefort_chunky, FontWeight.Normal)
)

fun fontDisplay(): FontFamily {
    return if (ThemeConfig.isBoringTextEnabled) FontFamilyTreeFortBook else FontFamilyTreeFortClubhouse
}

fun fontBody(): FontFamily {
    return FontFamilyTreeFortBook
}

// Mapped typography
val ArchivoFamily get() = fontBody()
val ArchivoBlackFamily get() = fontDisplay()
val ArchivoNarrowFamily get() = fontBody()

val Typography: Typography
    get() = Typography(
        displayLarge = TextStyle(
            fontFamily = fontDisplay(),
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = fontDisplay(),
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = fontDisplay(),
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = fontDisplay(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = fontBody(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = fontBody(),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = fontBody(),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
        ),
    )
