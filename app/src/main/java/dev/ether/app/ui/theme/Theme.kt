package dev.ether.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val Ember = Color(0xFFFF5A36)
private val EmberDeep = Color(0xFFC63A1C)
private val EmberSoft = Color(0xFFFFDBD1)
private val Volt = Color(0xFF00B894)
private val VoltSoft = Color(0xFFB8F5E4)
private val Sun = Color(0xFFFFB800)
private val SunSoft = Color(0xFFFFE8A8)
private val Ink = Color(0xFF16131A)
private val InkRaised = Color(0xFF221E27)
private val InkHigh = Color(0xFF2E2934)
private val Paper = Color(0xFFFFF8F4)
private val PaperRaised = Color(0xFFFFFFFF)

/** Stamp colors are fixed across themes so LIKE always reads green and NOPE red. */
val LikeGreen = Color(0xFF00C48C)
val NopeRed = Color(0xFFFF3B5C)

private val Light = lightColorScheme(
    primary = Ember,
    onPrimary = Color.White,
    primaryContainer = EmberSoft,
    onPrimaryContainer = Color(0xFF3D0B00),
    secondary = Volt,
    onSecondary = Color.White,
    secondaryContainer = VoltSoft,
    onSecondaryContainer = Color(0xFF00382A),
    tertiary = Sun,
    onTertiary = Ink,
    tertiaryContainer = SunSoft,
    onTertiaryContainer = Color(0xFF3A2A00),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF5E6E0),
    onSurfaceVariant = Color(0xFF5A4842),
    surfaceContainerLowest = PaperRaised,
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFCEBE4),
    surfaceContainerHigh = Color(0xFFF7E4DC),
    surfaceContainerHighest = Color(0xFFF1DDD5),
    outline = Color(0xFF8E7770),
    error = NopeRed,
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF7A5C),
    onPrimary = Color(0xFF3D0B00),
    primaryContainer = EmberDeep,
    onPrimaryContainer = EmberSoft,
    secondary = Color(0xFF3FE0B5),
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF005F48),
    onSecondaryContainer = VoltSoft,
    tertiary = Color(0xFFFFCB45),
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF5C4400),
    onTertiaryContainer = SunSoft,
    background = Ink,
    onBackground = Color(0xFFF2E9EE),
    surface = Ink,
    onSurface = Color(0xFFF2E9EE),
    surfaceVariant = InkHigh,
    onSurfaceVariant = Color(0xFFCDBFC6),
    surfaceContainerLowest = Color(0xFF100E13),
    surfaceContainerLow = Color(0xFF1C1820),
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = Color(0xFF39333F),
    outline = Color(0xFF978A91),
    error = Color(0xFFFF6B83),
)

private val BaseType = Typography()
private val EtherType = BaseType.copy(
    displaySmall = BaseType.displaySmall.copy(fontWeight = FontWeight.Black),
    headlineMedium = BaseType.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = BaseType.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseType.titleLarge.copy(fontWeight = FontWeight.Bold),
    labelLarge = BaseType.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun EtherTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = EtherType, content = content)
}
