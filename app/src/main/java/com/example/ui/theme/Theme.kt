package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val id: String) {
    MIDNIGHT_NAVY("midnight_navy"),
    OLED_BLACK("oled_black"),
    ROYAL_PURPLE("royal_purple");

    companion object {
        fun fromId(id: String?): AppThemeMode =
            entries.firstOrNull { it.id == id } ?: MIDNIGHT_NAVY
    }
}

private val MidnightNavyScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF172554),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = RoyalPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = SkyCyan,
    onTertiary = Color(0xFF082F49),
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NavyOutline,
    error = LiveCrimson,
    onError = Color.White,
)

private val OledBlackScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF10214D),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = VividViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C1059),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = SkyCyan,
    onTertiary = Color(0xFF082F49),
    background = OledBackground,
    onBackground = TextPrimary,
    surface = OledSurface,
    onSurface = TextPrimary,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OledOutline,
    error = LiveCrimson,
    onError = Color.White,
)

private val RoyalPurpleScheme = darkColorScheme(
    primary = RoyalPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1B69),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF172554),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = SkyCyan,
    onTertiary = Color(0xFF082F49),
    background = PurpleBackground,
    onBackground = TextPrimary,
    surface = PurpleSurface,
    onSurface = TextPrimary,
    surfaceVariant = PurpleSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = PurpleOutline,
    error = LiveCrimson,
    onError = Color.White,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun D09TvTheme(
    themeMode: AppThemeMode = AppThemeMode.MIDNIGHT_NAVY,
    isArabicScript: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.MIDNIGHT_NAVY -> MidnightNavyScheme
        AppThemeMode.OLED_BLACK -> OledBlackScheme
        AppThemeMode.ROYAL_PURPLE -> RoyalPurpleScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(isArabicScript),
        shapes = AppShapes,
        content = content,
    )
}
