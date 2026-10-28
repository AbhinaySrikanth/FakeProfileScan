package com.fakeprofilescan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// Three colours only: Ink (primary), Paper (surfaces), Amber (attention).
private val Ink = Color(0xFF1D3B4A)
private val Paper = Color(0xFFF5F4F0)
private val PaperDeep = Color(0xFFE8E6DE)
private val Amber = Color(0xFF8A5A00)       // text-safe amber on Paper (>5:1)
private val AmberFill = Color(0xFFE9B04A)

private val InkDark = Color(0xFF9CC3D5)
private val PaperDark = Color(0xFF11181C)
private val PaperDeepDark = Color(0xFF1B2429)
private val AmberDark = Color(0xFFE9B04A)

private val Light = lightColorScheme(
    primary = Ink, onPrimary = Paper, primaryContainer = PaperDeep, onPrimaryContainer = Ink,
    secondary = Ink, onSecondary = Paper,
    tertiary = Amber, onTertiary = Paper, tertiaryContainer = AmberFill, onTertiaryContainer = Ink,
    background = Paper, onBackground = Ink, surface = Paper, onSurface = Ink,
    surfaceVariant = PaperDeep, onSurfaceVariant = Ink.copy(alpha = 0.75f),
    surfaceContainer = PaperDeep, surfaceContainerHigh = PaperDeep, surfaceContainerHighest = PaperDeep,
    outline = Ink.copy(alpha = 0.55f), outlineVariant = Ink.copy(alpha = 0.18f),
    error = Amber, onError = Paper,
)

private val Dark = darkColorScheme(
    primary = InkDark, onPrimary = PaperDark, primaryContainer = PaperDeepDark, onPrimaryContainer = InkDark,
    secondary = InkDark, onSecondary = PaperDark,
    tertiary = AmberDark, onTertiary = PaperDark, tertiaryContainer = AmberDark, onTertiaryContainer = PaperDark,
    background = PaperDark, onBackground = Color(0xFFE6EBEE), surface = PaperDark, onSurface = Color(0xFFE6EBEE),
    surfaceVariant = PaperDeepDark, onSurfaceVariant = Color(0xFFE6EBEE).copy(alpha = 0.75f),
    surfaceContainer = PaperDeepDark, surfaceContainerHigh = PaperDeepDark, surfaceContainerHighest = PaperDeepDark,
    outline = InkDark.copy(alpha = 0.6f), outlineVariant = InkDark.copy(alpha = 0.2f),
    error = AmberDark, onError = PaperDark,
)

private val type = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val shapes = Shapes(
    small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(14.dp),
)

@Composable
fun FakeProfileScanTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = type, shapes = shapes, content = content)
