package dev.ether.app.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
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
import dev.ether.app.ui.components.ScreenHeader
import dev.ether.app.ui.components.Seats
import dev.ether.app.ui.components.Tone
import dev.ether.app.ui.components.initials
import dev.ether.app.ui.components.staggerIn
import dev.ether.app.ui.contactLabel
import dev.ether.app.ui.openContact
import dev.ether.app.ui.shortDate
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.MintSoft
import dev.ether.app.ui.theme.NumberStyle
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
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("My teams")
            when (val s = state) {
                TeamsUiState.Loading -> LoadingBox()
                is TeamsUiState.Failed -> MessageBox(s.message, action = "Retry", tone = Tone.ERROR, onAction = vm::load)
                is TeamsUiState.Loaded ->
                    if (s.teams.isEmpty()) {
                        MessageBox("No teams yet. A mutual match in a competition puts you in one.", tone = Tone.TEAM)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
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
        }
        ArcadeSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
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
                CapsLabel(t.competition.name, color = Ink)
                Text(t.name, style = MaterialTheme.typography.headlineSmall, color = Ink)
                Text(
                    if (team.eventEnded) "Event ended ${t.competition.eventEnd.shortDate()}" else "Event ends ${t.competition.eventEnd.shortDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink,
                )
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Arcade.colors.line))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Seats(listOf("ME") + t.teammates.map { initials(it.name) }, t.competition.maxTeamSize)
                Text(
                    "${t.teammates.size + 1} / ${t.competition.maxTeamSize}",
                    Modifier.padding(start = 10.dp),
                    style = NumberStyle,
                    color = Arcade.colors.text,
                )
            }
            if (t.teammates.isEmpty()) {
                Text("No teammates yet. Keep swiping to fill your open roles.", style = MaterialTheme.typography.bodyMedium, color = Arcade.colors.text)
            }
            t.teammates.forEach { TeammateRow(it, team.eventEnded, busy, onEndorse) }
            if (team.eventEnded) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(Arcade.colors.track))
                ResultSection(t.result, busy, onReport, onConfirm)
            }
        }
    }
}

private val EndedFill = Color(0xFFD9D3C7)

@Composable
private fun TeammateRow(mate: Teammate, eventEnded: Boolean, busy: Boolean, onEndorse: (Teammate) -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(mate.name, null, 44.dp, color = Peach, ledge = 3.dp)
        Column(Modifier.weight(1f)) {
            Text(mate.name, style = MaterialTheme.typography.titleSmall, color = Arcade.colors.text)
            mate.contactUrl?.let { url ->
                Row(
                    Modifier.heightIn(min = 32.dp).clickable(role = Role.Button) { openContact(context, url) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(ArcadeIcons.Chat, contentDescription = null, tint = Arcade.colors.muted, modifier = Modifier.size(16.dp))
                    Text(contactLabel(url), style = MaterialTheme.typography.bodySmall, color = Arcade.colors.muted)
                }
            }
        }
        if (eventEnded) {
            if (mate.endorsedByMe) {
                Chip("Endorsed", color = MintSoft, icon = ArcadeIcons.Check)
            } else {
                LedgeButton(
                    "Endorse", { onEndorse(mate) }, Modifier.testTag("endorse ${mate.name}"),
                    color = Sun, enabled = !busy, icon = ArcadeIcons.ThumbUp, height = 44.dp, shape = RoundedCornerShape(14.dp),
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
            Text("Did your team place? Report it and a teammate confirms.", style = MaterialTheme.typography.bodyMedium, color = text)
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
            Text("${result.reporterName} reported ${result.result.label}.", style = MaterialTheme.typography.bodyMedium, color = text)
            LedgeButton("Confirm ${result.result.label}", onConfirm, Modifier.fillMaxWidth(), color = Sun, enabled = !busy, icon = ArcadeIcons.Check)
        }
    }
}
