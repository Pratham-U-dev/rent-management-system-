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

private val DarkColorScheme =
  darkColorScheme(
    primary = MinimalPurpleLight,
    secondary = MinimalTeal,
    tertiary = MinimalRed,
    background = CleanTextPrimary,
    surface = CleanTextPrimary,
    onPrimary = MinimalPurpleDark,
    onSecondary = White,
    onBackground = CleanBg,
    onSurface = CleanBg,
    outlineVariant = CleanTextSubdued
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalPurple,
    onPrimary = White,
    primaryContainer = MinimalPurpleLight,
    onPrimaryContainer = MinimalPurpleDark,
    secondary = MinimalTeal,
    onSecondary = White,
    secondaryContainer = MinimalTealLight,
    onSecondaryContainer = MinimalTeal,
    tertiary = MinimalRed,
    onTertiary = White,
    tertiaryContainer = MinimalRedLight,
    background = CleanBg,
    surface = White,
    onBackground = CleanTextPrimary,
    onSurface = CleanTextPrimary,
    surfaceVariant = Color(0xFFF3F3FA),
    onSurfaceVariant = CleanTextSubdued,
    outline = BorderLight,
    outlineVariant = BorderExtraLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable Android 12+ dynamic coloring by default to strictly enforce the requested Clean Minimalism theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
