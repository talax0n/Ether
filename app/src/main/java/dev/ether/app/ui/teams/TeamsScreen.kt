package dev.ether.app.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.ether.app.ui.components.LedgeIconButton
import dev.ether.app.ui.dayMonth
import dev.ether.app.ui.theme.CoralSoft
import dev.ether.app.ui.theme.Paper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ether.app.domain.ReportedResult
import dev.ether.app.domain.TeamResult
import dev.ether.app.domain.Teammate
import dev.ether.app.ui.components.ArcadeIcons
import dev.ether.app.ui.components.ArcadeSnackbarHost
import dev.ether.app.ui.components.Avatar
import dev.ether.app.ui.components.CapsLabel
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.components.LedgeButton
import dev.ether.app.ui.components.LedgeCard
import dev.ether.app.ui.components.LoadingBox
import dev.ether.app.ui.components.MessageBox
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.contactLabel
import dev.ether.app.ui.openContact
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.MintSoft
import dev.ether.app.ui.theme.Peach
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.Sun
import dev.ether.app.ui.theme.SunSoft
import org.koin.compose.viewmodel.koinViewModel

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
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            TeamsUiState.Loading -> Column(Modifier.fillMaxSize()) { Header(null); LoadingBox() }
            is TeamsUiState.Failed -> Column(Modifier.fillMaxSize()) {
                Header(null)
                MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
            }
            is TeamsUiState.Loaded -> if (s.teams.isEmpty()) {
                Column(Modifier.fillMaxSize()) {
                    Header(null)
                    MessageBox("No teams yet. A mutual match in a competition puts you in one.", tone = Tone.TEAM)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "header") { Header(s.teams, horizontal = 0.dp) }
                    itemsIndexed(s.teams, key = { _, t -> t.team.id.value }) { i, team ->
                        TeamCard(
                            team = team,
                            busy = s.busy,
                            onEndorse = { vm.endorse(team, it) },
                            onReport = { vm.reportResult(team, it) },
                            onConfirm = { vm.confirmResult(team) },
                            modifier = Modifier.staggerIn(i),
                        )
                    }
                }
            }
        }
        ArcadeSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** Title plus a one-line summary of what's waiting. [teams] is null while there is nothing to summarize. */
@Composable
private fun Header(teams: List<TeamView>?, horizontal: Dp = 20.dp) {
    val toDo = teams?.sumOf { it.toDo } ?: 0
    Column(Modifier.fillMaxWidth().padding(start = horizontal, end = horizontal, top = 20.dp, bottom = 4.dp)) {
        Text("Teams", Modifier.semantics { heading() }, style = MaterialTheme.typography.displaySmall, color = Arcade.colors.text)
        if (teams != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(if (teams.size == 1) "1 team" else "${teams.size} teams")
                    if (toDo > 0) append(" · ${if (toDo == 1) "1 thing" else "$toDo things"} to do")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Arcade.colors.muted,
            )
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
    modifier: Modifier,
) {
    val t = team.team
    LedgeCard(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(if (team.eventEnded) EndedFill else Sky)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CapsLabel(t.competition.name, Modifier.weight(1f), color = Ink)
                    if (team.toDo > 0) Chip("${team.toDo} to do", color = CoralSoft)
                }
                Text(t.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("${t.teammates.size + 1}/${t.competition.maxTeamSize} members", color = Paper, icon = ArcadeIcons.Users)
                    Chip(
                        "${if (team.eventEnded) "Ended" else "Ends"} ${t.competition.eventEnd.dayMonth()}",
                        color = Paper, icon = ArcadeIcons.Calendar,
                    )
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Arcade.colors.line))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (t.teammates.isEmpty()) {
                Text("No teammates yet. Keep swiping to fill your open roles.", style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.muted)
            }
            if (team.eventEnded && t.teammates.any { !it.endorsedByMe }) {
                CapsLabel("Endorse your teammates · +30 XP each")
            }
            t.teammates.forEach { TeammateRow(it, isCaptain = it.id == t.ownerId, team.eventEnded, busy, onEndorse) }
            if (team.eventEnded) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(Arcade.colors.track))
                ResultSection(t.result, busy, onReport, onConfirm)
            }
        }
    }
}

private val EndedFill = Color(0xFFD9D3C7)

@Composable
private fun TeammateRow(mate: Teammate, isCaptain: Boolean, eventEnded: Boolean, busy: Boolean, onEndorse: (Teammate) -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(mate.name, null, 48.dp, color = Peach, ledge = 3.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(mate.name, style = MaterialTheme.typography.titleMedium, color = Arcade.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val detail = listOfNotNull(
                "Captain".takeIf { isCaptain },
                mate.contactUrl?.let { contactLabel(it).removePrefix("Message on ") },
            ).joinToString(" · ")
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, color = Arcade.colors.muted)
        }
        mate.contactUrl?.let { url ->
            Spacer(Modifier.width(8.dp))
            LedgeIconButton(ArcadeIcons.Chat, contactLabel(url), { openContact(context, url) }, size = 44.dp, color = MintSoft, tint = Ink)
        }
        if (eventEnded) {
            Spacer(Modifier.width(8.dp))
            if (mate.endorsedByMe) {
                Chip("Endorsed", color = MintSoft, icon = ArcadeIcons.Check)
            } else {
                LedgeButton(
                    "Endorse", { onEndorse(mate) }, Modifier.testTag("endorse ${mate.name}"),
                    color = Sun, enabled = !busy, height = 44.dp, shape = RoundedCornerShape(14.dp),
                )
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
    val text = Arcade.colors.text
    Text("Result", style = MaterialTheme.typography.titleMedium, color = text)
    when {
        result == null -> {
            Text("Did your team place? Report it and a teammate confirms. A confirmed result is worth 200 XP.", style = MaterialTheme.typography.bodyMedium, color = text)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TeamResult.entries.forEach { r ->
                    LedgeButton(
                        r.label, { onReport(r) }, Modifier.weight(1f), enabled = !busy,
                        color = if (r == TeamResult.WINNER) SunSoft else GrapeSoft,
                        icon = if (r == TeamResult.WINNER) ArcadeIcons.Star else ArcadeIcons.Flag,
                    )
                }
            }
        }
        result.confirmed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip(result.result.label, color = if (result.result == TeamResult.WINNER) SunSoft else GrapeSoft, icon = ArcadeIcons.Trophy)
            Text("${result.result.label}, confirmed by your team.", style = MaterialTheme.typography.bodyMedium, color = text)
        }
        result.reportedByMe -> Text(
            "You reported ${result.result.label}. Waiting for a teammate to confirm.",
            style = MaterialTheme.typography.bodyMedium,
            color = text,
        )
        else -> {
            Text("${result.reporterName} reported ${result.result.label}. Confirm it to lock in 200 XP.", style = MaterialTheme.typography.bodyMedium, color = text)
            LedgeButton("Confirm ${result.result.label}", onConfirm, Modifier.fillMaxWidth(), color = Sun, enabled = !busy, icon = ArcadeIcons.Check)
        }
    }
}
