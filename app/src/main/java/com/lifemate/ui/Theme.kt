package com.lifemate.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Jade = Color(0xFF246B55)
val Lime = Color(0xFFD9ECC0)
val Apricot = Color(0xFFF7DDC4)
private val Light = lightColorScheme(primary = Jade, onPrimary = Color.White, primaryContainer = Color(0xFFE0EFDF), onPrimaryContainer = Color(0xFF204D3F),
    secondary = Color(0xFFB98050), secondaryContainer = Apricot, background = Color(0xFFF7F9F5), onBackground = Color(0xFF203B32),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF203B32), surfaceVariant = Color(0xFFEDF1E9), onSurfaceVariant = Color(0xFF778079), outline = Color(0xFF8B968C), outlineVariant = Color(0xFFE1E7DE))
private val Dark = darkColorScheme(primary = Color(0xFFAFD9B5), onPrimary = Color(0xFF153D2D), primaryContainer = Color(0xFF274E3E), onPrimaryContainer = Color(0xFFD9ECC0),
    secondary = Color(0xFFE5BD96), secondaryContainer = Color(0xFF573D29), background = Color(0xFF111D18), onBackground = Color(0xFFE5EEE4),
    surface = Color(0xFF1A2921), onSurface = Color(0xFFE5EEE4), surfaceVariant = Color(0xFF29382E), onSurfaceVariant = Color(0xFFA6B6AA), outlineVariant = Color(0xFF35463A))
@Composable fun LifeTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "Dark" || mode == "System" && isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(
        displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 42.sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
        titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp), bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    ), content = content)
}
