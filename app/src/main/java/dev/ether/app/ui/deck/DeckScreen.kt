package dev.ether.app.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.theme.Coral
import dev.ether.app.ui.theme.Mint
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ether.app.ui.shareText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeSnackbarHost
import dev.ether.app.ui.components.EnergyMeter
import dev.ether.app.ui.components.EthaLive
import dev.ether.app.ui.components.EthaMove
import dev.ether.app.ui.components.rememberEthaController
import dev.ether.app.domain.SwipeDirection
import androidx.compose.runtime.rememberCoroutineScope
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
    val etha = rememberEthaController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
                // Etha watches the deck: cheers on a like, shrugs on a pass.
                EthaLive(Modifier.padding(end = 8.dp), size = 56.dp, controller = etha)
                LedgeIconButton(ArcadeIcons.Heart, "Matches", onOpenMatches, size = 42.dp)
            }
            state.remaining?.let { EnergyMeter(it, Modifier.padding(horizontal = 20.dp).fillMaxWidth()) }
            val inner = Modifier.weight(1f)
            when (state.phase) {
                DeckPhase.LOADING -> LoadingBox(inner)
                DeckPhase.FAILED -> MessageBox(state.error ?: "Couldn't load the deck", inner, "Retry", Tone.ERROR, onAction = vm::load)
                DeckPhase.OUT_OF_CARDS -> OutOfCards(
                    competitionName = state.competitionName,
                    hasMatches = state.hasMatches,
                    onInvite = { shareText(context, inviteMessage(state.competitionName), "Invite friends") },
                    onRefresh = vm::load,
                    onOpenMatches = onOpenMatches,
                    modifier = inner,
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
                    onSwipe = { card, direction ->
                        vm.swipe(card, direction)
                        etha.play(scope, if (direction == SwipeDirection.RIGHT) EthaMove.CELEBRATE else EthaMove.SHRUG)
                    },
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


/**
 * The deck ran dry. Nobody is left to swipe on until more people join, so the way forward is bringing them in:
 * share an invite, check again, or go back to the matches already made.
 */
@Composable
private fun OutOfCards(
    competitionName: String?,
    hasMatches: Boolean,
    onInvite: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMatches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Asleep between pokes, like every other empty state.
        EthaLive(size = 150.dp, controller = rememberEthaController(EthaMove.SLEEP), pool = emptyList())
        Text(
            "You've seen everyone",
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall, color = Arcade.colors.text, textAlign = TextAlign.Center,
        )
        Text(
            "Nobody new has joined ${competitionName ?: "this competition"} yet. " +
                "Invite friends who are looking for a team, then check back.",
            style = MaterialTheme.typography.titleMedium, color = Arcade.colors.muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        val wide = Modifier.widthIn(max = 320.dp).fillMaxWidth()
        LedgeButton("Invite friends", onInvite, wide, color = Coral, icon = ArcadeIcons.Users)
        LedgeButton("Refresh", onRefresh, wide, color = Arcade.colors.surface, contentColor = Arcade.colors.text)
        if (hasMatches) LedgeButton("See your matches", onOpenMatches, wide, color = Mint, icon = ArcadeIcons.HeartFilled)
    }
}
