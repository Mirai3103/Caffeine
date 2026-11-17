package com.laffy.caffeine.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CoffeePrimaryDark,
    onPrimary = CoffeeOnPrimaryDark,
    primaryContainer = CoffeePrimaryContainerDark,
    onPrimaryContainer = CoffeeOnPrimaryContainerDark,
    secondary = CoffeeSecondaryDark,
    onSecondary = CoffeeOnSecondaryDark,
    secondaryContainer = CoffeeSecondaryContainerDark,
    onSecondaryContainer = CoffeeOnSecondaryContainerDark,
    tertiary = CoffeeTertiaryDark,
    onTertiary = CoffeeOnTertiaryDark,
    tertiaryContainer = CoffeeTertiaryContainerDark,
    onTertiaryContainer = CoffeeOnTertiaryContainerDark,
    background = CoffeeBackgroundDark,
    onBackground = CoffeeOnBackgroundDark,
    surface = CoffeeBackgroundDark,
    onSurface = CoffeeOnBackgroundDark,
    surfaceVariant = CoffeeSurfaceVariantDark,
    onSurfaceVariant = CoffeeOnSurfaceVariantDark,
    outline = CoffeeOutlineDark,
    outlineVariant = CoffeeOutlineVariantDark,
    surfaceDim = CoffeeBackgroundDark,
    surfaceBright = CoffeeSurfaceBrightDark,
    surfaceContainerLowest = CoffeeSurfaceContainerLowestDark,
    surfaceContainerLow = CoffeeSurfaceContainerLowDark,
    surfaceContainer = CoffeeSurfaceContainerDark,
    surfaceContainerHigh = CoffeeSurfaceContainerHighDark,
    surfaceContainerHighest = CoffeeSurfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeePrimaryLight,
    onPrimary = CoffeeOnPrimaryLight,
    primaryContainer = CoffeePrimaryContainerLight,
    onPrimaryContainer = CoffeeOnPrimaryContainerLight,
    secondary = CoffeeSecondaryLight,
    onSecondary = CoffeeOnSecondaryLight,
    secondaryContainer = CoffeeSecondaryContainerLight,
    onSecondaryContainer = CoffeeOnSecondaryContainerLight,
    tertiary = CoffeeTertiaryLight,
    onTertiary = CoffeeOnTertiaryLight,
    tertiaryContainer = CoffeeTertiaryContainerLight,
    onTertiaryContainer = CoffeeOnTertiaryContainerLight,
    background = CoffeeBackgroundLight,
    onBackground = CoffeeOnBackgroundLight,
    surface = CoffeeBackgroundLight,
    onSurface = CoffeeOnBackgroundLight,
    surfaceVariant = CoffeeSurfaceVariantLight,
    onSurfaceVariant = CoffeeOnSurfaceVariantLight,
    outline = CoffeeOutlineLight,
    outlineVariant = CoffeeOutlineVariantLight,
    surfaceDim = CoffeeSurfaceDimLight,
    surfaceBright = CoffeeBackgroundLight,
    surfaceContainerLowest = CoffeeSurfaceContainerLowestLight,
    surfaceContainerLow = CoffeeSurfaceContainerLowLight,
    surfaceContainer = CoffeeSurfaceContainerLight,
    surfaceContainerHigh = CoffeeSurfaceContainerHighLight,
    surfaceContainerHighest = CoffeeSurfaceContainerHighestLight
)

@Composable
fun CaffeineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content
    )
}
