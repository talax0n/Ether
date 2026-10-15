package dev.ether.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.domain.levelFor
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.BadgeState
import dev.ether.app.ui.components.BadgeTile
import dev.ether.app.ui.components.CountUpText
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.components.LevelHex
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.ProgressRing
import dev.ether.app.ui.components.ScreenHeader
import dev.ether.app.ui.components.SectionHeader
import dev.ether.app.ui.components.TagRow
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.XpBar
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.dayMonth
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.CoralSoft
import dev.ether.app.ui.theme.Grape
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.Sun
import org.koin.compose.viewmodel.koinViewModel
import java.time.Duration
import java.time.Instant

@Composable
fun ProfileScreen(onEdit: () -> Unit, vm: ProfileViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Reload on every resume so edits made on the edit screen show up when navigating back.
    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose {}
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Profile") { LedgeIconButton(ArcadeIcons.Edit, "Edit profile", onEdit) }
        when (val s = state) {
            ProfileUiState.Loading -> LoadingBox()
            is ProfileUiState.Failed -> MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            is ProfileUiState.Loaded -> ProfileContent(s.summary, vm::signOut)
        }
    }
}

@Composable
private fun ProfileContent(summary: ProfileSummary, onSignOut: () -> Unit) {
    val p = summary.profile
    val text = Arcade.colors.text
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.staggerIn(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(p.name, p.avatarUrl, 92.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, style = MaterialTheme.typography.headlineMedium, color = text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                listOfNotNull(p.major, p.year?.let { "Year $it" }).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted)
                }
                TagRow(p.skills, SkySoft, Modifier.padding(top = 6.dp), maxLines = 1)
            }
        }
        XpCard(p.xp, Modifier.staggerIn(1))
        Row(Modifier.staggerIn(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val endorsements = summary.reliability.endorsements
            StatCard("$endorsements", if (endorsements == 1) "Endorsement" else "Endorsements", Modifier.weight(1f)) {
                ProgressRing(endorsements / RELIABLE_AT.toFloat())
            }
            StatCard("${summary.badges.size}", if (summary.badges.size == 1) "Badge" else "Badges", Modifier.weight(1f)) {
                Box(Modifier.size(50.dp).ledge(CircleShape, Grape, Arcade.colors.line, Arcade.colors.ledge, 0.dp), contentAlignment = Alignment.Center) {
                    Icon(ArcadeIcons.Star, contentDescription = null, tint = Paper, modifier = Modifier.size(26.dp))
                }
            }
        }
        BadgeGrid(summary, Modifier.staggerIn(3))
        if (p.bio.isNotBlank()) {
            Column(Modifier.staggerIn(4), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("About")
                Text(p.bio, style = MaterialTheme.typography.bodyLarge, color = text)
            }
        }
        Column(Modifier.staggerIn(5), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Roles")
            TagRow(p.roles, CoralSoft)
        }
        LedgeButton(
            "Sign out", onSignOut, Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
            color = Color.Transparent, contentColor = text, icon = ArcadeIcons.Logout,
        )
    }
}

@Composable
private fun XpCard(xp: Int, modifier: Modifier) {
    val level = levelFor(xp)
    val muted = Arcade.colors.muted
    LedgeCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, ledge = 5.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LevelHex(level.number)
                Column(Modifier.weight(1f)) {
                    Text("Level ${level.number} · ${level.title}", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = Arcade.colors.text)
                    Text(
                        level.nextXp?.let { "${it - xp} XP to Level ${level.number + 1}" } ?: "Top level reached",
                        style = MaterialTheme.typography.bodySmall, color = muted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    CountUpText(xp, NumberStyle.copy(fontSize = 22.sp), Arcade.colors.text)
                    Text(level.nextXp?.let { "/ $it XP" } ?: "XP", style = NumberStyle.copy(fontSize = 11.sp), color = muted)
                }
            }
            XpBar(level.progress(xp))
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier, sticker: @Composable () -> Unit) {
    LedgeCard(modifier, shape = MaterialTheme.shapes.medium, ledge = 4.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sticker()
            Column {
                Text(value, style = NumberStyle.copy(fontSize = 22.sp), color = Arcade.colors.text)
                Text(label, style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted, maxLines = 1)
            }
        }
    }
}

private const val RELIABLE_AT = 5
private val NEW_FOR: Duration = Duration.ofDays(3)

private class BadgeSpec(val code: String, val name: String, val icon: ImageVector, val color: Color, val tint: Color, val lockedHint: String)

private val catalog = listOf(
    BadgeSpec("first_team", "First Team", ArcadeIcons.Users, Mint, Ink, "Join a team"),
    BadgeSpec("finalist", "Finalist", ArcadeIcons.Flag, Grape, Paper, "Reach a final"),
    BadgeSpec("reliable", "Reliable", ArcadeIcons.Shield, Sky, Ink, ""),
    BadgeSpec("three_hackathons", "3 Hackathons", ArcadeIcons.Trophy, Sun, Ink, "Finish 3 events"),
    BadgeSpec("winner", "Winner", ArcadeIcons.Star, Coral, Ink, "Win one"),
)

@Composable
private fun BadgeGrid(summary: ProfileSummary, modifier: Modifier) {
    val earned = summary.badges.associateBy { it.code }
    val now = Instant.now()
    val endorsements = summary.reliability.endorsements
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Badges", trailing = "${catalog.count { it.code in earned }} of ${catalog.size}")
        // Earned first so new rewards sit at the top; a "???" tile hints there's more to chase.
        val tiles = catalog.sortedBy { it.code !in earned }.map { spec -> spec to earned[spec.code] } + (null to null)
        tiles.chunked(3).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                chunk.forEachIndexed { col, (spec, badge) ->
                    val index = row * 3 + col
                    val m = Modifier.weight(1f)
                    when {
                        spec == null -> BadgeTile("???", "Keep going", ArcadeIcons.Lock, Arcade.colors.track, BadgeState.LOCKED, index, m)
                        badge != null -> BadgeTile(
                            badge.name, badge.awardedAt.dayMonth(), spec.icon, spec.color,
                            if (Duration.between(badge.awardedAt, now) < NEW_FOR) BadgeState.NEW else BadgeState.UNLOCKED,
                            index, m, iconTint = spec.tint,
                        )
                        spec.code == "reliable" -> BadgeTile(
                            spec.name, "$endorsements / $RELIABLE_AT", spec.icon, spec.color, BadgeState.LOCKED, index, m,
                            progress = endorsements / RELIABLE_AT.toFloat(),
                        )
                        else -> BadgeTile(spec.name, spec.lockedHint, spec.icon, spec.color, BadgeState.LOCKED, index, m)
                    }
                }
                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
