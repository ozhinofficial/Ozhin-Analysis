package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldDark,
  onPrimary = Color(0xFF022C22),
  primaryContainer = EmeraldContainerDark,
  onPrimaryContainer = EmeraldOnContainerDark,
  secondary = TealSecondaryLight,
  onSecondary = Color(0xFF042F2E),
  secondaryContainer = TealContainerDark,
  onSecondaryContainer = Color(0xFF99F6E4),
  tertiary = SubscriptionPurple,
  onTertiary = Color.White,
  background = BackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = CardBorderDark,
  error = ExpenseRed,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = Color.White,
  primaryContainer = EmeraldContainerLight,
  onPrimaryContainer = EmeraldOnContainerLight,
  secondary = TealSecondary,
  onSecondary = Color.White,
  secondaryContainer = TealContainerLight,
  onSecondaryContainer = Color(0xFF115E59),
  tertiary = SubscriptionPurple,
  onTertiary = Color.White,
  background = BackgroundLight,
  onBackground = TextPrimaryLight,
  surface = SurfaceLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
  outline = CardBorderLight,
  error = ExpenseRed,
  onError = Color.White
)

@Composable
fun FinanceTrackerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
