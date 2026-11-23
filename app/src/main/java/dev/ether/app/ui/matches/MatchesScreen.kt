package dev.ether.app.ui.matches

import dev.ether.app.ui.components.ContactButtons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.theme.Coral
import java.time.Duration
import java.time.Instant
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.SectionHeader
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.shortDate
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.Peach
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.SunSoft
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MatchesScreen(
    competitionId: String?,
    onBack: (() -> Unit)?,
    onBrowse: () -> Unit,
    vm: MatchesViewModel = koinViewModel {
        if (competitionId == null) parametersOf() else parametersOf(competitionId)
    },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Reload on resume: a match made in the deck should show up when switching back to this tab.
    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose {}
    }
    val loaded = state as? MatchesUiState.Loaded
    Column(Modifier.fillMaxSize()) {
        if (onBack != null) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                LedgeIconButton(ArcadeIcons.Back, "Back", onBack, size = 42.dp)
            }
        }
        when (val s = state) {
            MatchesUiState.Loading -> { Header(null, onBack == null); LoadingBox() }
            is MatchesUiState.Failed -> {
                Header(null, onBack == null)
                MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            }
            is MatchesUiState.Loaded -> if (s.isEmpty) {
                Header(null, onBack == null)
                MessageBox(
                    "No matches yet. Swipe right in a competition and they'll show up here.",
                    action = "Browse competitions", tone = Tone.EMPTY, icon = ArcadeIcons.Heart, onAction = onBrowse,
                )
            } else {
                val now = remember(loaded) { Instant.now() }
                val rows = s.sections.flatMap { section ->
                    listOfNotNull(section.takeIf { competitionId == null }?.let { MatchListRow.Section(it.competitionName, it.matches.size) }) +
                        section.matches.sortedByDescending { it.createdAt }.map { MatchListRow.Item(it) }
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") { Header(s, onBack == null, horizontal = 0.dp) }
                    itemsIndexed(rows, key = { _, r -> r.key }) { i, r ->
                        when (r) {
                            is MatchListRow.Section -> SectionHeader(r.name, Modifier.staggerIn(i).padding(top = 8.dp), trailing = "${r.count}")
                            is MatchListRow.Item -> MatchRow(r.match, now, Modifier.staggerIn(i))
                        }
                    }
                }
            }
        }
    }
}

private sealed interface MatchListRow {
    val key: String
    data class Section(val name: String, val count: Int) : MatchListRow { override val key get() = "section-$name" }
    data class Item(val match: Match) : MatchListRow { override val key get() = match.id.value }
}

/** [topLevel] is the tab (big title); a competition's matches page sits under a back button instead. */
@Composable
private fun Header(loaded: MatchesUiState.Loaded?, topLevel: Boolean, horizontal: Dp = 20.dp) {
    Column(Modifier.fillMaxWidth().padding(start = horizontal, end = horizontal, top = if (topLevel) 20.dp else 4.dp, bottom = 4.dp)) {
        Text("Matches", Modifier.semantics { heading() }, style = MaterialTheme.typography.displaySmall, color = Arcade.colors.text)
        if (loaded != null) {
            val count = loaded.sections.sumOf { it.matches.size }
            Spacer(Modifier.height(4.dp))
            Text(
                if (count == 1) "1 match. Say hi while it's fresh." else "$count matches. Say hi while they're fresh.",
                style = MaterialTheme.typography.bodyLarge,
                color = Arcade.colors.muted,
            )
        }
    }
}

@Composable
private fun MatchRow(match: Match, now: Instant, modifier: Modifier) {
    val team = match.otherKind == ListingKind.TEAM
    val fresh = Duration.between(match.createdAt, now) < Duration.ofDays(1)
    LedgeCard(modifier.fillMaxWidth(), ledge = 4.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(match.name, match.avatarUrl, 52.dp, color = if (team) SkySoft else Peach, ledge = 3.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(match.name, style = MaterialTheme.typography.titleMedium, color = Arcade.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fresh) Chip("New", color = Coral)
                    Chip(if (team) "Team" else "Solo", color = if (team) GrapeSoft else SunSoft)
                }
                Text(matchedAgo(match.createdAt, now), style = MaterialTheme.typography.bodySmall, color = Arcade.colors.muted, maxLines = 1)
            }
        }
        ContactButtons(match.contacts, Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), color = Mint)
    }
}

/** "Matched just now", "Matched 3 hours ago", "Matched yesterday", then the date. */
internal fun matchedAgo(at: Instant, now: Instant): String {
    val minutes = Duration.between(at, now).toMinutes()
    return when {
        minutes < 1 -> "Matched just now"
        minutes < 60 -> "Matched $minutes min ago"
        minutes < 24 * 60 -> "Matched ${minutes / 60} hour${if (minutes / 60 == 1L) "" else "s"} ago"
        minutes < 48 * 60 -> "Matched yesterday"
        else -> "Matched ${at.shortDate()}"
    }
}
