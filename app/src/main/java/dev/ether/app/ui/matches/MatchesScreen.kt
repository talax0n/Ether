package dev.ether.app.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeTopBar
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.ScreenHeader
import dev.ether.app.ui.components.SectionHeader
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.contactLabel
import dev.ether.app.ui.openContact
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
    Column(Modifier.fillMaxSize()) {
        if (onBack == null) ScreenHeader("Matches") else ArcadeTopBar("Matches", onBack = onBack)
        when (val s = state) {
            MatchesUiState.Loading -> LoadingBox()
            is MatchesUiState.Failed -> MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            is MatchesUiState.Loaded ->
                if (s.isEmpty) {
                    MessageBox(
                        "No matches yet. Swipe right in a competition and they'll show up here.",
                        tone = Tone.EMPTY, icon = ArcadeIcons.Heart,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        var index = 0
                        s.sections.forEach { section ->
                            if (competitionId == null) {
                                val i = index++
                                item(key = section.competitionName) {
                                    SectionHeader(section.competitionName, Modifier.staggerIn(i), trailing = "${section.matches.size}")
                                }
                            }
                            section.matches.forEach { match ->
                                val i = index++
                                item(key = match.id.value) { MatchRow(match, Modifier.staggerIn(i)) }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun MatchRow(match: Match, modifier: Modifier) {
    val context = LocalContext.current
    val team = match.otherKind == ListingKind.TEAM
    LedgeCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, ledge = 5.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(match.name, match.avatarUrl, 52.dp, color = if (team) SkySoft else Peach, ledge = 3.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(match.name, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium, color = Arcade.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Chip(if (team) "Team" else "Solo", color = if (team) GrapeSoft else SunSoft)
                    }
                    Text("Matched ${match.createdAt.shortDate()}", style = MaterialTheme.typography.bodySmall, color = Arcade.colors.muted, maxLines = 1)
                    match.contactUrl?.let {
                        Text(it.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = Arcade.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            match.contactUrl?.let { url ->
                LedgeButton(
                    contactLabel(url).removePrefix("Message on "), { openContact(context, url) }, Modifier.fillMaxWidth(),
                    color = Mint, icon = ArcadeIcons.Chat, height = 48.dp, shape = RoundedCornerShape(14.dp),
                )
            }
        }
    }
}
