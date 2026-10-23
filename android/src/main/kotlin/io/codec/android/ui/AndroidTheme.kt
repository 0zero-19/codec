package io.codec.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.codec.model.ThemeMode

object AndroidColors {
    val DarkBackground = Color(0xFF0F1117)
    val DarkSurface = Color(0xFF171B26)
    val DarkCard = Color(0xFF1E2332)
    val DarkBorder = Color(0xFF2E364B)

    val PrimaryCyan = Color(0xFF00D2FF)
    val PrimaryIndigo = Color(0xFF6C5CE7)
    val AccentGreen = Color(0xFF00E676)
    val AccentOrange = Color(0xFFFF9F43)
    val AccentRed = Color(0xFFFF5252)

    val TextPrimaryDark = Color(0xFFF1F2F6)
    val TextSecondaryDark = Color(0xFFA4B0BE)

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
        background = Color(0xFFF8F9FD),
        onBackground = Color(0xFF1E272E),
        surface = Color.White,
        onSurface = Color(0xFF1E272E),
        surfaceVariant = Color(0xFFF1F3F9),
        onSurfaceVariant = Color(0xFF485460),
        outline = Color(0xFFE2E6F0),
        error = AccentRed,
        onError = Color.White
    )
}

@Composable
fun CodecAndroidTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val scheme = when (themeMode) {
        ThemeMode.LIGHT -> AndroidColors.lightScheme
        else -> AndroidColors.darkScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
