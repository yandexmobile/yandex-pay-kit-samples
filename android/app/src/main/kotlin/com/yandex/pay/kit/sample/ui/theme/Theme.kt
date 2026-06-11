package com.yandex.pay.kit.sample.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.yandex.pay.kit.sample.ui.theme.AppShapes
import com.yandex.pay.kit.sample.ui.theme.AppTypography
import com.yandex.pay.kit.sample.ui.theme.DarkColors
import com.yandex.pay.kit.sample.ui.theme.LightColors

@Composable
fun YaPayKitSampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled by default so the brand violet palette always wins.
    // Set to true (or pass true at the call site) to opt into Material You
    // wallpaper-derived colors on Android 12+.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
