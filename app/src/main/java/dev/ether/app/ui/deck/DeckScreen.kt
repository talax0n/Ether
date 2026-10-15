package dev.ether.app.ui.deck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.ether.app.ui.components.ArcadeTopBar
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
            ArcadeTopBar("Swipe", subtitle = "Find your hackathon team", onBack = onBack) {
                LedgeIconButton(ArcadeIcons.Heart, "Matches", onOpenMatches)
            }
            state.remaining?.let { EnergyMeter(it, Modifier.padding(horizontal = 18.dp).fillMaxWidth()) }
            val inner = Modifier.weight(1f)
            when (state.phase) {
                DeckPhase.LOADING -> LoadingBox(inner)
                DeckPhase.FAILED -> MessageBox(state.error ?: "Couldn't load the deck", inner, "Retry", Tone.ERROR, onAction = vm::load)
                DeckPhase.OUT_OF_CARDS -> MessageBox(
                    "No one left to swipe, check back later.", inner, "Refresh", Tone.EMPTY, ArcadeIcons.Users, onAction = vm::load,
                )
                DeckPhase.LISTING_CLOSED -> MessageBox(
                    "Your listing is closed, so you can't swipe here anymore. If you just matched, you're on a team now: check the Teams tab.",
                    inner, "See matches", Tone.TEAM, onAction = onOpenMatches,
                )
                DeckPhase.CAP_REACHED -> MessageBox(
                    "That's your 30 swipes for today. Your matches are waiting.", inner, "See matches", Tone.WAIT,
                    title = "Out of energy", onAction = onOpenMatches,
                )
                DeckPhase.SWIPING -> Column(inner, horizontalAlignment = Alignment.CenterHorizontally) {
                    SwipeDeck(
                        cards = state.cards,
                        onSwipe = vm::swipe,
                        enabled = !state.swiping,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp),
                    )
                    state.remaining?.let {
                        Text(
                            if (it == 1) "1 swipe left today" else "$it swipes left today",
                            Modifier.padding(top = 4.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Arcade.colors.muted,
                        )
                    }
                }
            }
        }
        ArcadeSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.match?.let { MatchCelebration(it, onDismiss = vm::dismissMatch) }
}

