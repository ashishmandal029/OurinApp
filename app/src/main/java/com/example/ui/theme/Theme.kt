package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val BentoColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    primaryContainer = BentoPurpleAccent,
    onPrimaryContainer = BentoPurpleText,
    secondaryContainer = BentoPurpleAccent,
    onSecondaryContainer = BentoPurpleText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Use light theme for Bento styling
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = BentoColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
