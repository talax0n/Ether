package dev.ether.app.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.R
import dev.ether.app.domain.Match
import dev.ether.app.ui.Avatar
import dev.ether.app.ui.LoadingBox
import dev.ether.app.ui.MessageBox
import dev.ether.app.ui.contactLabel
import dev.ether.app.ui.openContact
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Swipe")
                        state.remaining?.let {
                            Text(if (it == 1) "1 swipe left today" else "$it swipes left today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
                actions = { TextButton(onClick = onOpenMatches) { Text("Matches") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val inner = Modifier.padding(padding)
        when (state.phase) {
            DeckPhase.LOADING -> LoadingBox(inner)
            DeckPhase.FAILED -> MessageBox(state.error ?: "Couldn't load the deck", inner, "Retry", vm::load)
            DeckPhase.OUT_OF_CARDS -> MessageBox("No one left to swipe, check back later.", inner, "Refresh", vm::load)
            DeckPhase.LISTING_CLOSED -> MessageBox(
                "Your listing is closed, so you can't swipe here anymore. If you just matched, you're on a team now: check the Teams tab.",
                inner, "See matches", onOpenMatches,
            )
            DeckPhase.CAP_REACHED -> MessageBox("That's your 30 swipes for today. Your matches are waiting.", inner, "See matches", onOpenMatches)
            DeckPhase.SWIPING -> SwipeDeck(
                cards = state.cards,
                onSwipe = vm::swipe,
                modifier = inner.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
    state.match?.let { MatchDialog(it, onDismiss = vm::dismissMatch) }
}

@Composable
fun MatchDialog(match: Match, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Avatar(match.name, match.avatarUrl, 88.dp) },
        title = {
            Text(
                "It's a match!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You and ${match.name} both swiped right.", textAlign = TextAlign.Center)
                if (match.contactUrl == null) {
                    Text("Their contact link will show in Matches shortly.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            match.contactUrl?.let { url ->
                Button(onClick = { openContact(context, url) }) { Text(contactLabel(url)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep swiping") } },
    )
}
