package org.emvtools.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Color palette
val Primary = Color(0xFF1976D2)
val PrimaryVariant = Color(0xFF1565C0)
val Secondary = Color(0xFF26A69A)
val SecondaryVariant = Color(0xFF00897B)
val Background = Color(0xFFFAFAFA)
val Surface = Color(0xFFFFFFFF)
val Error = Color(0xFFD32F2F)
val OnPrimary = Color.White
val OnSecondary = Color.White
val OnBackground = Color(0xFF212121)
val OnSurface = Color(0xFF212121)
val OnError = Color.White

// Dark theme colors
val PrimaryDark = Color(0xFF90CAF9)
val PrimaryVariantDark = Color(0xFF64B5F6)
val SecondaryDark = Color(0xFF80CBC4)
val SecondaryVariantDark = Color(0xFF4DB6AC)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val ErrorDark = Color(0xFFEF5350)
val OnPrimaryDark = Color(0xFF212121)
val OnSecondaryDark = Color(0xFF212121)
val OnBackgroundDark = Color(0xFFE0E0E0)
val OnSurfaceDark = Color(0xFFE0E0E0)
val OnErrorDark = Color(0xFF212121)

// Category colors
val EmvColor = Color(0xFF1976D2)
val CryptoColor = Color(0xFF7B1FA2)
val BankingColor = Color(0xFF388E3C)
val MiscColor = Color(0xFFF57C00)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
    onError = OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryVariantDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryVariantDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark
)

@Composable
fun EmvToolsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

