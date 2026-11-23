package dev.ether.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.ether.app.domain.Profile
import dev.ether.app.ui.components.CapsLabel
import dev.ether.app.ui.appsSummary
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
import dev.ether.app.ui.components.EthaLive
import dev.ether.app.ui.components.EthaMove
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
        when (val s = state) {
            ProfileUiState.Loading -> LoadingBox()
            is ProfileUiState.Failed -> MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            is ProfileUiState.Loaded -> {
                ProfileContent(s.summary, s.leveledUp, onEdit, vm::signOut) { modifier ->
                    TrackRecordSection(
                        s.trackRecord, vm::addHackathon, vm::editHackathon, vm::addProject, vm::editProject, modifier,
                    )
                }
                s.editor?.let { editor ->
                    TrackRecordEditorDialog(
                        editor, s.currentYear, s.canSave,
                        onHackathonChange = vm::editHackathonForm,
                        onProjectChange = vm::editProjectForm,
                        onSave = vm::save,
                        onDelete = vm::delete,
                        onDismiss = vm::closeEditor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    summary: ProfileSummary,
    leveledUp: Boolean,
    onEdit: () -> Unit,
    onSignOut: () -> Unit,
    trackRecord: @Composable (Modifier) -> Unit,
) {
    val p = summary.profile
    val colors = Arcade.colors
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.staggerIn(0), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.name, p.avatarUrl, 88.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.name, Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                listOfNotNull(p.major, p.year?.let { "Year $it" }).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyLarge, color = colors.muted)
                }
            }
            LedgeIconButton(ArcadeIcons.Edit, "Edit profile", onEdit, size = 42.dp)
        }
        XpCard(p.xp, leveledUp, Modifier.staggerIn(1))
        CardPreview(p, Modifier.staggerIn(2))
        trackRecord(Modifier.staggerIn(3))
        BadgeGrid(summary, Modifier.staggerIn(4))
        LedgeButton(
            "Sign out", onSignOut, Modifier.fillMaxWidth().padding(top = 4.dp),
            color = Color.Transparent, contentColor = colors.text, icon = ArcadeIcons.Logout,
        )
    }
}

/** Everything a match or a deck viewer learns about you, in one place, so you can check it before they do. */
@Composable
private fun CardPreview(p: Profile, modifier: Modifier) {
    val colors = Arcade.colors
    LedgeCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How others see you", style = MaterialTheme.typography.titleMedium, color = colors.text)
            if (p.bio.isNotBlank()) Text(p.bio, style = MaterialTheme.typography.bodyLarge, color = colors.text)
            if (p.skills.isNotEmpty()) {
                CapsLabel("Skills")
                TagRow(p.skills, SkySoft)
            }
            if (p.roles.isNotEmpty()) {
                CapsLabel("Roles")
                TagRow(p.roles, CoralSoft)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.track))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ArcadeIcons.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    p.contacts.appsSummary()?.let { "Matches and teammates reach you on $it." }
                        ?: "No contacts yet. Matches can't reach you until you add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun XpCard(xp: Int, leveledUp: Boolean, modifier: Modifier) {
    val level = levelFor(xp)
    val muted = Arcade.colors.muted
    LedgeCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, ledge = 5.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (leveledUp) {
                // Shown once per new level on this device; Etha powers up, then fidgets.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EthaLive(size = 72.dp, first = EthaMove.LEVEL_UP)
                    Column(Modifier.weight(1f)) {
                        Text("Level up!", style = MaterialTheme.typography.titleLarge, color = Arcade.colors.text)
                        Text("You reached ${level.title}.", style = MaterialTheme.typography.bodyMedium, color = muted)
                    }
                }
            }
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
