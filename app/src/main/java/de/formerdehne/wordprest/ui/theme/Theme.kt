package de.formerdehne.wordprest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF1F3A5F)
val Brass = Color(0xFFC9A96E)

private val Light = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E4F3),
    onPrimaryContainer = Color(0xFF0B1D33),
    secondary = Color(0xFF55606E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E7EC),
    onSecondaryContainer = Color(0xFF1B232D),
    tertiary = Color(0xFF8A6A33),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E6CF),
    onTertiaryContainer = Color(0xFF2C1F06),
    background = Color(0xFFF7F7F5),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF7F7F5),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE6E8EB),
    onSurfaceVariant = Color(0xFF494E55),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F2F2),
    surfaceContainerHigh = Color(0xFFEBECED),
    surfaceContainerHighest = Color(0xFFE5E6E8),
    outline = Color(0xFF8A9099),
    outlineVariant = Color(0xFFD4D7DB),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFAFC8EC),
    onPrimary = Color(0xFF0B2545),
    primaryContainer = Color(0xFF26446D),
    onPrimaryContainer = Color(0xFFD9E4F3),
    secondary = Color(0xFFBCC5D1),
    onSecondary = Color(0xFF26303B),
    secondaryContainer = Color(0xFF3B4551),
    onSecondaryContainer = Color(0xFFDDE3EA),
    tertiary = Color(0xFFDCC08D),
    onTertiary = Color(0xFF3F2E0B),
    tertiaryContainer = Color(0xFF5A4520),
    onTertiaryContainer = Color(0xFFF3E6CF),
    background = Color(0xFF111316),
    onBackground = Color(0xFFE3E4E6),
    surface = Color(0xFF111316),
    onSurface = Color(0xFFE3E4E6),
    surfaceVariant = Color(0xFF2B2F35),
    onSurfaceVariant = Color(0xFFC2C6CC),
    surfaceContainerLowest = Color(0xFF0C0E10),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF24282C),
    surfaceContainerHighest = Color(0xFF2E3237),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF41464D),
    error = Color(0xFFF2B8B5),
)

private val base = Typography()
private val AppTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun WordPrestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
