package dev.ether.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ether.app.domain.BestHackathon
import dev.ether.app.domain.HackathonResult
import dev.ether.app.domain.TrackRecordSummary
import dev.ether.app.ui.theme.GrapeSoft
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.theme.SkySoft
import dev.ether.app.ui.theme.SunSoft

val HackathonResult.label: String
    get() = when (this) {
        HackathonResult.PARTICIPANT -> "Participant"
        HackathonResult.HONORABLE_MENTION -> "Honorable mention"
        HackathonResult.FINALIST -> "Finalist"
        HackathonResult.WINNER -> "Winner"
    }

val HackathonResult.color: Color
    get() = when (this) {
        HackathonResult.PARTICIPANT -> Paper
        HackathonResult.HONORABLE_MENTION -> SkySoft
        HackathonResult.FINALIST -> GrapeSoft
        HackathonResult.WINNER -> SunSoft
    }

/** "Winner · GEMASTIK 2025" */
val BestHackathon.headline: String get() = "${result.label} · $name $year"

fun projectCountLabel(n: Int) = if (n == 1) "1 project" else "$n projects"

fun hackathonCountLabel(n: Int) = if (n == 1) "1 hackathon" else "$n hackathons"

/** One line for a deck card: the best hackathon result, then the project count. Nothing when both are empty. */
@Composable
fun TrackRecordLine(summary: TrackRecordSummary, modifier: Modifier = Modifier) {
    if (summary.isEmpty) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        // The best result shrinks (with an ellipsis) before the project count does.
        summary.best?.let { Chip(it.headline, Modifier.weight(1f, fill = false), color = it.result.color, icon = ArcadeIcons.Trophy) }
        if (summary.projectCount > 0) Chip(projectCountLabel(summary.projectCount), color = Paper, icon = ArcadeIcons.Bolt)
    }
}
