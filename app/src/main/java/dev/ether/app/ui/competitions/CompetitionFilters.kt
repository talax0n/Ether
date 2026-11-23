package dev.ether.app.ui.competitions

import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionMode
import java.time.Instant

/**
 * What the Compete tab is narrowed to. Within one group the chips are alternatives (Online or Hybrid);
 * across groups they all have to hold (Open and Online and tagged AI). An empty group filters nothing.
 */
data class CompetitionFilters(
    val query: String = "",
    val statuses: Set<EventStatus> = emptySet(),
    val modes: Set<CompetitionMode> = emptySet(),
    /** Lowercased, so "AI" and "ai" from different rows are one chip. */
    val tags: Set<String> = emptySet(),
) {
    val isActive: Boolean get() = query.isNotBlank() || statuses.isNotEmpty() || modes.isNotEmpty() || tags.isNotEmpty()

    fun matches(c: Competition, now: Instant): Boolean =
        matchesQuery(c) &&
            (statuses.isEmpty() || c.status(now) in statuses) &&
            (modes.isEmpty() || c.mode in modes) &&
            (tags.isEmpty() || c.tags.any { it.lowercase() in tags })

    /** Every word has to appear in the name, organizer, city or a tag, so "ai bandung" narrows. */
    private fun matchesQuery(c: Competition): Boolean {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = (listOfNotNull(c.name, c.organizer, c.city) + c.tags).joinToString("\n").lowercase()
        return words.all { it in haystack }
    }

    fun toggle(status: EventStatus) = copy(statuses = statuses.toggled(status))
    fun toggle(mode: CompetitionMode) = copy(modes = modes.toggled(mode))
    fun toggleTag(tag: String) = copy(tags = tags.toggled(tag.lowercase()))
}

private fun <T> Set<T>.toggled(item: T) = if (item in this) this - item else this + item

/** One chip per tag in the data, most common first, spelled as it first appears. */
fun List<Competition>.availableTags(): List<String> =
    flatMap { it.tags }
        .groupBy { it.lowercase() }
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, List<String>>> { it.value.size }.thenBy { it.key })
        .map { it.value.first() }

val CompetitionMode.label: String
    get() = when (this) {
        CompetitionMode.ONLINE -> "Online"
        CompetitionMode.OFFLINE -> "Offline"
        CompetitionMode.HYBRID -> "Hybrid"
    }

/** "Online", "Offline · Yogyakarta", "Hybrid · Bandung". */
val Competition.whereLabel: String
    get() = listOfNotNull(mode.label, city?.takeIf { mode != CompetitionMode.ONLINE }).joinToString(" · ")

val EventStatus.filterLabel: String
    get() = when (this) {
        EventStatus.OPEN -> "Open"
        EventStatus.CLOSED -> "Closed"
        EventStatus.ENDED -> "Finished"
    }

/** "dorahacks.io" for the button under "Open official page". */
fun officialHost(url: String): String? =
    runCatching { java.net.URI(url).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }
