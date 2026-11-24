package com.supernatural.bwriter.web

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The web build's design language: Apple's — a light neutral ground, white
// surfaces, ink text, one restrained accent. The accent is Bwriter's own
// gold, the colour the Android reader uses for its seal and footnote marks,
// so the two builds read as one product.

val AppleBackground = Color(0xFFF5F5F7)
val AppleSurface = Color(0xFFFFFFFF)
val AppleInk = Color(0xFF1D1D1F)
val AppleSecondary = Color(0xFF6E6E73)
val AppleSeparator = Color(0xFFD2D2D7)
val BwriterGold = Color(0xFF8C6D23)

private val BwriterColorScheme = lightColorScheme(
  primary = BwriterGold,
  onPrimary = Color.White,
  background = AppleBackground,
  onBackground = AppleInk,
  surface = AppleSurface,
  onSurface = AppleInk,
  surfaceVariant = AppleBackground,
  onSurfaceVariant = AppleSecondary,
  outline = AppleSeparator
)

private val BwriterShapes = Shapes(
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun BwriterTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = BwriterColorScheme,
    shapes = BwriterShapes,
    content = content
  )
}
