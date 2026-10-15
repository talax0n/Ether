package dev.ether.app.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ether.app.R

// Arcade palette (docs/design/directions/arcade.html). Accents are fixed across light and dark.
val Ink = Color(0xFF1E1B2E)
val Cream = Color(0xFFFFF7EA)
val Paper = Color(0xFFFFFFFF)
val Coral = Color(0xFFFF5A36)
val CoralDeep = Color(0xFFD93E1C)
val Sun = Color(0xFFFFC233)
val SunDeep = Color(0xFFE0A100)
val Mint = Color(0xFF2BD47D)
val MintDeep = Color(0xFF16A85C)
val Sky = Color(0xFF3BA7FF)
val Grape = Color(0xFF8B5CF6)
val Cloud = Color(0xFFECE6DA)
val Muted = Color(0xFF5B5670)

// Sticker fills. Text on them is always Ink.
val SunSoft = Color(0xFFFFE7A8)
val MintSoft = Color(0xFFC9F5DD)
val SkySoft = Color(0xFFCFE7FF)
val GrapeSoft = Color(0xFFE4D8FF)
val CoralSoft = Color(0xFFFFD6CB)
val Peach = Color(0xFFFFE1D6)

/** Surfaces and lines that change between light and dark; accents and stickers do not. */
@Immutable
data class ArcadeColors(
    val background: Color,
    val surface: Color,
    /** The 3dp outline on every container. */
    val line: Color,
    /** The solid offset shadow under a container. */
    val ledge: Color,
    val text: Color,
    val muted: Color,
    val track: Color,
    val locked: Color,
    val lockedLine: Color,
    /** Coral dark enough for body-size text on [background] and [surface]. */
    val danger: Color,
    val dark: Boolean,
)

private val LightArcade = ArcadeColors(
    background = Cream, surface = Paper, line = Ink, ledge = Ink, text = Ink, muted = Muted,
    track = Cloud, locked = Color(0xFFF4F0E8), lockedLine = Color(0xFFA9A3B6), danger = Color(0xFFB8321A), dark = false,
)

private val DarkArcade = ArcadeColors(
    background = Color(0xFF17142A), surface = Color(0xFF26223D), line = Color(0xFF07060D), ledge = Color(0xFF07060D),
    text = Cream, muted = Color(0xFFC3BDD6), track = Color(0xFF3A3552), locked = Color(0xFF1F1C33),
    lockedLine = Color(0xFF55506B), danger = Color(0xFFFF8A6E), dark = true,
)

val LocalArcadeColors = staticCompositionLocalOf { LightArcade }

/** True when the system animator duration scale is 0: skip loops, confetti and entrances, show final states. */
val LocalReducedMotion = staticCompositionLocalOf { false }

object Arcade {
    val colors: ArcadeColors
        @Composable @ReadOnlyComposable get() = LocalArcadeColors.current
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

val Nunito = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_extrabold, FontWeight.ExtraBold),
)

/** Every number, timer and XP value: monospace with tabular figures so ticking digits don't jitter. */
val NumberStyle = TextStyle(fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")

private fun nunito(size: Int, weight: FontWeight, line: Int, spacing: Float = 0f) = TextStyle(
    fontFamily = Nunito, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp,
)

// Scale from the spec: 52 display / 30 card name / 22 title / 17 body-bold / 15 body / 13 label caps.
private val ArcadeType = Typography(
    displayLarge = nunito(52, FontWeight.Black, 50, -1f),
    displayMedium = nunito(40, FontWeight.Black, 42, -0.5f),
    displaySmall = nunito(34, FontWeight.Black, 38, -0.5f),
    headlineLarge = nunito(30, FontWeight.Black, 32, -0.5f),
    headlineMedium = nunito(26, FontWeight.Black, 30),
    headlineSmall = nunito(22, FontWeight.Black, 26),
    titleLarge = nunito(20, FontWeight.Black, 24),
    titleMedium = nunito(17, FontWeight.ExtraBold, 22),
    titleSmall = nunito(15, FontWeight.ExtraBold, 20),
    bodyLarge = nunito(16, FontWeight.Bold, 22),
    bodyMedium = nunito(15, FontWeight.Bold, 21),
    bodySmall = nunito(13, FontWeight.Bold, 18),
    labelLarge = nunito(15, FontWeight.Black, 20),
    labelMedium = nunito(13, FontWeight.ExtraBold, 16, 0.2f),
    labelSmall = nunito(12, FontWeight.Black, 14, 1f),
)

val ArcadeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

private fun scheme(a: ArcadeColors) = if (a.dark) {
    darkColorScheme(
        primary = Coral, onPrimary = Ink, primaryContainer = CoralSoft, onPrimaryContainer = Ink,
        secondary = Mint, onSecondary = Ink, tertiary = Sun, onTertiary = Ink,
        background = a.background, onBackground = a.text, surface = a.surface, onSurface = a.text,
        surfaceVariant = a.track, onSurfaceVariant = a.muted, surfaceContainerHigh = a.surface,
        outline = a.line, error = a.danger, inverseSurface = Cream, inverseOnSurface = Ink,
    )
} else {
    lightColorScheme(
        primary = Coral, onPrimary = Ink, primaryContainer = CoralSoft, onPrimaryContainer = Ink,
        secondary = Mint, onSecondary = Ink, tertiary = Sun, onTertiary = Ink,
        background = a.background, onBackground = a.text, surface = a.surface, onSurface = a.text,
        surfaceVariant = a.track, onSurfaceVariant = a.muted, surfaceContainerHigh = a.surface,
        outline = a.line, error = a.danger, inverseSurface = Ink, inverseOnSurface = Cream,
    )
}

@Composable
fun EtherTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val arcade = if (dark) DarkArcade else LightArcade
    CompositionLocalProvider(LocalArcadeColors provides arcade, LocalReducedMotion provides reduced) {
        MaterialTheme(colorScheme = scheme(arcade), typography = ArcadeType, shapes = ArcadeShapes, content = content)
    }
}
