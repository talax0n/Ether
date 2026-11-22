package dev.ether.app.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ether.app.domain.levelFor
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.CapsLabel
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LevelHex
import dev.ether.app.ui.components.TagRow
import dev.ether.app.ui.components.XpBar
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.EtherTheme
import dev.ether.app.ui.theme.Grape
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.MintDeep
import dev.ether.app.ui.theme.MintSoft
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.theme.Peach
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.Sun
import dev.ether.app.ui.theme.SunSoft
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sin

enum class OnboardingPage(val title: String, val body: String) {
    SWIPE("Swipe to find\nyour hackathon team", "Right on the builders you want. Left to pass. Only students from your campus are in the deck."),
    MATCH("Match, then\nmessage", "Two right swipes make a team. WhatsApp and Telegram links unlock only for the people you matched."),
    LEVEL("Level up with\nevery event", "Earn XP for joining, shipping and winning. Teammates endorse you once the event ends."),
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = OnboardingPage.entries
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    val colors = Arcade.colors

    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (!last) {
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onDone).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Skip", style = MaterialTheme.typography.titleSmall, color = colors.muted)
                    Icon(ArcadeIcons.Back, null, tint = colors.muted, modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 180f })
                }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val page = pages[index]
            val offset = pager.offsetOf(index)
            Column(Modifier.fillMaxSize()) {
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                        // The hero drifts slower than the page and fades, so pages feel layered rather than slid.
                        translationX = offset * size.width * 0.45f
                        alpha = 1f - offset.absoluteValue.coerceAtMost(1f) * 0.8f
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    val scale = (maxHeight / 380.dp).coerceIn(0.7f, 1.1f)
                    Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                        when (page) {
                            OnboardingPage.SWIPE -> SwipeHero()
                            OnboardingPage.MATCH -> MatchHero()
                            OnboardingPage.LEVEL -> LevelHero()
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 24.dp).graphicsLayer { alpha = 1f - offset.absoluteValue.coerceAtMost(1f) }) {
                    Text(page.title, style = MaterialTheme.typography.displaySmall, color = colors.text)
                    Spacer(Modifier.height(10.dp))
                    Text(page.body, style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                }
            }
        }
        PageDots(pages.size, pager.currentPage, Modifier.padding(start = 24.dp, top = 22.dp))
        LedgeButton(
            if (last) "Get started" else "Continue",
            { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            icon = if (last) ArcadeIcons.Bolt else null,
        )
    }
}

private fun PagerState.offsetOf(page: Int) = (currentPage - page) + currentPageOffsetFraction

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) 22.dp else 8.dp, label = "dot-width")
            val fill by animateColorAsState(if (active) Coral else Arcade.colors.track, label = "dot-fill")
            Box(Modifier.width(width).height(8.dp).background(fill, CircleShape))
        }
    }
}

/** A 0..2π phase that loops forever, or stays at 0 under reduced motion. */
@Composable
private fun phase(periodMs: Int): State<Float> =
    if (Arcade.reducedMotion) remember { mutableFloatStateOf(0f) }
    else rememberInfiniteTransition(label = "phase").animateFloat(
        0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "t",
    )

@Composable
private fun Sticker(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(
        modifier.size(size + 4.dp).ledge(CircleShape, color, Arcade.colors.line, Arcade.colors.ledge, 4.dp),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Ink, modifier = Modifier.size(size * 0.46f)) }
}

