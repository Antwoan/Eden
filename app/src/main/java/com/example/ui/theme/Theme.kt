package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.viewmodel.AppThemeSetting

private val ArcaneColorScheme = darkColorScheme(
    primary = ArcaneGold,
    onPrimary = Color(0xFF1E1500),
    primaryContainer = ArcaneGoldDark,
    onPrimaryContainer = Color(0xFFFFECCC),
    secondary = ArcanePurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFE9D5FF),
    background = ArcaneBackground,
    onBackground = Color(0xFFF3F0FA),
    surface = ArcaneSurface,
    onSurface = Color(0xFFF3F0FA),
    surfaceVariant = ArcaneSurfaceVariant,
    onSurfaceVariant = Color(0xFFD4CEE2),
    outline = ArcaneBorder
)

private val ParchmentColorScheme = darkColorScheme(
    primary = ParchmentCopper,
    onPrimary = Color.Black,
    secondary = ParchmentAccent,
    onSecondary = Color.Black,
    background = ParchmentBackground,
    onBackground = Color(0xFFF5EBE0),
    surface = ParchmentSurface,
    onSurface = Color(0xFFF5EBE0),
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = Color(0xFFD5BDAF),
    outline = Color(0xFF4A3B32)
)

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = CyberPink,
    onSecondary = Color.White,
    background = CyberBackground,
    onBackground = Color(0xFFE0F7FA),
    surface = CyberSurface,
    onSurface = Color(0xFFE0F7FA),
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = Color(0xFF80DEEA),
    outline = Color(0xFF1E3A5F)
)

private val CaiColorScheme = darkColorScheme(
    primary = CaiAccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color(0xFFF4F4F5),
    background = CaiBackground,
    onBackground = CaiTextPrimary,
    surface = CaiSurface,
    onSurface = CaiTextPrimary,
    surfaceVariant = CaiSurfaceVariant,
    onSurfaceVariant = CaiTextSecondary,
    outline = CaiBorder,
    outlineVariant = Color(0xFF1F1F23)
)

private val OledColorScheme = darkColorScheme(
    primary = OledEmerald,
    onPrimary = Color.Black,
    secondary = Color(0xFF60A5FA),
    onSecondary = Color.Black,
    background = OledBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = OledSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF27272A)
)

@Composable
fun MythosRoleplayTheme(
    appTheme: AppThemeSetting = AppThemeSetting.CHARACTER_AI_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeSetting.CHARACTER_AI_DARK -> CaiColorScheme
        AppThemeSetting.DARK_FANTASY -> ArcaneColorScheme
        AppThemeSetting.PARCHMENT -> ParchmentColorScheme
        AppThemeSetting.CYBERPUNK -> CyberColorScheme
        AppThemeSetting.OLED_BLACK -> OledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
