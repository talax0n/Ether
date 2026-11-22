package dev.ether.app.ui.competitions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import dev.ether.app.ui.components.CapsLabel
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.theme.CoralSoft
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.MintSoft
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
import dev.ether.app.ui.components.Countdown
import dev.ether.app.ui.components.ErrorText
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.Seats
import dev.ether.app.ui.components.TagRow
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.initials
import dev.ether.app.ui.components.ledge
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.dayMonth
import dev.ether.app.ui.openContact
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
    val competition = state.competition
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            LedgeIconButton(ArcadeIcons.Back, "Back", onBack, size = 42.dp)
            Spacer(Modifier.weight(1f))
            competition?.url?.let { url -> LedgeIconButton(ArcadeIcons.Link, "Official page", { openContact(context, url) }, size = 42.dp) }
        }
        when {
            state.loading -> LoadingBox()
            competition == null -> MessageBox(state.error ?: "Not found", action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            else -> {
                val entry = state.entry
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Overview(competition, Modifier.staggerIn(0))
                    if (competition.status() == EventStatus.OPEN) {
                        LedgeCard(Modifier.fillMaxWidth().staggerIn(1)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CapsLabel("Registration closes in")
                                Countdown(competition.registrationDeadline)
                            }
                        }
                    }
                    Text(
                        "Your entry",
                        Modifier.padding(top = 10.dp).semantics { heading() }.staggerIn(2),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = Arcade.colors.text,
                    )
                    if (state.teamForm == null) state.error?.let { ErrorText(it) }
                    Column(Modifier.staggerIn(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EntryContent(entry, state.busy, vm::joinSolo, vm::openTeamForm, onOpenMatches)
                    }
                }
                BottomAction(entry, onOpenDeck, onOpenTeams)
            }
        }
    }
    state.teamForm?.let { form ->
        TeamFormDialog(form, state.busy, state.error, vm::editTeamForm, vm::createTeam, vm::closeTeamForm)
    }
}

@Composable
private fun Overview(c: Competition, modifier: Modifier) {
    val status = c.status()
    Column(modifier) {
        Chip(status.label, color = status.chip)
        Spacer(Modifier.height(12.dp))
        Text(c.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.displaySmall, color = Arcade.colors.text)
        Spacer(Modifier.height(8.dp))
        Text(c.description, style = MaterialTheme.typography.bodyLarge, color = Arcade.colors.muted)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Teams of ${c.minTeamSize}–${c.maxTeamSize}", icon = ArcadeIcons.Users)
            Chip("${if (status == EventStatus.ENDED) "Ended" else "Ends"} ${c.eventEnd.dayMonth()}", icon = ArcadeIcons.Calendar)
        }
    }
}

@Composable
private fun EntryContent(entry: Entry, busy: Boolean, onJoinSolo: () -> Unit, onCreateTeam: () -> Unit, onOpenMatches: () -> Unit) {
    when (entry) {
        Entry.Choose -> {
            ChoiceCard(ArcadeIcons.User, MintSoft, "Join solo", "Get matched into a team. Swipe on teams and other solos.", busy, onJoinSolo)
            ChoiceCard(ArcadeIcons.Users, SkySoft, "Start a team", "Name your team, list the roles you need, and swipe on solos to fill them.", busy, onCreateTeam)
        }
        Entry.RegistrationClosed -> StatusCard(
            ArcadeIcons.Lock, Arcade.colors.track, "Registration has closed", "No new solos or teams can join this competition.",
        )
        is Entry.Listed -> {
            val team = entry.team
            StatusCard(
                if (entry.listing is Listing.Team) ArcadeIcons.Users else ArcadeIcons.User,
                if (entry.canSwipe) MintSoft else Arcade.colors.track,
                when (entry.listing) {
                    is Listing.Solo -> "You're in solo"
                    is Listing.Team -> "${team?.name ?: "Your team"} is listed"
                },
                when {
                    !entry.listing.active -> "This listing is closed, so it no longer appears in decks."
                    !entry.registrationOpen -> "Registration has closed, so swiping has stopped. Your matches are still here."
                    entry.listing is Listing.Solo -> "Swipe on teams and other solos. A mutual like puts you together."
                    else -> "Swipe on solos to fill your open roles."
                },
            ) { team?.let { TeamSeats(listOf("ME") + it.teammates.map { m -> initials(m.name) }, it.competition.maxTeamSize) } }
            LinkRow(ArcadeIcons.Heart, "Your matches", onOpenMatches)
        }
        is Entry.OnTeam -> {
            StatusCard(ArcadeIcons.Users, GrapeSoft, "You're on ${entry.team.name}", "Your team formed through a match. The owner swipes for new members.") {
                TeamSeats(listOf("ME") + entry.team.teammates.map { initials(it.name) }, entry.team.competition.maxTeamSize)
            }
            LinkRow(ArcadeIcons.Heart, "Your matches", onOpenMatches)
        }
    }
}