@Composable
private fun SwipeHero() {
    val t by phase(4200)
    val card = RoundedCornerShape(26.dp)
    Box(Modifier.size(width = 300.dp, height = 360.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.offset(x = (-34).dp, y = 22.dp).graphicsLayer { rotationZ = -11f }
                .size(196.dp, 250.dp).ledge(card, GrapeSoft, Arcade.colors.line, Arcade.colors.ledge, 5.dp),
        )
        Box(
            Modifier.offset(x = 36.dp, y = 14.dp).graphicsLayer { rotationZ = 9f }
                .size(196.dp, 250.dp).ledge(card, SkySoft, Arcade.colors.line, Arcade.colors.ledge, 5.dp),
        )
        Column(
            Modifier
                .graphicsLayer {
                    rotationZ = sin(t) * 5f
                    translationX = sin(t) * 14.dp.toPx()
                }
                .size(214.dp, 272.dp)
                .ledge(card, Paper, Arcade.colors.line, Arcade.colors.ledge, 6.dp)
                .padding(18.dp),
        ) {
            Avatar("Nadia Rahma", null, 64.dp, color = MintSoft)
            Spacer(Modifier.height(14.dp))
            Text("Nadia Rahma", style = MaterialTheme.typography.titleLarge, color = Ink)
            Text("Android dev · Year 3", style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            TagRow(listOf("Kotlin", "UI/UX", "Firebase"), SunSoft, maxLines = 2)
        }
        Text(
            "LIKE",
            Modifier
                .offset(x = 62.dp, y = 30.dp)
                .graphicsLayer { rotationZ = 14f; alpha = (0.35f + sin(t) * 0.65f).coerceIn(0f, 1f) }
                .border(3.dp, MintDeep, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = 2.sp),
            color = MintDeep,
        )
        Row(Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Sticker(ArcadeIcons.Close, Peach)
            Sticker(ArcadeIcons.HeartFilled, Mint)
        }
    }
}

@Composable
private fun MatchHero() {
    val t by phase(1400)
    val beat = 1f + (sin(t).coerceAtLeast(0f)) * 0.12f
    Box(Modifier.size(width = 300.dp, height = 330.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.offset(y = (-36).dp), contentAlignment = Alignment.Center) {
            Avatar("Ana Putri", null, 112.dp, Modifier.offset(x = (-58).dp).graphicsLayer { rotationZ = -7f }, color = SkySoft, ledge = 6.dp)
            Avatar("Budi Santoso", null, 112.dp, Modifier.offset(x = 58.dp).graphicsLayer { rotationZ = 7f }, color = SunSoft, ledge = 6.dp)
            Sticker(ArcadeIcons.HeartFilled, Coral, Modifier.offset(y = 44.dp).graphicsLayer { scaleX = beat; scaleY = beat }, size = 60.dp)
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Chip("It's a match!", color = Sun, icon = ArcadeIcons.Star)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("WhatsApp", color = MintSoft, icon = ArcadeIcons.Chat)
                Chip("Telegram", color = SkySoft, icon = ArcadeIcons.Chat)
            }
        }
    }
}

@Composable
private fun LevelHero() {
    val t by phase(3600)
    val xp = 520
    val level = levelFor(xp)
    Box(Modifier.size(width = 300.dp, height = 330.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(252.dp).ledge(RoundedCornerShape(26.dp), Paper, Arcade.colors.line, Arcade.colors.ledge, 6.dp).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelHex(level.number)
                Spacer(Modifier.width(12.dp))
                Column {
                    CapsLabel("Level ${level.number}", color = Ink.copy(alpha = 0.6f))
                    Text(level.title, style = MaterialTheme.typography.headlineSmall, color = Ink)
                }
            }
            Spacer(Modifier.height(16.dp))
            XpBar(level.progress(xp))
            Spacer(Modifier.height(8.dp))
            Text("$xp / ${level.nextXp} XP", style = NumberStyle.copy(fontSize = 13.sp), color = Ink.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Sticker(ArcadeIcons.Trophy, Sun, size = 44.dp)
                Sticker(ArcadeIcons.ThumbUp, Sky, size = 44.dp)
                Sticker(ArcadeIcons.Shield, Grape, size = 44.dp)
            }
        }
        Chip("+50 XP", Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 22.dp).graphicsLayer {
            translationY = sin(t) * 6.dp.toPx(); rotationZ = 8f
        }, color = Sun, numeric = true)
        Chip("+25 XP", Modifier.align(Alignment.BottomStart).offset(x = 4.dp, y = (-20).dp).graphicsLayer {
            translationY = sin(t + 2f) * 6.dp.toPx(); rotationZ = -6f
        }, color = Mint, numeric = true)
    }
}

@Preview
@Composable
private fun OnboardingPreview() {
    EtherTheme { OnboardingScreen {} }
}
