package dev.ether.app.ui.competitions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.CapsLabel
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.Countdown
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.dayMonth
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.NumberStyle
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun CompetitionListScreen(
    onOpen: (CompetitionId) -> Unit,
    vm: CompetitionListViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        CompetitionListUiState.Loading -> Column(Modifier.fillMaxSize()) {
            Header(openCount = null)
            LoadingBox()
        }
        is CompetitionListUiState.Failed -> Column(Modifier.fillMaxSize()) {
            Header(openCount = null)
            MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
        }
        is CompetitionListUiState.Loaded -> if (s.competitions.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                Header(openCount = 0)
                MessageBox("No competitions yet. Check back soon.", tone = Tone.EMPTY, icon = ArcadeIcons.Trophy)
            }
        } else {
            CompetitionList(s.sections, onOpen)
        }
    }
}

private sealed interface ListRow {
    data class Section(val status: EventStatus, val count: Int) : ListRow
    data class Event(val competition: Competition, val status: EventStatus) : ListRow
}

@Composable
private fun CompetitionList(sections: List<Pair<EventStatus, List<Competition>>>, onOpen: (CompetitionId) -> Unit) {
    val openCount = sections.firstOrNull { it.first == EventStatus.OPEN }?.second?.size ?: 0
    val rows = sections.flatMap { (status, list) -> listOf(ListRow.Section(status, list.size)) + list.map { ListRow.Event(it, status) } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { Header(openCount, horizontal = 0.dp) }
        itemsIndexed(rows, key = { _, r -> if (r is ListRow.Event) r.competition.id.value else "section-${(r as ListRow.Section).status}" }) { i, r ->
            when (r) {
                is ListRow.Section -> SectionTitle(r.status, r.count, Modifier.staggerIn(i))
                is ListRow.Event -> if (r.status == EventStatus.OPEN) {
                    FeaturedCard(r.competition, Modifier.staggerIn(i)) { onOpen(r.competition.id) }
                } else {
                    CompactCard(r.competition, r.status, Modifier.staggerIn(i)) { onOpen(r.competition.id) }
                }
            }
        }
    }
}

private val todayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM")

/** Date, title, and how many events are taking sign-ups. [openCount] is null while loading. */
@Composable
private fun Header(openCount: Int?, horizontal: Dp = 20.dp) {
    Column(Modifier.fillMaxWidth().padding(start = horizontal, end = horizontal, top = 20.dp, bottom = 8.dp)) {
        CapsLabel(Instant.now().atZone(ZoneId.systemDefault()).format(todayFormat))
        Spacer(Modifier.height(4.dp))
        Text("Competitions", Modifier.semantics { heading() }, style = MaterialTheme.typography.displaySmall, color = Arcade.colors.text)
        if (openCount != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                when (openCount) {
                    0 -> "Nothing open for sign-up right now."
                    1 -> "1 competition open for sign-up."
                    else -> "$openCount competitions open for sign-up."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Arcade.colors.muted,
            )
        }
    }
}

@Composable
private fun SectionTitle(status: EventStatus, count: Int, modifier: Modifier = Modifier) {
    val title = when (status) {
        EventStatus.OPEN -> "Open for sign-up"
        EventStatus.CLOSED -> "Registration closed"
        EventStatus.ENDED -> "Finished"
    }
    Row(modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = Arcade.colors.text)
        Text("$count", style = NumberStyle.copy(fontSize = 13.sp), color = Arcade.colors.muted)
    }
}

/** An event taking sign-ups: live countdown and the main call to action. */
@Composable
private fun FeaturedCard(c: Competition, modifier: Modifier, onClick: () -> Unit) {
    LedgeCard(modifier.fillMaxWidth(), onClick = onClick) {
        EventHeader(c, EventStatus.OPEN, showTitle = true, descriptionLines = 2)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CapsLabel("Registration closes in")
            Countdown(c.registrationDeadline)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Teams of ${c.minTeamSize}–${c.maxTeamSize}", icon = ArcadeIcons.Users)
                Chip("Closes ${c.registrationDeadline.dayMonth()}", icon = ArcadeIcons.Calendar)
            }
            LedgeButton("Find teammates", onClick, Modifier.fillMaxWidth(), icon = ArcadeIcons.HeartFilled)
        }
    }
}

/** "Ends in 3 days", "Ends today", "Ended yesterday": the tile already shows the date, so say how far away it is. */
private fun relativeEnd(end: Instant, now: Instant = Instant.now()): String {
    val zone = ZoneId.systemDefault()
    val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), end.atZone(zone).toLocalDate())
    return when {
        days > 1 -> "Ends in $days days"
        days == 1L -> "Ends tomorrow"
        days == 0L -> if (end > now) "Ends today" else "Ended today"
        days == -1L -> "Ended yesterday"
        else -> "Ended ${-days} days ago"
    }
}

private val monthFormat = DateTimeFormatter.ofPattern("MMM")

/** A closed or finished event: one row with a calendar tile for the end date. */
@Composable
private fun CompactCard(c: Competition, status: EventStatus, modifier: Modifier, onClick: () -> Unit) {
    val colors = Arcade.colors
    val ended = status == EventStatus.ENDED
    val end = c.eventEnd.atZone(ZoneId.systemDefault())
    LedgeCard(modifier.fillMaxWidth(), color = if (ended) colors.locked else colors.surface, ledge = 4.dp, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.width(56.dp).ledge(RoundedCornerShape(14.dp), status.header, colors.line, colors.ledge, 3.dp).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(end.format(monthFormat).uppercase(), style = MaterialTheme.typography.labelSmall, color = Ink)
                Text("${end.dayOfMonth}", style = NumberStyle.copy(fontSize = 22.sp), color = Ink)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${relativeEnd(c.eventEnd)} · Teams of ${c.minTeamSize}–${c.maxTeamSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            Icon(ArcadeIcons.Back, null, tint = colors.muted, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f })
        }
    }
}
