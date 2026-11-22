package dev.ether.app.ui.deck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeSnackbarHost
import dev.ether.app.ui.components.EnergyMeter
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.theme.Arcade
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DeckScreen(
    competitionId: String,
    listingId: String,
    onOpenMatches: () -> Unit,
    onOpenTeams: () -> Unit,
    onBack: () -> Unit,
    vm: DeckViewModel = koinViewModel { parametersOf(competitionId, listingId) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error, state.phase) {
        if (state.phase != DeckPhase.FAILED) state.error?.let { snackbar.showSnackbar(it) }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                LedgeIconButton(ArcadeIcons.Back, "Back", onBack, size = 42.dp)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text("Swipe", style = MaterialTheme.typography.titleLarge, color = Arcade.colors.text)
                    state.remaining?.let {
                        Text(
                            if (it == 1) "1 swipe left today" else "$it swipes left today",
                            style = MaterialTheme.typography.bodySmall,
                            color = Arcade.colors.muted,
                        )
                    }
                }
                LedgeIconButton(ArcadeIcons.Heart, "Matches", onOpenMatches, size = 42.dp)
            }
            state.remaining?.let { EnergyMeter(it, Modifier.padding(horizontal = 20.dp).fillMaxWidth()) }
            val inner = Modifier.weight(1f)
            when (state.phase) {
                DeckPhase.LOADING -> LoadingBox(inner)
                DeckPhase.FAILED -> MessageBox(state.error ?: "Couldn't load the deck", inner, "Retry", Tone.ERROR, onAction = vm::load)
                DeckPhase.OUT_OF_CARDS -> MessageBox(
                    "No one left to swipe, check back later.", inner, "Refresh", Tone.EMPTY, ArcadeIcons.Users, onAction = vm::load,
                )
                DeckPhase.LISTING_CLOSED -> MessageBox(
                    "Your listing stopped taking swipes, usually because a match put you on a team.",
                    inner, "See your team", Tone.TEAM, title = "This deck is closed", onAction = onOpenTeams,
                )
                DeckPhase.CAP_REACHED -> MessageBox(
                    "That's your 30 swipes for today. Your matches are waiting.", inner, "See matches", Tone.WAIT,
                    title = "Out of energy", onAction = onOpenMatches,
                )
                DeckPhase.SWIPING -> SwipeDeck(
                    cards = state.cards,
                    onSwipe = vm::swipe,
                    enabled = !state.swiping,
                    modifier = inner.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                )
            }
        }
        ArcadeSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.match?.let {
        MatchCelebration(it, canKeepSwiping = state.phase != DeckPhase.LISTING_CLOSED, onDismiss = vm::dismissMatch, onOpenTeams = onOpenTeams)
    }
}

