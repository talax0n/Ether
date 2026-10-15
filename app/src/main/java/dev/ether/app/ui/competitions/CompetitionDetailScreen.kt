package dev.ether.app.ui.competitions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.Competition
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeTextField
import dev.ether.app.ui.components.ArcadeTopBar
import dev.ether.app.ui.components.Countdown
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.SectionHeader
import dev.ether.app.ui.components.Seats
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.initials
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.dayMonth
import dev.ether.app.ui.openContact
import dev.ether.app.ui.shortDate
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Mint
import dev.ether.app.ui.theme.NumberStyle
import dev.ether.app.ui.theme.Motion
import dev.ether.app.ui.theme.SkySoft
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CompetitionDetailScreen(
    competitionId: String,
    onOpenDeck: (ListingId) -> Unit,
    onOpenMatches: () -> Unit,
    onOpenTeams: () -> Unit,
    onBack: () -> Unit,
    vm: CompetitionDetailViewModel = koinViewModel { parametersOf(competitionId) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ArcadeTopBar(state.competition?.name.orEmpty(), onBack = onBack)
        val competition = state.competition
        when {
            state.loading -> LoadingBox()
            competition == null -> MessageBox(state.error ?: "Not found", action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            else -> Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompetitionInfo(competition, Modifier.staggerIn(0))
                if (state.teamForm == null) state.error?.let { ErrorText(it) }
                Column(Modifier.staggerIn(1).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ListingActions(state, onOpenDeck, onOpenMatches, onOpenTeams, vm::joinSolo, vm::openTeamForm)
                }
            }
        }
    }
    state.teamForm?.let { form ->
        TeamFormDialog(form, state.busy, state.error, vm::editTeamForm, vm::createTeam, vm::closeTeamForm)
    }
}

@Composable
private fun CompetitionInfo(c: Competition, modifier: Modifier) {
    val context = LocalContext.current
    val status = c.status()
    LedgeCard(modifier.fillMaxWidth()) {
        EventHeader(c, status, showTitle = false)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (status == EventStatus.OPEN) {
                Text("Registration closes ${c.registrationDeadline.dayMonth()}", style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted)
                Countdown(c.registrationDeadline)
            } else {
                Text("Registration closed ${c.registrationDeadline.shortDate()}", style = MaterialTheme.typography.labelMedium, color = Arcade.colors.muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(ArcadeIcons.Calendar, contentDescription = null, tint = Arcade.colors.text, modifier = Modifier.size(20.dp))
                Text(
                    "Event ends ${c.eventEnd.shortDate()} · Teams of ${c.minTeamSize}–${c.maxTeamSize}",
                    style = MaterialTheme.typography.titleSmall, color = Arcade.colors.text,
                )
            }
            c.url?.let { url ->
                LedgeButton("Official page", { openContact(context, url) }, color = Arcade.colors.surface, contentColor = Arcade.colors.text, icon = ArcadeIcons.Link, height = 48.dp)
            }
        }
    }
}

@Composable
private fun ListingActions(
    state: CompetitionDetailUiState,
    onOpenDeck: (ListingId) -> Unit,
    onOpenMatches: () -> Unit,
    onOpenTeams: () -> Unit,
    onJoinSolo: () -> Unit,
    onCreateTeam: () -> Unit,
) {
    val full = Modifier.fillMaxWidth()
    val plain = Arcade.colors.surface
    val text = Arcade.colors.text
    val listing = state.listing
    val busy = state.busy
    val team = state.team
    SectionHeader("Your entry")
    // A member who joined through a match swipes nowhere: only the owner swipes for a team.
    if (team != null && listing?.active != true) {
        Text("You're on ${team.name} in this competition.", style = MaterialTheme.typography.titleMedium, color = text)
        TeamSeats(listOf("ME") + team.teammates.map { initials(it.name) }, team.competition.maxTeamSize)
        LedgeButton("See your team", onOpenTeams, full, icon = ArcadeIcons.Users)
        LedgeButton("Matches", onOpenMatches, full, color = plain, contentColor = text, icon = ArcadeIcons.Heart)
        return
    }
    if (listing == null && !state.registrationOpen) {
        Text("Registration has closed. Nothing new can form in this competition.", style = MaterialTheme.typography.titleMedium, color = text)
        return
    }
    if (listing == null) {
        Text("How are you entering?", style = MaterialTheme.typography.titleMedium, color = text)
        LedgeButton("Join solo, find a team", onJoinSolo, full, enabled = !busy, icon = ArcadeIcons.User)
        LedgeButton("Create a team, find members", onCreateTeam, full, color = SkySoft, enabled = !busy, icon = ArcadeIcons.Users)
        return
    }
    Text(
        when (listing) {
            is Listing.Solo -> "You're in solo. Swipe on teams and other solos."
            is Listing.Team -> "Your team is listed. Swipe on solos to fill open roles."
        },
        style = MaterialTheme.typography.titleMedium,
        color = text,
    )
    if (team != null) TeamSeats(listOf("ME") + team.teammates.map { initials(it.name) }, team.competition.maxTeamSize)
    if (!listing.active) {
        Text("This listing is closed, so it no longer appears in decks.", style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted)
    }
    LedgeButton("Start swiping", { onOpenDeck(listing.id) }, full, color = Mint, enabled = listing.active, icon = ArcadeIcons.HeartFilled)
    LedgeButton("Matches", onOpenMatches, full, color = plain, contentColor = text, icon = ArcadeIcons.Heart)
}

@Composable
private fun TeamSeats(labels: List<String>, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Seats(labels, max)
        Text("${labels.size} / $max", Modifier.padding(start = 10.dp), style = NumberStyle, color = Arcade.colors.text)
    }
}

@Composable
private fun TeamFormDialog(form: TeamForm, busy: Boolean, error: String?, onChange: (TeamForm) -> Unit, onSubmit: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val reducedMotion = Arcade.reducedMotion
        val pop = remember { Animatable(if (reducedMotion) 1f else 0.85f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, Motion.pop()) }
        LedgeCard(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create a team", style = MaterialTheme.typography.headlineSmall, color = Arcade.colors.text)
                ArcadeTextField(form.name, { onChange(form.copy(name = it)) }, "Team name")
                ArcadeTextField(form.pitch, { onChange(form.copy(pitch = it)) }, "Pitch", singleLine = false, minLines = 2)
                ArcadeTextField(form.openRoles, { onChange(form.copy(openRoles = it)) }, "Open roles, comma separated", keyboardOptions = KeyboardOptions.Default)
                error?.let { ErrorText(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgeButton("Cancel", onDismiss, Modifier.weight(1f), color = Color.Transparent, contentColor = Arcade.colors.text)
                    LedgeButton("Create", onSubmit, Modifier.weight(1f), enabled = form.isValid && !busy, busy = busy)
                }
            }
        }
    }
}
