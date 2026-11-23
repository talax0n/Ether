package dev.ether.app.ui.profile

import dev.ether.app.domain.HackathonResult
import dev.ether.app.domain.PastHackathon
import dev.ether.app.domain.PastHackathonDraft
import dev.ether.app.domain.Project
import dev.ether.app.domain.ProjectDraft
import dev.ether.app.domain.TrackRecordRules

/** Raw text of the past-hackathon form. It becomes a [PastHackathonDraft] only when every field is valid. */
data class HackathonForm(
    val name: String = "",
    val year: String = "",
    val result: HackathonResult = HackathonResult.PARTICIPANT,
    val projectName: String = "",
    val url: String = "",
) {
    val nameInvalid get() = name.trim().length > TrackRecordRules.NAME_MAX
    val projectNameInvalid get() = projectName.trim().length > TrackRecordRules.NAME_MAX
    val urlInvalid get() = url.isNotBlank() && TrackRecordRules.normalizeUrl(url) == null

    /** Filled in and outside 2000..next year. */
    fun yearInvalid(currentYear: Int): Boolean =
        year.isNotBlank() && year.trim().toIntOrNull()?.takeIf { it in TrackRecordRules.MIN_YEAR..TrackRecordRules.maxYear(currentYear) } == null

    fun toDraftOrNull(currentYear: Int): PastHackathonDraft? {
        if (name.isBlank() || nameInvalid || projectNameInvalid || urlInvalid || year.isBlank() || yearInvalid(currentYear)) return null
        return PastHackathonDraft(
            name = name.trim(),
            year = year.trim().toInt(),
            result = result,
            projectName = projectName.trim().ifEmpty { null },
            url = url.takeIf { it.isNotBlank() }?.let(TrackRecordRules::normalizeUrl),
        )
    }

    companion object {
        fun new(currentYear: Int) = HackathonForm(year = currentYear.toString())
        fun of(h: PastHackathon) = HackathonForm(h.name, h.year.toString(), h.result, h.projectName.orEmpty(), h.url.orEmpty())
    }
}

/** Raw text of the project form. [tags] is comma separated. */
data class ProjectForm(
    val title: String = "",
    val blurb: String = "",
    val url: String = "",
    val tags: String = "",
) {
    val tagList get() = TrackRecordRules.splitTags(tags)
    val titleInvalid get() = title.trim().length > TrackRecordRules.NAME_MAX
    val blurbInvalid get() = blurb.trim().length > TrackRecordRules.BLURB_MAX
    val urlInvalid get() = url.isNotBlank() && TrackRecordRules.normalizeUrl(url) == null
    val tagsInvalid get() = !TrackRecordRules.tagsValid(tagList)

    fun toDraftOrNull(): ProjectDraft? {
        if (title.isBlank() || blurb.isBlank() || titleInvalid || blurbInvalid || urlInvalid || tagsInvalid) return null
        return ProjectDraft(
            title = title.trim(),
            // One line: a pasted newline would make the card blurb wrap oddly.
            blurb = blurb.trim().replace(Regex("\\s*\\n\\s*"), " "),
            url = url.takeIf { it.isNotBlank() }?.let(TrackRecordRules::normalizeUrl),
            tags = tagList,
        )
    }

    companion object {
        fun of(p: Project) = ProjectForm(p.title, p.blurb, p.url.orEmpty(), p.tags.joinToString(", "))
    }
}
