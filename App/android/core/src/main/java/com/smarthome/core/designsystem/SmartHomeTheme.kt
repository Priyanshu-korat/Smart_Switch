package com.smarthome.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SmartHomeDarkColorScheme = darkColorScheme(
    primary            = Brand400,
    onPrimary          = Neutral100,
    primaryContainer   = Brand700,
    onPrimaryContainer = Brand100,

    secondary          = Accent500,
    onSecondary        = Brand900,
    secondaryContainer = Color(0xFF00344A),
    onSecondaryContainer = Accent200,

    tertiary           = On500,
    onTertiary         = Neutral900,

    background         = Neutral900,
    onBackground       = Neutral100,

    surface            = Neutral800,
    onSurface          = Neutral100,
    surfaceVariant     = Neutral750,
    onSurfaceVariant   = Neutral300,

    outline            = Neutral600,
    outlineVariant     = Neutral700,

    error              = Danger500,
    onError            = Neutral100,
    errorContainer     = Color(0xFF4D0014),
    onErrorContainer   = Danger400,

    inverseSurface     = Neutral100,
    inverseOnSurface   = Neutral850,
    inversePrimary     = Brand500,
)

private val SmartHomeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun SmartHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartHomeDarkColorScheme,
        typography = SmartHomeTypography,
        content = content
    )
}
