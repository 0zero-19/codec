package io.codec.desktop.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.codec.model.ThemeMode

object CodecColors {
    // Dark Theme Colors (Deep Cybernetic Slate with Neon Cyan & Violet Accents)
    val DarkBackground = Color(0xFF0F1117)
    val DarkSurface = Color(0xFF171B26)
    val DarkCard = Color(0xFF1E2332)
    val DarkCardElevated = Color(0xFF262C3E)
    val DarkBorder = Color(0xFF2E364B)

    val PrimaryCyan = Color(0xFF00D2FF)
    val PrimaryIndigo = Color(0xFF6C5CE7)
    val AccentGreen = Color(0xFF00E676)
    val AccentOrange = Color(0xFFFF9F43)
    val AccentRed = Color(0xFFFF5252)

    val TextPrimaryDark = Color(0xFFF1F2F6)
    val TextSecondaryDark = Color(0xFFA4B0BE)
    val TextMutedDark = Color(0xFF747D8C)

    // Light Theme Colors
    val LightBackground = Color(0xFFF8F9FD)
    val LightSurface = Color(0xFFFFFFFF)
    val LightCard = Color(0xFFF1F3F9)
    val LightCardElevated = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFE2E6F0)

    val TextPrimaryLight = Color(0xFF1E272E)
    val TextSecondaryLight = Color(0xFF485460)
    val TextMutedLight = Color(0xFF808E9B)

    val darkScheme = darkColorScheme(
        primary = PrimaryCyan,
        onPrimary = Color.Black,
        secondary = PrimaryIndigo,
        onSecondary = Color.White,
        background = DarkBackground,
        onBackground = TextPrimaryDark,
        surface = DarkSurface,
        onSurface = TextPrimaryDark,
        surfaceVariant = DarkCard,
        onSurfaceVariant = TextSecondaryDark,
        outline = DarkBorder,
        error = AccentRed,
        onError = Color.White
    )

    val lightScheme = lightColorScheme(
        primary = Color(0xFF0984E3),
        onPrimary = Color.White,
        secondary = PrimaryIndigo,
        onSecondary = Color.White,
        background = LightBackground,
        onBackground = TextPrimaryLight,
        surface = LightSurface,
        onSurface = TextPrimaryLight,
        surfaceVariant = LightCard,
        onSurfaceVariant = TextSecondaryLight,
        outline = LightBorder,
        error = AccentRed,
        onError = Color.White
    )
}

@Composable
fun CodecTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> CodecColors.lightScheme
        ThemeMode.DARK -> CodecColors.darkScheme
        ThemeMode.SYSTEM -> CodecColors.darkScheme // Default to sleek dark on desktop
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
