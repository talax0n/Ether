package dev.ether.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Rounded 2.6-stroke icons, path data copied from the Arcade mockup. Tint them with `Icon(tint = …)`. */
object ArcadeIcons {
    val Heart = icon("M12 20s-7-4.4-9.2-9A5 5 0 0 1 12 6a5 5 0 0 1 9.2 5C19 15.6 12 20 12 20z")
    val HeartFilled = icon("M12 20s-7-4.4-9.2-9A5 5 0 0 1 12 6a5 5 0 0 1 9.2 5C19 15.6 12 20 12 20z", filled = true)
    val Close = icon("M6 6l12 12M18 6L6 18")
    val Bolt = icon("M13 2L4 14h7l-1 8 9-12h-7z", filled = true)
    val Back = icon("M15 5l-7 7 7 7", autoMirror = true)
    val Eye = icon("M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12zM9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0")
    val EyeOff = icon("M3 3l18 18M10.6 5.1A10.5 10.5 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-2.6 3.4M6.6 6.6A17 17 0 0 0 2 12s3.6 7 10 7a9.7 9.7 0 0 0 5.4-1.6M9.9 9.9a3 3 0 0 0 4.2 4.2")
    val Lock = icon("M7 11h10a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2zM8 11V8a4 4 0 0 1 8 0v3")
    val Trophy = icon("M8 4h8v5a4 4 0 0 1-8 0zM8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8 20h8")
    val Users = icon("M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0M3 20a6 6 0 0 1 12 0M16 4.5a3.5 3.5 0 0 1 0 7M18 14a6 6 0 0 1 3 6")
    val User = icon("M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0M4 21a8 8 0 0 1 16 0")
    val Star = icon("M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z")
    val Flag = icon("M5 21V4M5 4h11l-2 4 2 4H5")
    val Shield = icon("M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6zM9 12l2 2 4-4")
    val Chat = icon("M4 20l1.5-4A8 8 0 1 1 9 19.5z")
    val Phone = icon("M6 3h3l2 5-2.5 1.5a11 11 0 0 0 5 5L15 12l5 2v3a2 2 0 0 1-2 2A15 15 0 0 1 4 5a2 2 0 0 1 2-2z")
    val Camera = icon("M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4zM8.5 12a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0M17 7h.01")
    val Edit = icon("M4 20h4L19 9l-4-4L4 16zM14 6l4 4")
    val ThumbUp = icon("M7 11v9H4v-9zM7 11l4-7a2 2 0 0 1 3 2l-1 4h5a2 2 0 0 1 2 2.3l-1.2 6A2 2 0 0 1 16.8 20H7")
    val Check = icon("M5 12.5l4.5 4.5L19 7.5")
    val Calendar = icon("M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2zM4 10h16M8 3v4M16 3v4")
    val Link = icon("M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
    val WifiOff = icon("M3 3l18 18M8.5 16.5a5 5 0 0 1 7 0M5 12.9a10 10 0 0 1 5-2.7M19 12.9a10 10 0 0 0-2.5-1.8M2 8.8a15 15 0 0 1 4.3-2.7M22 8.8A15 15 0 0 0 11 5M12 20h.01")
    val Plus = icon("M12 5v14M5 12h14")
    val Search = icon("M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0M16.2 16.2L20 20")
    val Pin = icon("M12 21s-6.5-6.2-6.5-11a6.5 6.5 0 0 1 13 0c0 4.8-6.5 11-6.5 11zM9.8 10a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0-4.4 0")
    val Globe = icon("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M3 12h18M12 3c2.6 2.6 3.8 5.6 3.8 9s-1.2 6.4-3.8 9c-2.6-2.6-3.8-5.6-3.8-9S9.4 5.6 12 3z")
    /** A phone sending signal waves: Match nearby. */
    val Nearby = icon("M5 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM8 17.5h.01M16.5 9a4 4 0 0 1 0 6M19.5 6.5a8 8 0 0 1 0 11")
    /** Two slider knobs: filters. */
    val Sliders = icon("M4 7h9M17 7h3M4 17h3M11 17h9M15 5v4M9 15v4")
    val Logout = icon("M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 17l-5-5 5-5M5 12h11")

    private fun icon(path: String, filled: Boolean = false, autoMirror: Boolean = false): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = autoMirror)
            .addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
}
