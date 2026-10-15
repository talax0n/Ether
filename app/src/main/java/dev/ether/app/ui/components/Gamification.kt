package dev.ether.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Grape
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.theme.Sun
import dev.ether.app.ui.theme.SunDeep
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.math.floor
import kotlin.math.roundToInt

/** Sun disc with a bolt: the "energy" a swipe spends. */
@Composable
fun BoltSticker(modifier: Modifier = Modifier, size: Dp = 28.dp, color: Color = Sun) {
    Box(modifier.size(size).border(2.dp, Ink, CircleShape).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Icon(ArcadeIcons.Bolt, contentDescription = null, tint = Ink, modifier = Modifier.size(size * 0.55f))
    }
}

/** Daily swipe cap as a 30-tick energy meter. Ticks drain in the draw phase; only the number recomposes. */
@Composable
fun EnergyMeter(remaining: Int, modifier: Modifier = Modifier, total: Int = 30) {
    val animated by animateFloatAsState(remaining.toFloat(), Motion.settle(), label = "energy")
    val track = Arcade.colors.track
    val low = remaining <= 5
    Row(
        modifier
            .ledge(CircleShape, Arcade.colors.surface, 3.dp)
            .height(46.dp)
            .padding(start = 8.dp, end = 14.dp)
            .clearAndSetSemantics { contentDescription = "$remaining of $total swipes left today" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoltSticker(color = if (low) Coral else Sun)
        Canvas(Modifier.weight(1f).height(16.dp)) {
            val gap = 2.dp.toPx()
            val w = (size.width - gap * (total - 1)) / total
            val full = floor(animated).toInt()
            val partial = animated - full
            val on = if (low) Coral else Sun
            for (i in 0 until total) {
                val color = when {
                    i < full -> on
                    i == full && partial > 0f -> lerp(track, on, partial)
                    else -> track
                }
                // Lit ticks stand a touch taller, like a level meter.
                val h = if (i < full) size.height else size.height * 0.7f
                drawRoundRect(color, Offset(i * (w + gap), size.height - h), Size(w, h), CornerRadius(w / 2))
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$remaining", style = NumberStyle.copy(fontSize = 17.sp), color = Arcade.colors.text)
            Text("/$total", style = NumberStyle.copy(fontSize = 12.sp), color = Arcade.colors.muted, modifier = Modifier.padding(bottom = 1.dp))
        }
    }
}

/** Integer that counts up from 0 (or its previous value) to [value]. Settles on exactly [value]. */
@Composable
fun CountUpText(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val reducedMotion = Arcade.reducedMotion
    val anim = remember { Animatable(if (reducedMotion) value.toFloat() else 0f) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(Motion.COUNT_UP_MS, easing = FastOutSlowInEasing)) }
    Text(anim.value.roundToInt().toString(), modifier, style = style, color = color)
}

/** Chunky XP bar: springs from empty to [progress] (0..1) with a slight overshoot. */
@Composable
fun XpBar(progress: Float, modifier: Modifier = Modifier) {
    val reducedMotion = Arcade.reducedMotion
    val anim = remember { Animatable(if (reducedMotion) progress else 0f) }
    LaunchedEffect(progress) {
        delay(150)
        anim.animateTo(progress, Motion.pop())
    }
    val line = Arcade.colors.line
    val track = Arcade.colors.track
    Canvas(modifier.fillMaxWidth().height(26.dp)) {
        val stroke = 3.dp.toPx()
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        val fillW = (size.width * anim.value.coerceIn(0f, 1.04f)).coerceIn(0f, size.width)
        if (fillW > 0f) {
            val w = fillW.coerceAtLeast(size.height)
            drawRoundRect(Sun, size = Size(w, size.height), cornerRadius = r)
            drawRoundRect(SunDeep, Offset(0f, size.height - 6.dp.toPx()), Size(w, 6.dp.toPx() - stroke / 2), r)
            drawRoundRect(Color.White.copy(alpha = 0.65f), Offset(10.dp.toPx(), 5.dp.toPx()), Size((w - 20.dp.toPx()).coerceAtLeast(0f), 5.dp.toPx()), CornerRadius(3.dp.toPx()))
            drawRoundRect(line, size = Size(w, size.height), cornerRadius = r, style = Stroke(stroke))
        }
        for (i in 1..4) {
            val x = size.width * i / 5
            drawLine(line.copy(alpha = 0.25f), Offset(x, stroke), Offset(x, size.height - stroke), 2.dp.toPx())
        }
        drawRoundRect(line, cornerRadius = r, style = Stroke(stroke), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
    }
}

private fun hexPath(size: Size, inset: Float = 0f) = Path().apply {
    val w = size.width - inset * 2
    val h = size.height - inset * 2
    moveTo(inset + w / 2, inset)
    lineTo(inset + w, inset + h * 0.25f)
    lineTo(inset + w, inset + h * 0.75f)
    lineTo(inset + w / 2, inset + h)
    lineTo(inset, inset + h * 0.75f)
    lineTo(inset, inset + h * 0.25f)
    close()
}

/** Level badge: Sun hexagon with an Ink rim. [caption] adds a tiny "LV" above the number. */
@Composable
fun LevelHex(level: Int, modifier: Modifier = Modifier, width: Dp = 54.dp, caption: Boolean = false) {
    Box(
        modifier
            .size(width, width * 1.11f)
            .drawBehind {
                drawPath(hexPath(size), Ink)
                drawPath(hexPath(size, 3.dp.toPx()), Sun)
            }
            .semantics { contentDescription = "Level $level" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (caption) Text("LV", style = NumberStyle.copy(fontSize = 11.sp, lineHeight = 11.sp), color = Ink)
            Text("$level", style = NumberStyle.copy(fontSize = (width.value * 0.4f).sp, lineHeight = (width.value * 0.42f).sp), color = Ink)
        }
    }
}

/** Mint ring filling to [progress], with a white hole, like a chunky donut gauge. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 50.dp, color: Color = Mint) {
    val reducedMotion = Arcade.reducedMotion
    val anim = remember { Animatable(if (reducedMotion) progress else 0f) }
    LaunchedEffect(progress) { anim.animateTo(progress, tween(900, 200, FastOutSlowInEasing)) }
    val line = Arcade.colors.line
    val track = Arcade.colors.track
    Canvas(modifier.size(size)) {
        val stroke = 3.dp.toPx()
        drawCircle(track)
        drawArc(color, -90f, 360f * anim.value, useCenter = true)
        drawCircle(line, radius = this.size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        drawCircle(Paper, radius = this.size.minDimension * 0.28f)
        drawCircle(line, radius = this.size.minDimension * 0.28f, style = Stroke(2.dp.toPx()))
    }
}

enum class BadgeState { LOCKED, UNLOCKED, NEW }

/** One badge medal tile. Unlocked tiles get a single shine sweep; NEW ones also wobble and wear a tag. */
@Composable
fun BadgeTile(
    name: String,
    caption: String,
    icon: ImageVector,
    medalColor: Color,
    state: BadgeState,
    index: Int,
    modifier: Modifier = Modifier,
    iconTint: Color = Ink,
    progress: Float? = null,
) {
    val reduced = Arcade.reducedMotion
    val locked = state == BadgeState.LOCKED
    val colors = Arcade.colors
    val shine = remember { Animatable(if (locked || reduced) 2f else -0.2f) }
    val wobble = remember { Animatable(if (state == BadgeState.NEW && !reduced) 0f else 1f) }
    LaunchedEffect(state) {
        if (locked || reduced) return@LaunchedEffect
        delay(500L + index * 140L)
        if (state == BadgeState.NEW) wobble.animateTo(1f, Motion.pop())
        repeat(if (state == BadgeState.NEW) 2 else 1) {
            shine.snapTo(-0.2f)
            shine.animateTo(1.2f, tween(700, easing = FastOutSlowInEasing))
            delay(900)
        }
        shine.snapTo(2f)
    }
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier
            .graphicsLayer {
                // 0 → 1 wobble: squash, overshoot, settle (the spec's "unlock" keyframes).
                val w = wobble.value
                val s = if (w < 1f) 0.86f + 0.24f * w else 1f
                scaleX = s
                scaleY = s
                rotationZ = (1f - w) * -4f
            }
            .testTag(if (locked) "badge-locked $name" else "badge-unlocked $name"),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .ledge(shape, if (locked) colors.locked else colors.surface, if (locked) colors.lockedLine else colors.line, if (locked) colors.lockedLine else colors.ledge, 4.dp)
                .drawWithContent {
                    drawContent()
                    val p = shine.value
                    if (p in -0.2f..1.2f) {
                        val x = size.width * p
                        val band = 36.dp.toPx()
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                                start = Offset(x - band, 0f), end = Offset(x + band, size.height * 0.35f),
                            ),
                        )
                    }
                }
                .padding(top = 12.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(56.dp)
                    .border(3.dp, if (locked) colors.lockedLine else colors.line, CircleShape)
                    .background(if (locked) colors.track else medalColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (locked) colors.muted else iconTint, modifier = Modifier.size(28.dp))
            }
            Text(name, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp, lineHeight = 16.sp), color = colors.text, textAlign = TextAlign.Center, maxLines = 1)
            Text(caption, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = colors.muted, textAlign = TextAlign.Center, maxLines = 1)
            if (progress != null) {
                Box(Modifier.padding(top = 6.dp, start = 10.dp, end = 10.dp).fillMaxWidth().height(8.dp).background(colors.track, RoundedCornerShape(4.dp))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).background(Grape, RoundedCornerShape(4.dp)))
                }
            }
        }
        if (state == BadgeState.NEW) {
            Text(
                "NEW",
                Modifier.align(Alignment.TopEnd).padding(6.dp)
                    .graphicsLayer { scaleX = wobble.value; scaleY = wobble.value }
                    .border(2.dp, Ink, RoundedCornerShape(6.dp)).background(Coral, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp),
                style = NumberStyle.copy(fontSize = 10.sp, lineHeight = 16.sp),
                color = Ink,
            )
        }
    }
}

/** Ticking DAYS / HRS / MIN / SEC tiles in mono digits. Stops at zero. */
@Composable
fun Countdown(deadline: Instant, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(deadline) {
        while (now < deadline) {
            delay(1000 - System.currentTimeMillis() % 1000)
            now = Instant.now()
        }
    }
    val left = Duration.between(now, deadline).seconds.coerceAtLeast(0)
    val d = left / 86_400
    val h = (left % 86_400 / 3600).toInt()
    val m = (left % 3600 / 60).toInt()
    val s = (left % 60).toInt()
    Row(
        modifier.clearAndSetSemantics { contentDescription = "$d days $h hours $m minutes left" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountdownTile(d.toInt(), "DAYS")
        CountdownTile(h, "HRS")
        CountdownTile(m, "MIN")
        CountdownTile(s, "SEC")
    }
}

@Composable
private fun CountdownTile(value: Int, label: String) {
    val reduced = Arcade.reducedMotion
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(58.dp)
                .ledge(RoundedCornerShape(12.dp), Ink, Ink, Color.Black, 4.dp)
                .height(46.dp)
                .drawWithContent {
                    drawContent()
                    drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx())
                },
            contentAlignment = Alignment.Center,
        ) {
            val style = NumberStyle.copy(fontSize = 24.sp)
            if (reduced) {
                Text(value.toString().padStart(2, '0'), style = style, color = Sun)
            } else {
                AnimatedContent(
                    value,
                    transitionSpec = { (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut()) },
                    label = "tick",
                ) { Text(it.toString().padStart(2, '0'), style = style, color = Sun) }
            }
        }
        Text(label, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Arcade.colors.muted)
    }
}
