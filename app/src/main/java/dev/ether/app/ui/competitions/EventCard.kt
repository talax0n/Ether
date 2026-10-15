package dev.ether.app.ui.competitions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ether.app.domain.Competition
import dev.ether.app.ui.components.Chip
import dev.ether.app.ui.theme.Arcade
import dev.ether.app.ui.theme.Ink
import dev.ether.app.ui.theme.MintSoft
import dev.ether.app.ui.theme.Paper
import dev.ether.app.ui.theme.Sky
import dev.ether.app.ui.theme.Sun
import dev.ether.app.ui.theme.SunSoft
import java.time.Instant

enum class EventStatus(val label: String, val header: Color, val chip: Color) {
    OPEN("● OPEN", Sky, MintSoft),
    CLOSED("CLOSED", Sun, SunSoft),
    ENDED("ENDED", Color(0xFFD9D3C7), Paper),
}

fun Competition.status(now: Instant = Instant.now()) = when {
    now > eventEnd -> EventStatus.ENDED
    now > registrationDeadline -> EventStatus.CLOSED
    else -> EventStatus.OPEN
}

/** Colored event header: status sticker, optional title, and the pitch. Text is Ink on every fill. */
@Composable
fun EventHeader(c: Competition, status: EventStatus, showTitle: Boolean, descriptionLines: Int = Int.MAX_VALUE) {
    Box(Modifier.fillMaxWidth().background(status.header)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            if (showTitle) {
                Text(c.name, Modifier.padding(end = 96.dp), style = MaterialTheme.typography.headlineSmall, color = Ink)
            }
            Text(
                c.description,
                Modifier.padding(top = if (showTitle) 4.dp else 0.dp, end = if (showTitle) 0.dp else 96.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink, maxLines = descriptionLines, overflow = TextOverflow.Ellipsis,
            )
        }
        Chip(status.label, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp), color = status.chip)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Arcade.colors.line))
    }
}
