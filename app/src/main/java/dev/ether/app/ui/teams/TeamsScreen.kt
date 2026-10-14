package dev.ether.app.ui.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.R
import dev.ether.app.domain.ReportedResult
import dev.ether.app.domain.TeamResult
import dev.ether.app.domain.Teammate
import dev.ether.app.ui.Avatar
import dev.ether.app.ui.LoadingBox
import dev.ether.app.ui.MessageBox
import dev.ether.app.ui.contactLabel
import dev.ether.app.ui.openContact
import dev.ether.app.ui.shortDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(vm: TeamsViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose {}
    }
    val snackbar = remember { SnackbarHostState() }
    val message = (state as? TeamsUiState.Loaded)?.message
    LaunchedEffect(message) {
        if (message != null) {
            vm.messageShown()
            snackbar.showSnackbar(message)
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("My teams") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val inner = Modifier.padding(padding)
        when (val s = state) {
            TeamsUiState.Loading -> LoadingBox(inner)
            is TeamsUiState.Failed -> MessageBox(s.message, inner, "Retry", vm::load)
            is TeamsUiState.Loaded ->
                if (s.teams.isEmpty()) {
                    MessageBox("No teams yet. A mutual match in a competition puts you in one.", inner)
                } else {
                    LazyColumn(inner, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(s.teams, key = { it.team.id.value }) { team ->
                            TeamCard(
                                team = team,
                                busy = s.busy,
                                onEndorse = { vm.endorse(team, it) },
                                onReport = { vm.reportResult(team, it) },
                                onConfirm = { vm.confirmResult(team) },
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun TeamCard(
    team: TeamView,
    busy: Boolean,
    onEndorse: (Teammate) -> Unit,
    onReport: (TeamResult) -> Unit,
    onConfirm: () -> Unit,
) {
    val t = team.team
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t.competition.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(t.name, style = MaterialTheme.typography.titleLarge)
            Text(
                if (team.eventEnded) "Event ended ${t.competition.eventEnd.shortDate()}" else "Event ends ${t.competition.eventEnd.shortDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (t.teammates.isEmpty()) {
                Text("No teammates yet. Keep swiping to fill your open roles.", style = MaterialTheme.typography.bodyMedium)
            }
            t.teammates.forEach { TeammateRow(it, team.eventEnded, busy, onEndorse) }
            if (team.eventEnded) {
                HorizontalDivider()
                ResultSection(t.result, busy, onReport, onConfirm)
            }
        }
    }
}

@Composable
private fun TeammateRow(mate: Teammate, eventEnded: Boolean, busy: Boolean, onEndorse: (Teammate) -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(mate.name, null, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(mate.name, style = MaterialTheme.typography.titleSmall)
            mate.contactUrl?.let { url ->
                TextButton(onClick = { openContact(context, url) }, contentPadding = PaddingValues(0.dp)) {
                    Text(contactLabel(url), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (eventEnded) {
            if (mate.endorsedByMe) {
                Text("Endorsed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                FilledTonalButton(onClick = { onEndorse(mate) }, enabled = !busy, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(painterResource(R.drawable.ic_thumb_up), contentDescription = null, Modifier.size(18.dp))
                    Text("Endorse", Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

private val TeamResult.label get() = when (this) {
    TeamResult.FINALIST -> "Finalist"
    TeamResult.WINNER -> "Winner"
}

@Composable
private fun ResultSection(result: ReportedResult?, busy: Boolean, onReport: (TeamResult) -> Unit, onConfirm: () -> Unit) {
    Text("Result", style = MaterialTheme.typography.titleSmall)
    when {
        result == null -> {
            Text("Did your team place? Report it and a teammate confirms.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TeamResult.entries.forEach { r ->
                    OutlinedButton(onClick = { onReport(r) }, enabled = !busy) { Text(r.label) }
                }
            }
        }
        result.confirmed -> Text("${result.result.label}, confirmed by your team.", style = MaterialTheme.typography.bodyMedium)
        result.reportedByMe -> Text(
            "You reported ${result.result.label}. Waiting for a teammate to confirm.",
            style = MaterialTheme.typography.bodyMedium,
        )
        else -> {
            Text("${result.reporterName} reported ${result.result.label}.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onConfirm, enabled = !busy) { Text("Confirm ${result.result.label}") }
        }
    }
}
