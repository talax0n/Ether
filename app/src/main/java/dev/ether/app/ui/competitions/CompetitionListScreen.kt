package dev.ether.app.ui.competitions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.Countdown
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.ScreenHeader
import dev.ether.app.ui.components.Seats
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.dayMonth
import dev.ether.app.ui.theme.Arcade
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CompetitionListScreen(
    onOpen: (CompetitionId) -> Unit,
    vm: CompetitionListViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Competitions")
        when (val s = state) {
            CompetitionListUiState.Loading -> LoadingBox()
            is CompetitionListUiState.Failed -> MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            is CompetitionListUiState.Loaded ->
                if (s.competitions.isEmpty()) {
                    MessageBox("No competitions yet. Check back soon.", tone = Tone.EMPTY, icon = ArcadeIcons.Trophy)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(s.competitions, key = { _, c -> c.id.value }) { i, c ->
                            CompetitionCard(c, Modifier.staggerIn(i)) { onOpen(c.id) }
                        }
                    }
                }
        }
    }
}

@Composable
private fun CompetitionCard(c: Competition, modifier: Modifier, onClick: () -> Unit) {
    val status = c.status()
    LedgeCard(modifier.fillMaxWidth(), onClick = onClick) {
        EventHeader(c, status, showTitle = true, descriptionLines = 2)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (status) {
                EventStatus.OPEN -> {
                    Text("Registration closes ${c.registrationDeadline.dayMonth()}", style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted)
                    Countdown(c.registrationDeadline)
                }
                EventStatus.CLOSED -> Text(
                    "Registration closed · Event ends ${c.eventEnd.dayMonth()}",
                    style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted,
                )
                EventStatus.ENDED -> Text("Finished ${c.eventEnd.dayMonth()}", style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Seats(emptyList(), c.maxTeamSize)
                Text(
                    "Teams of ${c.minTeamSize}–${c.maxTeamSize}",
                    Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Arcade.colors.text,
                )
            }
            if (status == EventStatus.OPEN) {
                LedgeButton("Find teammates", onClick, Modifier.fillMaxWidth(), icon = ArcadeIcons.HeartFilled)
            }
        }
    }
}
