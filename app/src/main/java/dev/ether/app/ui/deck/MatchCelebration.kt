package dev.ether.app.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.Confetti
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.Rays
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.contactLabel
import dev.ether.app.ui.openContact
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Grape
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.MintDeep
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.theme.Peach
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.Sun
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Reward(val icon: ImageVector, val color: Color, val title: String, val detail: String, val amount: String?)

/** What the match did, stated only as far as the client can be sure of it (the database awards the XP). */
private fun rewardsFor(match: Match) = listOfNotNull(
    if (match.otherKind == ListingKind.TEAM) {
        // A solo who matches a team always joins it, and joining a team is worth 100 XP (SPEC §4).
        Reward(ArcadeIcons.Users, Sun, "Joined ${match.name}", "You're on the team now", "+100")
    } else {
        Reward(ArcadeIcons.Users, Sun, "Team formed", "You and ${match.name} are teammates now", null)
    },
    Reward(
        ArcadeIcons.Chat, Mint, "Contact unlocked",
        match.contactUrl?.let { contactLabel(it).removePrefix("Message on ") + " link shared" } ?: "Their link lands in Matches shortly",
        null,
    ),
    Reward(ArcadeIcons.Trophy, Sky, "XP on its way", "Check your profile for the new total", null),
)

/**
 * Full-screen celebration: headline slam, avatars collide, heart pops, rewards stack in 120 ms apart,
 * confetti falls over sun rays. Under reduced motion every piece starts in its final state.
 */
@Composable
fun MatchCelebration(match: Match, onDismiss: () -> Unit) {
    val reduced = Arcade.reducedMotion
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val rewards = remember(match.id) { rewardsFor(match) }
    val start = if (reduced) 1f else 0f
    val headline = remember { Animatable(start) }
    val duo = remember { Animatable(start) }
    val heart = remember { Animatable(start) }
    val rows = remember { List(rewards.size) { Animatable(start) } }
    val cta = remember { Animatable(start) }
    LaunchedEffect(match.id) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (reduced) return@LaunchedEffect
        launch { headline.animateTo(1f, Motion.pop()) }
        launch { delay(220); duo.animateTo(1f, Motion.pop()) }
        launch { delay(480); heart.animateTo(1f, Motion.pop()); haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
        rows.forEachIndexed { i, row -> launch { delay(700 + i * Motion.REWARD_STAGGER_MS); row.animateTo(1f, Motion.pop()) } }
        launch { delay(1050); cta.animateTo(1f, Motion.settle()) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Grape)) {
            Rays(Modifier.fillMaxSize(), centerY = 0.36f)
            Confetti(Modifier.fillMaxSize())
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Headline(Modifier.graphicsLayer {
                    val p = headline.value
                    val s = 2.2f - 1.2f * p
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                })
                Text(
                    "You and ${match.name} both swiped right.",
                    Modifier.padding(top = 12.dp).graphicsLayer { alpha = headline.value.coerceIn(0f, 1f) },
                    style = MaterialTheme.typography.titleMedium,
                    color = Paper,
                    textAlign = TextAlign.Center,
                )
                Duo(match, { duo.value }, { heart.value })
                Column(
                    Modifier.fillMaxWidth().ledge(MaterialTheme.shapes.large, Paper, Ink, Ink, 6.dp).padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    rewards.forEachIndexed { i, reward ->
                        if (i > 0) HorizontalDivider(thickness = 2.dp, color = Color(0xFFECE6DA))
                        RewardRow(reward) { rows[i].value }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(
                    Modifier.fillMaxWidth().graphicsLayer {
                        alpha = cta.value.coerceIn(0f, 1f)
                        translationY = (1f - cta.value) * 40.dp.toPx()
                    },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    match.contactUrl?.let { url ->
                        LedgeButton(contactLabel(url), { openContact(context, url) }, Modifier.fillMaxWidth(), color = Mint, icon = ArcadeIcons.Chat)
                    } ?: Text(
                        "Their contact link will show in Matches shortly.",
                        Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = Paper, textAlign = TextAlign.Center,
                    )
                    LedgeButton(
                        "Keep swiping", onDismiss, Modifier.fillMaxWidth(),
                        color = Color.Transparent, contentColor = Paper, line = Paper, ledgeColor = Ink.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

/** "IT'S A MATCH!" in Sun with an Ink stroke and a solid drop, drawn as stacked text layers. */
@Composable
private fun Headline(modifier: Modifier) {
    val text = "IT'S A\nMATCH!"
    val base = MaterialTheme.typography.displayLarge.copy(textAlign = TextAlign.Center)
    val stroke = with(LocalDensity.current) { Stroke(width = 7.dp.toPx(), join = StrokeJoin.Round) }
    Box(modifier.clearAndSetSemantics { this.text = AnnotatedString("It's a match!"); heading() }) {
        Text(text, Modifier.graphicsLayer { translationY = 5.dp.toPx() }, style = base.copy(drawStyle = stroke), color = Ink)
        Text(text, style = base.copy(drawStyle = stroke), color = Ink)
        Text(text, style = base, color = Sun)
    }
}

@Composable
private fun Duo(match: Match, duo: () -> Float, heart: () -> Float) {
    Box(Modifier.fillMaxWidth().height(176.dp), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy((-22).dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                "You", null, 128.dp,
                Modifier.graphicsLayer {
                    val p = duo()
                    translationX = (1f - p) * -200.dp.toPx()
                    rotationZ = -30f + 26f * p
                },
                color = Peach,
                label = "YOU",
            )
            Avatar(
                match.name, match.avatarUrl, 128.dp,
                Modifier.graphicsLayer {
                    val p = duo()
                    translationX = (1f - p) * 200.dp.toPx()
                    rotationZ = 30f - 26f * p
                },
                color = SkySoft,
            )
        }
        Box(
            Modifier
                .graphicsLayer {
                    val s = heart()
                    scaleX = s
                    scaleY = s
                }
                .size(62.dp)
                .ledge(CircleShape, Coral, Ink, Ink, 4.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(ArcadeIcons.HeartFilled, contentDescription = null, tint = Paper, modifier = Modifier.size(28.dp)) }
    }
}

@Composable
private fun RewardRow(reward: Reward, progress: () -> Float) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).graphicsLayer { alpha = progress().coerceIn(0f, 1f) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    val p = progress()
                    scaleX = p
                    scaleY = p
                    rotationZ = (1f - p) * -90f
                }
                .size(44.dp)
                .ledge(RoundedCornerShape(14.dp), reward.color, Ink, Ink, 0.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(reward.icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(reward.title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(reward.detail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5B5670))
        }
        reward.amount?.let {
            Text(it, style = NumberStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = MintDeep)
        }
    }
}
