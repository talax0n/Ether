package dev.ether.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
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

fun contactLabel(url: String): String = when {
    "wa.me" in url || "whatsapp" in url -> "Message on WhatsApp"
    "t.me" in url || "telegram" in url -> "Message on Telegram"
    else -> "Open contact"
}
