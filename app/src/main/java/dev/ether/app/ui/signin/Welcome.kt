package dev.ether.app.ui.signin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.EthaLive
import dev.ether.app.ui.components.EthaMove
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.Wordmark
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.MintSoft
import dev.ether.app.ui.theme.Peach
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.SunSoft
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One student bubble in the welcome burst, placed relative to the centre. Far bubbles sit back, faded. */
private class Bubble(val name: String, val color: Color, val x: Int, val y: Int, val size: Int, val far: Boolean = false)

// ponytail: hand-placed, not laid out; tuned for phone widths, the burst is decoration and may clip at the edges.
private val bubbles = listOf(
    Bubble("Joko Widodo", GrapeSoft, -190, -250, 130, far = true),
    Bubble("Kirana Ayu", SkySoft, 200, 230, 130, far = true),
    Bubble("Gita Sari", MintSoft, -70, -150, 52, far = true),
    Bubble("Hadi Putra", Peach, 120, -190, 58, far = true),
    Bubble("Intan Lestari", SunSoft, -175, 150, 56, far = true),
    Bubble("Nadia Rahma", MintSoft, 10, -215, 78),
    Bubble("Budi Santoso", SunSoft, -150, -95, 66),
    Bubble("Citra Dewi", SkySoft, 160, -60, 72),
    Bubble("Dimas Pratama", Peach, -165, 55, 62),
    Bubble("Eka Putri", GrapeSoft, 140, 105, 60),
    Bubble("Fajar Nugroho", SunSoft, -5, 195, 84),
)

/**
 * First screen for a signed-out user who has seen the intro: Etha and the wordmark at the centre of a burst of
 * student bubbles, then the two ways in. [onGoogle] is null when this build has no Google sign-in.
 */
@Composable
internal fun Welcome(onGoogle: (() -> Unit)?, onEmail: () -> Unit, busy: Boolean, error: String?) {
    val colors = Arcade.colors
    Column(Modifier.fillMaxSize()) {
        Burst(Modifier.weight(1f).fillMaxWidth())
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Welcome to Ether", Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge, color = colors.text, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Swipe to find your hackathon team", style = MaterialTheme.typography.bodyLarge,
                color = colors.muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            val pill = RoundedCornerShape(50)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                onGoogle?.let { GoogleButton(it, enabled = !busy, shape = pill) }
                LedgeButton(
                    "Continue with email", onEmail, Modifier.fillMaxWidth(),
                    enabled = !busy, icon = ArcadeIcons.Bolt, height = 61.dp, shape = pill,
                )
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorText(it)
            }
        }
    }
}

/** Rays from the centre, student bubbles popping in and bobbing, Etha and the wordmark in the middle. */
@Composable
private fun Burst(modifier: Modifier) {
    val reduced = Arcade.reducedMotion
    val phase: State<Float> = if (reduced) remember { mutableFloatStateOf(0f) } else rememberInfiniteTransition(label = "burst").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "t",
    )
    val t by phase
    val ray = Arcade.colors.track
    BoxWithConstraints(modifier.clipToBounds().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        // Fit the burst's ~520dp design height to whatever space is left above the buttons.
        val scale = (maxHeight / 540.dp).coerceIn(0.6f, 1.1f)
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val reach = size.maxDimension
            repeat(RAYS) { i ->
                val a = (2 * PI * i / RAYS).toFloat() + t / 12f
                drawLine(
                    ray, c + Offset(cos(a), sin(a)) * 70.dp.toPx(), c + Offset(cos(a), sin(a)) * reach,
                    strokeWidth = (if (i % 2 == 0) 3f else 1.5f).dp.toPx(), cap = StrokeCap.Round,
                )
            }
        }
        bubbles.forEachIndexed { i, b -> BubbleView(b, i, t, scale) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
            EthaLive(size = 150.dp, first = EthaMove.WAVE, pool = EthaMove.FIDGETS + EthaMove.WAVE)
            Wordmark(Modifier.offset(y = (-8).dp))
        }
    }
}

@Composable
private fun BubbleView(b: Bubble, index: Int, t: Float, scale: Float) {
    val reduced = Arcade.reducedMotion
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(120L + index * 70L)
        pop.animateTo(1f, dev.ether.app.ui.theme.Motion.pop())
    }
    val bob: Dp = (if (b.far) 4 else 8).dp
    Avatar(
        b.name, null, b.size.dp,
        Modifier
            .offset((b.x * scale).dp, (b.y * scale).dp)
            .graphicsLayer {
                val s = pop.value * scale
                scaleX = s; scaleY = s
                translationY = sin(t + index) * bob.toPx()
                alpha = if (b.far) 0.3f else 1f
            },
        color = b.color,
    )
}

private const val RAYS = 24
