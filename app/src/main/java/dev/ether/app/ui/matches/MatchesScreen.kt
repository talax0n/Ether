package dev.ether.app.ui.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.R
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.ui.Avatar
import dev.ether.app.ui.LoadingBox
import dev.ether.app.ui.MessageBox
import dev.ether.app.ui.contactLabel
import dev.ether.app.ui.openContact
import dev.ether.app.ui.shortDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matches") },
                navigationIcon = {
                    onBack?.let { IconButton(onClick = it) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") } }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            MatchesUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is MatchesUiState.Failed -> MessageBox(s.message, Modifier.padding(padding), "Retry", vm::load)
            is MatchesUiState.Loaded ->
                if (s.isEmpty) {
                    MessageBox("No matches yet. Swipe right in a competition and they'll show up here.", Modifier.padding(padding))
                } else {
                    LazyColumn(
                        Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        s.sections.forEach { section ->
                            if (competitionId == null) {
                                item(key = section.competitionName) {
                                    Text(section.competitionName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            items(section.matches, key = { it.id.value }) { MatchRow(it) }
                        }
                    }
                }
        }
    }
}

@Composable
private fun MatchRow(match: Match) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(match.name, match.avatarUrl, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(match.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${if (match.otherKind == ListingKind.TEAM) "Team" else "Solo"} · matched ${match.createdAt.shortDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                match.contactUrl?.let { Text(it.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
            match.contactUrl?.let { url ->
                FilledTonalButton(onClick = { openContact(context, url) }) { Text(contactLabel(url).removePrefix("Message on ")) }
            }
        }
    }
}
