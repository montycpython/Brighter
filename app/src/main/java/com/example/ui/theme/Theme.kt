package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BookGoldLight,
    onPrimary = InkBlack,
    primaryContainer = Color(0xFF382F1E),
    onPrimaryContainer = BookGoldLight,
    secondary = Color(0xFF8EB5D6),
    onSecondary = InkBlack,
    secondaryContainer = Color(0xFF1E354A),
    onSecondaryContainer = Color(0xFFC7E2F8),
    tertiary = CrimsonSealLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4D1416),
    onTertiaryContainer = Color(0xFFFFDADA),
    background = InkBlack,
    onBackground = TextPrimaryDark,
    surface = InkNavy,
    onSurface = TextPrimaryDark,
    surfaceVariant = InkSlate,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF535B6D),
    outlineVariant = Color(0xFF333845)
)

private val LightColorScheme = lightColorScheme(
    primary = BookGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7ECDA),
    onPrimaryContainer = Color(0xFF44330F),
    secondary = Color(0xFF28547C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E6F5),
    onSecondaryContainer = Color(0xFF0F263B),
    tertiary = CrimsonSeal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD9),
    onTertiaryContainer = Color(0xFF400008),
    background = ParchmentCream,
    onBackground = TextPrimaryLight,
    surface = ParchmentPaper,
    onSurface = TextPrimaryLight,
    surfaceVariant = ParchmentDark,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFB5AD9F),
    outlineVariant = Color(0xFFD9D3C5)
)

@Composable
fun BwriterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep editorial warm parchment aesthetic by default
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BwriterTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