/** The screen's one primary action, pinned under the scroll so it never hides below the fold. */
@Composable
private fun BottomAction(entry: Entry, onOpenDeck: (ListingId) -> Unit, onOpenTeams: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
    when (entry) {
        // When swiping is off, the status card already says why; a dead button would only repeat it.
        is Entry.Listed -> if (entry.canSwipe) {
            LedgeButton("Start swiping", { onOpenDeck(entry.listing.id) }, modifier, color = Mint, icon = ArcadeIcons.HeartFilled)
        }
        is Entry.OnTeam -> LedgeButton("See your team", onOpenTeams, modifier, icon = ArcadeIcons.Users)
        Entry.Choose, Entry.RegistrationClosed -> Unit
    }
}

@Composable
private fun EntrySticker(icon: ImageVector, color: Color) {
    Box(
        Modifier.size(52.dp).ledge(RoundedCornerShape(16.dp), color, Arcade.colors.line, Arcade.colors.ledge, 3.dp),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun ChoiceCard(icon: ImageVector, color: Color, title: String, body: String, busy: Boolean, onClick: () -> Unit) {
    LedgeCard(Modifier.fillMaxWidth(), onClick = { if (!busy) onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            EntrySticker(icon, color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Arcade.colors.text)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted)
            }
            Spacer(Modifier.width(8.dp))
            Chevron()
        }
    }
}

@Composable
private fun StatusCard(icon: ImageVector, color: Color, title: String, body: String, extra: @Composable () -> Unit = {}) {
    LedgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntrySticker(icon, color)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Arcade.colors.text)
                    Spacer(Modifier.height(2.dp))
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted)
                }
            }
            extra()
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    LedgeCard(Modifier.fillMaxWidth(), ledge = 4.dp, onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Arcade.colors.text, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = Arcade.colors.text)
            Chevron()
        }
    }
}

@Composable
private fun Chevron() {
    Icon(ArcadeIcons.Back, contentDescription = null, tint = Arcade.colors.muted, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f })
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
                Text("Start a team", style = MaterialTheme.typography.headlineSmall, color = Arcade.colors.text)
                Text(
                    "Solos see your team in their deck. Say what you're building and who you need.",
                    style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted,
                )
                ArcadeTextField(form.name, { onChange(form.copy(name = it)) }, "Team name")
                ArcadeTextField(form.pitch, { onChange(form.copy(pitch = it)) }, "Pitch", singleLine = false, minLines = 2)
                ArcadeTextField(form.openRoles, { onChange(form.copy(openRoles = it)) }, "Open roles, comma separated", keyboardOptions = KeyboardOptions.Default)
                // Shows exactly what will be saved, so "Mobile, Designer" visibly becomes two roles.
                if (form.roles.isNotEmpty()) TagRow(form.roles, CoralSoft)
                error?.let { ErrorText(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgeButton("Cancel", onDismiss, Modifier.weight(1f), color = Color.Transparent, contentColor = Arcade.colors.text)
                    LedgeButton("Create", onSubmit, Modifier.weight(1f), enabled = form.isValid && !busy, busy = busy)
                }
            }
        }
    }
}
