package dev.ether.app.ui.competitions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import dev.ether.app.domain.CompetitionMode
import dev.ether.app.ui.components.ArcadeTextField
import dev.ether.app.ui.components.BarIconButton
import dev.ether.app.ui.components.ToggleChip
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.SunSoft
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
            CompetitionList(
                s, onOpen,
                FilterActions(vm::setQuery, vm::toggleStatus, vm::toggleMode, vm::toggleTag, vm::clearFilters),
            )
        }
    }
}

/** The list's search and filter callbacks, bundled so the composables below stay readable. */
private class FilterActions(
    val onQuery: (String) -> Unit,
    val onStatus: (EventStatus) -> Unit,
    val onMode: (CompetitionMode) -> Unit,
    val onTag: (String) -> Unit,
    val onClear: () -> Unit,
)

private sealed interface ListRow {
    data class Section(val status: EventStatus, val count: Int) : ListRow
    data class Event(val competition: Competition, val status: EventStatus) : ListRow
}

@Composable
private fun CompetitionList(s: CompetitionListUiState.Loaded, onOpen: (CompetitionId) -> Unit, actions: FilterActions) {
    val rows = s.sections.flatMap { (status, list) -> listOf(ListRow.Section(status, list.size)) + list.map { ListRow.Event(it, status) } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { Header(s.openCount, horizontal = 0.dp) }
        item(key = "filters") { SearchAndFilters(s.filters, s.tags, actions) }
        if (s.filters.isActive && s.results.isNotEmpty()) {
            item(key = "result-count") { ResultCount(s.results.size, s.competitions.size, actions.onClear) }
        }
        if (s.results.isEmpty()) {
            item(key = "no-results") {
                MessageBox(
                    "Try another word, or turn off a filter or two.",
                    Modifier.fillParentMaxHeight(0.6f),
                    title = "No hackathons match",
                    action = "Clear filters",
                    tone = Tone.EMPTY,
                    icon = ArcadeIcons.Search,
                    onAction = actions.onClear,
                )
            }
        }
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

@Composable
private fun SearchAndFilters(filters: CompetitionFilters, tags: List<String>, actions: FilterActions) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ArcadeTextField(
            filters.query, actions.onQuery, "Search name, organizer, tag or city",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            trailingIcon = {
                if (filters.query.isEmpty()) {
                    Icon(ArcadeIcons.Search, contentDescription = null, tint = Arcade.colors.muted, modifier = Modifier.size(22.dp))
                } else {
                    BarIconButton(ArcadeIcons.Close, "Clear search", { actions.onQuery("") })
                }
            },
        )
        Spacer(Modifier.height(4.dp))
        ChipStrip {
            EventStatus.entries.forEach { status ->
                ToggleChip(status.filterLabel, status in filters.statuses, { actions.onStatus(status) })
            }
            Box(Modifier.padding(horizontal = 2.dp).width(2.dp).height(22.dp).background(Arcade.colors.line))
            CompetitionMode.entries.forEach { mode ->
                ToggleChip(mode.label, mode in filters.modes, { actions.onMode(mode) }, selectedColor = Sky)
            }
        }
        if (tags.isNotEmpty()) {
            ChipStrip {
                tags.forEach { tag ->
                    ToggleChip(tag, tag.lowercase() in filters.tags, { actions.onTag(tag) }, selectedColor = Mint)
                }
            }
        }
    }
}

/** One row of chips that scrolls sideways instead of wrapping, so the list starts high on the screen. */
@Composable
private fun ChipStrip(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun ResultCount(shown: Int, total: Int, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$shown of $total hackathons", Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted,
        )
        Text(
            "Clear filters",
            Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClear).padding(horizontal = 4.dp).wrapContentHeight(),
            style = MaterialTheme.typography.titleSmall, color = Arcade.colors.text, textDecoration = TextDecoration.Underline,
        )
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WhereChip(c)
                c.prize?.let { Chip(it, icon = ArcadeIcons.Trophy, color = SunSoft) }
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
                Text(
                    listOfNotNull(c.whereLabel, c.prize).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(ArcadeIcons.Back, null, tint = colors.muted, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f })
        }
    }
}

/** "Online", or the city with a pin for offline and hybrid events. */
@Composable
fun WhereChip(c: Competition) {
    Chip(c.whereLabel, icon = if (c.mode == CompetitionMode.ONLINE) ArcadeIcons.Globe else ArcadeIcons.Pin, color = SkySoft)
}
