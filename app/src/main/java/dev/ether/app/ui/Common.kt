package dev.ether.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import dev.ether.app.domain.ContactKind
import dev.ether.app.domain.Contacts
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "Something went wrong"

fun openContact(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open $url", Toast.LENGTH_LONG).show()
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
fun Instant.shortDate(): String = atZone(ZoneId.systemDefault()).format(dateFormat)

private val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM")
fun Instant.dayMonth(): String = atZone(ZoneId.systemDefault()).format(dayMonthFormat)

/** The app a contact opens in. */
val ContactKind.appName: String
    get() = when (this) {
        ContactKind.WHATSAPP -> "WhatsApp"
        ContactKind.INSTAGRAM -> "Instagram"
        ContactKind.LINE -> "LINE"
    }

/** "WhatsApp, Instagram and LINE", or null when there are no contacts. */
fun Contacts.appsSummary(): String? {
    val names = entries.map { it.kind.appName }
    return when (names.size) {
        0 -> null
        1 -> names[0]
        else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
    }
}
