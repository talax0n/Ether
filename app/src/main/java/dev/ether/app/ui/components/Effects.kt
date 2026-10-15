package dev.ether.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.Sun
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    val x: Float, val delay: Float, val vy: Float, val sway: Float, val swayRate: Float,
    val spin: Float, val color: Color, val round: Boolean,
)

private const val CONFETTI_SECONDS = 3.6f

/**
 * One burst of falling confetti, capped at [count] pieces. Time advances via withFrameNanos into a single
 * float that only the Canvas draw reads, so the burst never recomposes anything. Off under reduced motion.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 40) {
    if (Arcade.reducedMotion) return
    val particles = remember {
        val colors = listOf(Coral, Sun, Mint, Sky, Color.White)
        val rng = Random(7)
        List(count.coerceAtMost(40)) { i ->
            Particle(
                x = 0.04f + rng.nextFloat() * 0.92f, delay = rng.nextFloat() * 0.5f, vy = 0.75f + rng.nextFloat() * 0.5f,
                sway = 10f + rng.nextFloat() * 26f, swayRate = 2f + rng.nextFloat() * 3f,
                spin = rng.nextFloat() * 720f - 360f, color = colors[i % colors.size], round = i % 3 == 0,
            )
        }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (t < CONFETTI_SECONDS) withFrameNanos { t = (it - start) / 1e9f }
    }
    Canvas(modifier) {
        if (t >= CONFETTI_SECONDS) return@Canvas
        val pw = 12.dp.toPx()
        val ph = 18.dp.toPx()
        val outline = Stroke(2.dp.toPx())
        particles.forEach { p ->
            val life = ((t - p.delay) / (CONFETTI_SECONDS - p.delay)).coerceAtLeast(0f)
            if (life <= 0f) return@forEach
            // Ease-out fall, like the mockup's cubic-bezier(.2,.6,.4,1).
            val fall = 1f - (1f - life) * (1f - life)
            val x = p.x * size.width + sin(t * p.swayRate) * p.sway.dp.toPx()
            val y = -ph + fall * (size.height + ph * 2) * p.vy
            rotate(p.spin * life, Offset(x, y)) {
                if (p.round) {
                    drawCircle(p.color, pw / 2, Offset(x, y))
                    drawCircle(Ink, pw / 2, Offset(x, y), style = outline)
                } else {
                    val tl = Offset(x - pw / 2, y - ph / 2)
                    drawRoundRect(p.color, tl, Size(pw, ph), CornerRadius(3.dp.toPx()))
                    drawRoundRect(Ink, tl, Size(pw, ph), CornerRadius(3.dp.toPx()), style = outline)
                }
            }
        }
    }
}

/** Slowly rotating sunburst behind the match celebration. Static under reduced motion. */
@Composable
fun Rays(modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.14f), centerY: Float = 0.4f) {
    val angle = if (Arcade.reducedMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "rays").animateFloat(0f, 360f, infiniteRepeatable(tween(Motion.RAYS_PERIOD_MS, easing = LinearEasing)), label = "angle")
    }
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height * centerY)
        val r = size.maxDimension * 1.2f
        rotate(angle.value, center) {
            for (i in 0 until 18) {
                drawArc(color, i * 20f, 10f, useCenter = true, topLeft = center - Offset(r, r), size = Size(r * 2, r * 2))
            }
        }
    }
}

/** Three bouncing dots: the busy indicator inside buttons and the splash. */
@Composable
fun BouncingDots(color: Color, modifier: Modifier = Modifier) {
    val reduced = Arcade.reducedMotion
    val transition = rememberInfiniteTransition(label = "dots")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val y = if (reduced) null else transition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(560, delayMillis = 0, easing = LinearEasing), RepeatMode.Restart, initialStartOffset = androidx.compose.animation.core.StartOffset(i * 120)),
                label = "dot$i",
            )
            Box(
                Modifier
                    .graphicsLayer {
                        val v = y?.value ?: 0f
                        translationY = -sin(v * Math.PI).toFloat().coerceAtLeast(0f) * 7.dp.toPx()
                    }
                    .size(9.dp)
                    .background(color, CircleShape),
            )
        }
    }
}
