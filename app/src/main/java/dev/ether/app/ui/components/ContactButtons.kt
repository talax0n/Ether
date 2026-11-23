package dev.ether.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.ether.app.domain.ContactEntry
import dev.ether.app.domain.ContactKind
import dev.ether.app.domain.Contacts
import dev.ether.app.ui.appName
import dev.ether.app.ui.openContact
import dev.ether.app.ui.theme.MintSoft

val ContactKind.icon: ImageVector
    get() = when (this) {
        ContactKind.WHATSAPP -> ArcadeIcons.Phone
        ContactKind.INSTAGRAM -> ArcadeIcons.Camera
        ContactKind.LINE -> ArcadeIcons.Chat
    }

/** One compact button per available contact, each opening its chat app. Renders nothing when empty. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactButtons(contacts: Contacts, modifier: Modifier = Modifier, color: Color = MintSoft) {
    val entries = contacts.entries
    if (entries.isEmpty()) return
    val context = LocalContext.current
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
            LedgeButton(
                entry.kind.appName, { openContact(context, entry.url) }, Modifier.testTag("contact ${entry.kind}"),
                color = color, icon = entry.kind.icon, height = 40.dp, shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

/** Full-width button for one contact, labelled with the app and the handle or number it opens. */
@Composable
fun ContactButton(entry: ContactEntry, modifier: Modifier = Modifier, color: Color) {
    val context = LocalContext.current
    LedgeButton(
        "${entry.kind.appName} · ${entry.display}", { openContact(context, entry.url) }, modifier,
        color = color, icon = entry.kind.icon,
    )
}
