package dev.ether.app.ui.competitions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.R
import dev.ether.app.domain.Competition
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.ui.LoadingBox
import dev.ether.app.ui.MessageBox
import dev.ether.app.ui.openContact
import dev.ether.app.ui.shortDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionDetailScreen(
    competitionId: String,
    onOpenDeck: (ListingId) -> Unit,
    onOpenMatches: () -> Unit,
    onBack: () -> Unit,
    vm: CompetitionDetailViewModel = koinViewModel { parametersOf(competitionId) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.competition?.name.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } },
            )
        },
    ) { padding ->
        val competition = state.competition
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            competition == null -> MessageBox(state.error ?: "Not found", Modifier.padding(padding), "Retry", vm::load)
            else -> Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompetitionInfo(competition)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ListingActions(state.listing, state.busy, onOpenDeck, onOpenMatches, vm::joinSolo, vm::openTeamForm)
            }
        }
    }
    state.teamForm?.let { form ->
        TeamFormDialog(form, state.busy, vm::editTeamForm, vm::createTeam, vm::closeTeamForm)
    }
}

@Composable
private fun CompetitionInfo(c: Competition) {
    val context = LocalContext.current
    Text(c.description, style = MaterialTheme.typography.bodyLarge)
    Text("Registration closes ${c.registrationDeadline.shortDate()}", style = MaterialTheme.typography.labelLarge)
    Text("Event ends ${c.eventEnd.shortDate()} · Teams of ${c.minTeamSize}–${c.maxTeamSize}", style = MaterialTheme.typography.labelLarge)
    c.url?.let { url -> TextButton(onClick = { openContact(context, url) }) { Text("Official page") } }
}

@Composable
private fun ListingActions(
    listing: Listing?,
    busy: Boolean,
    onOpenDeck: (ListingId) -> Unit,
    onOpenMatches: () -> Unit,
    onJoinSolo: () -> Unit,
    onCreateTeam: () -> Unit,
) {
    val full = Modifier.fillMaxWidth().height(52.dp)
    if (listing == null) {
        Text("How are you entering?", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onJoinSolo, enabled = !busy, modifier = full) { Text("Join solo, find a team") }
        OutlinedButton(onClick = onCreateTeam, enabled = !busy, modifier = full) { Text("Create a team, find members") }
        return
    }
    Text(
        when (listing) {
            is Listing.Solo -> "You're in solo. Swipe on teams and other solos."
            is Listing.Team -> "Your team is listed. Swipe on solos to fill open roles."
        },
        style = MaterialTheme.typography.titleMedium,
    )
    if (!listing.active) {
        Text("This listing is closed, so it no longer appears in decks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Button(onClick = { onOpenDeck(listing.id) }, enabled = listing.active, modifier = full) { Text("Start swiping") }
    OutlinedButton(onClick = onOpenMatches, modifier = full) { Text("Matches") }
}

@Composable
private fun TeamFormDialog(form: TeamForm, busy: Boolean, onChange: (TeamForm) -> Unit, onSubmit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a team") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(form.name, { onChange(form.copy(name = it)) }, label = { Text("Team name") }, singleLine = true)
                OutlinedTextField(form.pitch, { onChange(form.copy(pitch = it)) }, label = { Text("Pitch") }, minLines = 2)
                OutlinedTextField(
                    form.openRoles, { onChange(form.copy(openRoles = it)) },
                    label = { Text("Open roles, comma separated") }, singleLine = true,
                )
            }
        },
        confirmButton = { Button(onClick = onSubmit, enabled = form.isValid && !busy) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
