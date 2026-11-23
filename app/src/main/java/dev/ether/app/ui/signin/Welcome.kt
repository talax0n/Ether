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
import androidx.compose.ui.graphics.CompositingStrategy
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
import dev.ether.app.R
import androidx.annotation.DrawableRes
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.EthaLive
import dev.ether.app.ui.components.rememberEthaController
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Motion
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * One thing floating in the welcome burst, placed relative to the centre. Near ones are students with the
 * role they bring; far ones are the skills a team is built from, faded back for depth.
 */
private class Bubble(
    val name: String, val label: String, val color: Color, val x: Int, val y: Int,
    val size: Int = 0, @DrawableRes val face: Int? = null, val far: Boolean = false,
)

// ponytail: hand-placed, not laid out; tuned for phone widths, the burst is decoration and may clip at the edges.
private val bubbles = listOf(
    Bubble("", "Kotlin", SkySoft, -150, -250, far = true),
    Bubble("", "Figma", Peach, 150, -235, far = true),
    Bubble("", "Python", MintSoft, -185, 170, far = true),
    Bubble("", "Pitching", SunSoft, 165, 225, far = true),
    Bubble("", "React", GrapeSoft, 205, 20, far = true),
    Bubble("Nadia Rahma", "Android", MintSoft, 10, -205, 78, R.drawable.face_nadia),
    Bubble("Budi Santoso", "Designer", SunSoft, -150, -95, 72, R.drawable.face_budi),
    Bubble("Citra Dewi", "Frontend", SkySoft, 155, -85, 76, R.drawable.face_citra),
    Bubble("Dimas Pratama", "Hardware", Peach, -160, 70, 70, R.drawable.face_dimas),
    Bubble("Eka Putri", "Data / ML", GrapeSoft, 135, 110, 70, R.drawable.face_eka),
    Bubble("Fajar Nugroho", "Backend", SunSoft, -5, 200, 80, R.drawable.face_fajar),
)

/**
 * First screen for a signed-out user who has seen the intro: Etha and the wordmark at the centre of a burst of
 * student bubbles, then the two ways in. [onGoogle] is null when this build has no Google sign-in.
 */
