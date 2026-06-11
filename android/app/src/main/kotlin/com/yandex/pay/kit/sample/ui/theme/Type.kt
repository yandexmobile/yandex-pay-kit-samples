package com.yandex.pay.kit.sample.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultTypography = Typography()

internal val AppTypography = DefaultTypography.copy(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = DefaultTypography.titleSmall.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    labelLarge = DefaultTypography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp,
    ),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        lineHeight = 24.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        lineHeight = 20.sp,
    ),
)
