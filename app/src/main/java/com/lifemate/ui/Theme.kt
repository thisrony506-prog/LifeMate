package com.lifemate.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Sage = Color(0xFFA8C3B9)
val PremiumPink = Sage
// Compatibility aliases for existing card/editor components, now using the restrained palette.
val Jade = PremiumPink
val Lime = Color(0xFFE2EEE7)
val Apricot = Color(0xFFF5F8F6)
private val Light = lightColorScheme(
    primary = PremiumPink, onPrimary = Color(0xFF1A1A1A), primaryContainer = Color(0xFFEDF4F0), onPrimaryContainer = Color(0xFF244234),
    secondary = Color(0xFF252529), onSecondary = Color.White, secondaryContainer = Color(0xFFF2F2F4), onSecondaryContainer = Color(0xFF222226),
    tertiary = PremiumPink, background = Color(0xFFFFFFFF), onBackground = Color(0xFF17171B),
    surface = Color.White, onSurface = Color(0xFF17171B), surfaceVariant = Color(0xFFF3F3F5), onSurfaceVariant = Color(0xFF64646E),
    outline = Color(0xFF797981), outlineVariant = Color(0xFFE7E7EB), error = Color(0xFFB32635))
private val Dark = darkColorScheme(
    primary = Color(0xFFA8C3B9), onPrimary = Color(0xFF1A1A1A), primaryContainer = Color(0xFF263A32), onPrimaryContainer = Color(0xFFE2EEE7),
    secondary = Color(0xFFE6E6EB), onSecondary = Color(0xFF202024), secondaryContainer = Color(0xFF29292F), onSecondaryContainer = Color.White,
    tertiary = Color(0xFFA8C3B9), background = Color(0xFF151C18), onBackground = Color(0xFFF5F5F8),
    surface = Color(0xFF1C1C21), onSurface = Color(0xFFF5F5F8), surfaceVariant = Color(0xFF27272E), onSurfaceVariant = Color(0xFFB4B4BE),
    outline = Color(0xFF8D8D98), outlineVariant = Color(0xFF36363F), error = Color(0xFFFFB3B8))
@Composable fun LifeTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "Dark" || mode == "System" && isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(
        displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 27.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
        titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp), bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    ), content = content)
}
