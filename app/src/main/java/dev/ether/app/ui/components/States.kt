package dev.ether.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.painterResource
import dev.ether.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
/**
 * Launch screen. The mark sits at the exact screen centre, where the system splash icon was, so the handoff
 * reads as one motion: the mark settles, then the wordmark rises under it. Dots appear only if loading
 * outlasts the intro, so a fast start never flashes a spinner.
 */
@Composable
fun Splash(modifier: Modifier = Modifier) {
    val reduced = Arcade.reducedMotion
    val pop = remember { Animatable(if (reduced) 1f else 0.86f) }
    val rise = remember { Animatable(if (reduced) 1f else 0f) }
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, Motion.pop()) }
        delay(160)
        rise.animateTo(1f, Motion.settle())
        delay(1200)
        slow = true
    }
    // Same size and spot as the system splash icon (a 160dp Coral circle), so the handoff doesn't jump.
    val markSize = SplashIconSize
    Box(modifier.fillMaxSize().background(Arcade.colors.background).semantics { contentDescription = "Loading" }) {
        Box(
            Modifier.align(Alignment.Center).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.size(markSize).background(Coral, CircleShape),
            contentAlignment = Alignment.Center,
        ) { EtherMark(size = markSize * 0.76f) }
        Column(
            Modifier.align(Alignment.Center).offset(y = markSize / 2 + 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wordmark(Modifier.graphicsLayer { alpha = rise.value.coerceIn(0f, 1f); translationY = (1f - rise.value) * 24.dp.toPx() })
            Box(Modifier.height(48.dp), contentAlignment = Alignment.BottomCenter) {
                if (slow) BouncingDots(Coral)
            }
        }
    }
}

/** The app mark: the launcher heart in a Coral ledge tile. */
@Composable
fun EtherMark(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Image(
        painterResource(R.drawable.ether_mark), contentDescription = null,
        modifier = modifier.height(size).aspectRatio(MarkAspect),
    )
}

/** Width over height of drawable/ether_mark.png (the launcher foreground cropped to the mark). */
private const val MarkAspect = 361f / 384f
private val SplashIconSize = 160.dp

/** The "ether" logotype: Coral letters with an Ink ledge. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.displayLarge
    Box(modifier) {
        Text("ether", Modifier.offset(y = 5.dp), style = style, color = Ink)
        Text("ether", style = style, color = Coral)
    }
}

/** [mascot] is the Etha move shown instead of the icon sticker; errors and confirmations keep the plain icon. */
enum class Tone(val fill: Color, val icon: ImageVector, val mascot: EthaMove?) {
    EMPTY(SunSoft, ArcadeIcons.Star, EthaMove.SLEEP),
    ERROR(Coral, ArcadeIcons.WifiOff, null),
    DONE(Mint, ArcadeIcons.Check, null),
    WAIT(Sky, ArcadeIcons.Bolt, EthaMove.SLEEP),
    TEAM(Grape, ArcadeIcons.Users, EthaMove.IDLE),
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
        val mascot = tone.mascot
        if (mascot != null) {
            Etha(mascot, Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }, size = 160.dp)
        } else Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
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