@Composable
internal fun Welcome(
    onGoogle: (() -> Unit)?,
    onEmail: () -> Unit,
    busy: Boolean,
    error: String?,
    /** Applied to Etha, so a shared-element transition can fly it to the next screen. */
    ethaModifier: Modifier = Modifier,
    /** 0 while shown, 1 when gone: bubbles fly outward and fade as it rises. */
    scatter: () -> Float = { 0f },
) {
    val colors = Arcade.colors
    Column(Modifier.fillMaxSize()) {
        Burst(Modifier.weight(1f).fillMaxWidth(), ethaModifier, scatter)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Welcome to Ether", Modifier.staggerIn(6).semantics { heading() },
                style = MaterialTheme.typography.headlineLarge, color = colors.text, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Swipe to find your hackathon team", Modifier.staggerIn(7), style = MaterialTheme.typography.bodyLarge,
                color = colors.muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            val pill = RoundedCornerShape(50)
            Column(Modifier.staggerIn(8), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

/**
 * Rays from the centre; student bubbles pop in, bob, and slowly orbit (far ones the other way, for depth);
 * every so often one sparks a heart, like a match happening; Etha and the wordmark land last.
 */
@Composable
private fun Burst(modifier: Modifier, ethaModifier: Modifier, scatter: () -> Float) {
    val reduced = Arcade.reducedMotion
    val loop = rememberInfiniteTransition(label = "burst")
    val still = remember { mutableFloatStateOf(0f) }
    val phase: State<Float> = if (reduced) still else
        loop.animateFloat(0f, TAU, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "bob")
    // One full turn a minute: slow enough to read as drift, and a whole turn so the loop never jumps.
    val orbit: State<Float> = if (reduced) still else
        loop.animateFloat(0f, TAU, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "orbit")
    var spark by remember { mutableStateOf(Spark(-1, 0)) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        delay(1_400)
        val near = bubbles.indices.filter { !bubbles[it].far }
        while (true) {
            spark = Spark(near.random(), spark.seq + 1)
            delay(Random.nextLong(1_100, 2_000))
        }
    }
    val centre = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(120L + bubbles.size * 70L)
        centre.animateTo(1f, Motion.pop())
    }
    val ray = Arcade.colors.track
    BoxWithConstraints(modifier.clipToBounds().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        // Fit the burst's ~520dp design height to whatever space is left above the buttons.
        val scale = (maxHeight / 540.dp).coerceIn(0.6f, 1.1f)
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val reach = size.maxDimension
            // Static rays: redrawing them every frame cost more than the slow turn was worth.
            repeat(RAYS) { i ->
                val a = TAU * i / RAYS
                drawLine(
                    ray, c + Offset(cos(a), sin(a)) * 70.dp.toPx(), c + Offset(cos(a), sin(a)) * reach,
                    strokeWidth = (if (i % 2 == 0) 3f else 1.5f).dp.toPx(), cap = StrokeCap.Round,
                )
            }
        }
        bubbles.forEachIndexed { i, b -> BubbleView(b, i, { phase.value }, { orbit.value }, scale, spark, scatter) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val p = centre.value
                scaleX = scale * (0.6f + 0.4f * p); scaleY = scaleX
                alpha = p.coerceIn(0f, 1f)
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        ) {
            FloatingEtha({ phase.value }, ethaModifier)
            Wordmark(Modifier.offset(y = (-8).dp))
        }
    }
}

/**
 * Etha floating at the burst's centre: it bobs and sways on [bob], really leaves the ground when it plays its hop,
 * and a shadow under it shrinks as it rises. Tapping it still gets a giggle.
 */
@Composable
private fun FloatingEtha(bob: () -> Float, modifier: Modifier) {
    val etha = rememberEthaController()
    val jump = remember { Animatable(0f) }
    LaunchedEffect(etha.take) {
        if (etha.move != EthaMove.HOP) return@LaunchedEffect
        val hop = EthaMove.HOP
        // Crouch frame first, then up and down across the airborne frames.
        delay(hop.frameMs)
        val air = ((hop.frames - 2) * hop.frameMs).toInt()
        jump.animateTo(1f, tween(air / 2, easing = FastOutSlowInEasing))
        jump.animateTo(0f, tween(air / 2, easing = LinearOutSlowInEasing))
    }
    val size = 150.dp
    Box(contentAlignment = Alignment.BottomCenter) {
        // Ground shadow: stays put while Etha floats, narrows and fades as it goes up.
        Canvas(Modifier.size(size * 0.62f, 14.dp).offset(y = (-6).dp)) {
            val lift = (sin(bob()) + 1f) / 2f * 0.25f + jump.value * 0.6f
            val w = size.toPx() * 0.5f * (1f - lift * 0.5f)
            drawOval(
                Color.Black.copy(alpha = 0.12f * (1f - lift * 0.6f)),
                topLeft = Offset((this.size.width - w) / 2f, 0f),
                size = androidx.compose.ui.geometry.Size(w, this.size.height),
            )
        }
        EthaLive(
            modifier.graphicsLayer {
                val b = bob()
                translationY = (sin(b) * 7.dp.toPx()) - 10.dp.toPx() - jump.value * 34.dp.toPx()
                rotationZ = sin(b * 2f) * 3f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
            size = size,
            controller = etha,
            first = EthaMove.WAVE,
            pool = EthaMove.FIDGETS + EthaMove.WAVE + EthaMove.HOP + EthaMove.HOP,
            restMs = 1_800L..3_500L,
        )
    }
}

/** Bubble [index] sparks a heart; [seq] makes a repeat on the same bubble a new event. */
private data class Spark(val index: Int, val seq: Int)

@Composable
private fun BubbleView(b: Bubble, index: Int, bob: () -> Float, orbit: () -> Float, scale: Float, spark: Spark, scatter: () -> Float) {
    val reduced = Arcade.reducedMotion
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(120L + index * 70L)
        pop.animateTo(1f, Motion.pop())
    }
    val pulse = remember { Animatable(0f) }
    val heart = remember { Animatable(1f) }
    LaunchedEffect(spark) {
        if (spark.index != index) return@LaunchedEffect
        launch { pulse.snapTo(1f); pulse.animateTo(0f, tween(600)) }
        heart.snapTo(0f)
        heart.animateTo(1f, tween(1_100, easing = LinearEasing))
    }
    val lift: Dp = (if (b.far) 4 else 8).dp
    val sizeDp = b.size.dp
    Box(
        Modifier.graphicsLayer {
            val a = if (b.far) -orbit() else orbit()
            // Rotate the hand-placed spot around the centre on an upright ellipse: the screen is taller than wide,
            // so students and their role tags stay on screen as they come round the sides.
            // Leaving, every bubble flies straight out from the centre; far ones go further, for depth.
            val out = 1f + scatter() * (if (b.far) 1.6f else 1.1f)
            translationX = (b.x * cos(a) - b.y * sin(a)) * scale * density * (if (b.far) 1f else 0.75f) * out
            translationY = (b.x * sin(a) + b.y * cos(a)) * scale * density * out + sin(bob() + index) * lift.toPx()
            alpha = 1f - scatter()
        },
        contentAlignment = Alignment.Center,
    ) {
        val look = Modifier.graphicsLayer {
            val s = pop.value * scale * (1f + 0.12f * pulse.value)
            scaleX = s; scaleY = s
            alpha = if (b.far) 0.35f else 1f
            // Fade by multiplying each draw instead of compositing an offscreen buffer every frame.
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
        if (b.far) {
            Chip(b.label, look, color = b.color)
        } else {
            // A student and the role they bring; the tag tucks under the avatar like a name badge.
            Column(look, horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(b.name, null, sizeDp, color = b.color, face = b.face)
                Chip(b.label, Modifier.offset(y = (-10).dp), color = Paper)
            }
        }
        if (heart.value < 1f) {
            Icon(
                ArcadeIcons.HeartFilled, null, tint = Coral,
                modifier = Modifier.size(26.dp).graphicsLayer {
                    val p = heart.value
                    translationX = sizeDp.toPx() * 0.35f * scale
                    // From the avatar's top-right, above the role tag.
                    translationY = -(sizeDp.toPx() * 0.5f + 10.dp.toPx()) * scale - p * 46.dp.toPx()
                    val s = if (p < 0.2f) p / 0.2f else 1f
                    scaleX = s; scaleY = s
                    alpha = 1f - p
                },
            )
        }
    }
}

private const val RAYS = 24
private const val TAU = (2 * PI).toFloat()
