package dev.ether.app.ui.competitions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.ui.LoadingBox
import dev.ether.app.ui.MessageBox
import dev.ether.app.ui.shortDate
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionListScreen(
    onOpen: (CompetitionId) -> Unit,
    vm: CompetitionListViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Competitions") })
        },
    ) { padding ->
        when (val s = state) {
            CompetitionListUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is CompetitionListUiState.Failed -> MessageBox(s.message, Modifier.padding(padding), "Retry", vm::load)
            is CompetitionListUiState.Loaded ->
                if (s.competitions.isEmpty()) {
                    MessageBox("No competitions yet. Check back soon.", Modifier.padding(padding))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(padding),
                    ) {
                        items(s.competitions, key = { it.id.value }) { CompetitionCard(it) { onOpen(it.id) } }
                    }
                }
        }
    }
}

@Composable
private fun CompetitionCard(c: Competition, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(c.name, style = MaterialTheme.typography.titleLarge)
            Text(c.description, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            val now = Instant.now()
            val status = when {
                now > c.eventEnd -> "Ended ${c.eventEnd.shortDate()}"
                now > c.registrationDeadline -> "Registration closed"
                else -> "Register by ${c.registrationDeadline.shortDate()}"
            }
            Text(
                "$status · Teams of ${c.minTeamSize}–${c.maxTeamSize}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
