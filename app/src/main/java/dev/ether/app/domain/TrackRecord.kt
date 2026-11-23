package dev.ether.app.domain

@JvmInline value class PastHackathonId(val value: String)
@JvmInline value class ProjectId(val value: String)

/** How a past hackathon went. Declared in rank order, best last, like the Postgres `hackathon_result` enum. */
enum class HackathonResult { PARTICIPANT, HONORABLE_MENTION, FINALIST, WINNER }

data class PastHackathon(
    val id: PastHackathonId,
    val name: String,
    val year: Int,
    val result: HackathonResult,
    val projectName: String?,
    val url: String?,
)

data class Project(
    val id: ProjectId,
    val title: String,
    val blurb: String,
    val url: String?,
    val tags: List<String>,
)

/** The signed-in user's own past hackathons (newest first) and projects (newest first). */
data class TrackRecord(val hackathons: List<PastHackathon>, val projects: List<Project>) {
    val canAddHackathon: Boolean get() = hackathons.size < TrackRecordRules.MAX_ENTRIES
    val canAddProject: Boolean get() = projects.size < TrackRecordRules.MAX_ENTRIES

    companion object {
        val EMPTY = TrackRecord(emptyList(), emptyList())
    }
}

/** What the user writes for a past hackathon. Values are already trimmed and validated. */
data class PastHackathonDraft(
    val name: String,
    val year: Int,
    val result: HackathonResult,
    val projectName: String?,
    val url: String?,
)

data class ProjectDraft(val title: String, val blurb: String, val url: String?, val tags: List<String>)

/** The best result among a card's hackathons, as picked by the database (best result, then most recent). */
data class BestHackathon(val result: HackathonResult, val name: String, val year: Int)

/** The compact track record on a deck card. For a team it covers every current member. */
data class TrackRecordSummary(val best: BestHackathon?, val hackathonCount: Int, val projectCount: Int) {
    val isEmpty: Boolean get() = best == null && projectCount == 0

    companion object {
        val NONE = TrackRecordSummary(null, 0, 0)
    }
}

/**
 * Mirrors the check constraints and triggers on `profile_hackathons` / `profile_projects` so the form
 * can say what's wrong before saving. The database stays the source of truth.
 */
object TrackRecordRules {
    const val MAX_ENTRIES = 20
    const val MIN_YEAR = 2000
    const val NAME_MAX = 80
    const val BLURB_MAX = 140
    const val TAGS_MAX = 6
    const val TAG_MAX = 30
    private const val URL_MAX = 500

    /** A hackathon can be next year's: registered for, not held yet. */
    fun maxYear(currentYear: Int) = currentYear + 1

    private val scheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")
    private val httpsUrl = Regex("^https://[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+(:[0-9]{1,5})?([/?#]\\S*)?$")

    /**
     * The link as stored (https only), or null when [raw] is not an https link. A bare
     * "github.com/x" gets "https://" in front; "http://" and other schemes are rejected.
     */
    fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        val withScheme = when {
            trimmed.startsWith("https://", ignoreCase = true) -> "https://" + trimmed.drop(8)
            scheme.containsMatchIn(trimmed) -> return null
            else -> "https://$trimmed"
        }
        return withScheme.takeIf { it.length <= URL_MAX && httpsUrl.matches(it) }
    }

    /** Comma-separated tags, trimmed and de-duplicated. Validity is checked by [tagsValid]. */
    fun splitTags(raw: String): List<String> = raw.split(',').map(String::trim).filter(String::isNotEmpty).distinct()

    fun tagsValid(tags: List<String>): Boolean = tags.size <= TAGS_MAX && tags.all { it.length <= TAG_MAX }
}
