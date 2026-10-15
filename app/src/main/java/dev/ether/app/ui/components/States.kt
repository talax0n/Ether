package dev.ether.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Grape
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.Sun
import dev.ether.app.ui.theme.SunSoft

/** Skeleton cards that pulse while a list loads. Shaped like the content they stand in for. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    val pulse = if (Arcade.reducedMotion) null else rememberInfiniteTransition(label = "skeleton")
        .animateFloat(0.55f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    val bone = Arcade.colors.track
    Column(
        modifier.fillMaxSize().padding(20.dp).semantics { contentDescription = "Loading" }.graphicsLayer { alpha = pulse?.value ?: 1f },
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        repeat(3) { i ->
            Column(
                Modifier.fillMaxWidth().ledge(MaterialTheme.shapes.large, Arcade.colors.surface, Arcade.colors.lockedLine, Arcade.colors.lockedLine, 5.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).background(bone, CircleShape))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(if (i == 1) 120.dp else 170.dp).height(16.dp).background(bone, CircleShape))
                        Box(Modifier.width(96.dp).height(12.dp).background(bone, CircleShape))
                    }
                }
                Box(Modifier.fillMaxWidth().height(12.dp).background(bone, CircleShape))
                Box(Modifier.fillMaxWidth(0.7f).height(12.dp).background(bone, CircleShape))
            }
        }
    }
}

/** App-start splash: the wordmark over bouncing dots. */
@Composable
fun SplashLoading(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(Arcade.colors.background).semantics { contentDescription = "Loading" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark()
        Spacer(Modifier.height(20.dp))
        BouncingDots(Coral)
    }
}

/** The "ether" logotype: Coral letters with an Ink ledge. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.displayLarge
    Box(modifier) {
        Text("ether", Modifier.offset(y = 5.dp), style = style, color = Ink)
        Text("ether", style = style, color = Coral)
    }
}

enum class Tone(val fill: Color, val icon: ImageVector) {
    EMPTY(SunSoft, ArcadeIcons.Star),
    ERROR(Coral, ArcadeIcons.WifiOff),
    DONE(Mint, ArcadeIcons.Check),
    WAIT(Sky, ArcadeIcons.Bolt),
    TEAM(Grape, ArcadeIcons.Users),
}

/**
 * Empty, error and end-of-road states: a sticker that pops in with a small wobble, the message, an optional action.
 */
@Composable
fun MessageBox(
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    tone: Tone = Tone.EMPTY,
    icon: ImageVector = tone.icon,
    title: String? = null,
    onAction: () -> Unit = {},
) {
    val reducedMotion = Arcade.reducedMotion
    val pop = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, Motion.pop()) }
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            // Confetti dots around the sticker, fixed positions so the scene reads as a little illustration.
            listOf(Triple(-58, -44, Sun), Triple(60, -30, Mint), Triple(-52, 48, Sky), Triple(56, 52, Coral)).forEach { (x, y, c) ->
                Box(
                    Modifier.offset(x.dp, y.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                        .size(14.dp).ledge(CircleShape, c, Arcade.colors.line, Arcade.colors.ledge, 2.dp, lineWidth = 2.dp),
                )
            }
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        rotationZ = -8f + (1f - pop.value) * -20f
                    }
                    .size(104.dp)
                    .ledge(CircleShape, tone.fill, Arcade.colors.line, Arcade.colors.ledge, 6.dp),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(48.dp)) }
        }
        title?.let { Text(it, style = MaterialTheme.typography.headlineSmall, color = Arcade.colors.text, textAlign = TextAlign.Center) }
        Text(message, style = MaterialTheme.typography.titleMedium, color = if (title == null) Arcade.colors.text else Arcade.colors.muted, textAlign = TextAlign.Center)
        if (action != null) LedgeButton(action, onAction, Modifier.padding(top = 4.dp).width(220.dp), color = if (tone == Tone.ERROR) Sun else Coral)
    }
}
