package dev.ether.app.data.supabase

import dev.ether.app.domain.Badge
import dev.ether.app.domain.BestHackathon
import dev.ether.app.domain.HackathonResult
import dev.ether.app.domain.PastHackathon
import dev.ether.app.domain.PastHackathonDraft
import dev.ether.app.domain.PastHackathonId
import dev.ether.app.domain.Project
import dev.ether.app.domain.ProjectDraft
import dev.ether.app.domain.ProjectId
import dev.ether.app.domain.TrackRecordSummary
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.CompetitionMode
import dev.ether.app.domain.Contacts
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.domain.MatchId
import dev.ether.app.domain.Profile
import dev.ether.app.domain.ProfileId
import dev.ether.app.domain.Reliability
import dev.ether.app.domain.ReportedResult
import dev.ether.app.domain.TeamResult
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import dev.ether.app.domain.TeamId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

// Row shapes for the Postgres schema in supabase/migrations. Everything wire-shaped stays in this file.

@Serializable
enum class ListingKindDto {
    @SerialName("solo") SOLO,
    @SerialName("team") TEAM;

    fun toDomain() = when (this) {
        SOLO -> ListingKind.SOLO
        TEAM -> ListingKind.TEAM
    }
}

/** Postgres `swipe_direction` enum label. */
val SwipeDirection.wire: String
    get() = when (this) {
        SwipeDirection.LEFT -> "left"
        SwipeDirection.RIGHT -> "right"
    }

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val major: String? = null,
    val year: Int? = null,
    val bio: String? = null,
    val skills: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val xp: Int = 0,
) {
    fun toDomain(contacts: Contacts) = Profile(
        ProfileId(id), name, major, year, bio.orEmpty(), skills, roles, contacts, avatarUrl, xp,
    )
}

@Serializable
data class ProfileUpdateDto(
    val name: String,
    val major: String,
    val year: Int,
    val bio: String,
    val skills: List<String>,
    val roles: List<String>,
)

/** A `profile_contacts` row, as read back (RLS decides whose rows come back). */
@Serializable
data class ProfileContactDto(
    @SerialName("profile_id") val profileId: String,
    val phone: String? = null,
    val instagram: String? = null,
    @SerialName("line_id") val lineId: String? = null,
) {
    fun toDomain() = Contacts.of(phone, instagram, lineId)
}

/**
 * The writable `profile_contacts` columns. Nulls are encoded explicitly so clearing a field in the
 * form clears it in the database.
 */
@Serializable
data class ProfileContactWriteDto(
    val phone: String?,
    val instagram: String?,
    @SerialName("line_id") val lineId: String?,
) {
    constructor(contacts: Contacts) : this(contacts.phone, contacts.instagram, contacts.lineId)
}

@Serializable
data class ProfileContactInsertDto(
    @SerialName("profile_id") val profileId: String,
    val phone: String?,
    val instagram: String?,
    @SerialName("line_id") val lineId: String?,
) {
    constructor(profileId: String, contacts: Contacts) : this(profileId, contacts.phone, contacts.instagram, contacts.lineId)
}

@Serializable
data class CompetitionDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val url: String? = null,
    @SerialName("registration_deadline") val registrationDeadline: String,
    @SerialName("event_end") val eventEnd: String,
    @SerialName("min_team_size") val minTeamSize: Int,
    @SerialName("max_team_size") val maxTeamSize: Int,
    val organizer: String? = null,
    val prize: String? = null,
    val mode: CompetitionModeDto = CompetitionModeDto.ONLINE,
    val city: String? = null,
    val tags: List<String> = emptyList(),
) {
    fun toDomain() = Competition(
        CompetitionId(id), name, description.orEmpty(), url,
        parseTimestamp(registrationDeadline), parseTimestamp(eventEnd), minTeamSize, maxTeamSize,
        organizer = organizer, prize = prize, mode = mode.toDomain(), city = city, tags = tags,
    )
}

@Serializable
enum class CompetitionModeDto {
    @SerialName("online") ONLINE,
    @SerialName("offline") OFFLINE,
    @SerialName("hybrid") HYBRID;

    fun toDomain() = when (this) {
        ONLINE -> CompetitionMode.ONLINE
        OFFLINE -> CompetitionMode.OFFLINE
        HYBRID -> CompetitionMode.HYBRID
    }
}

@Serializable
data class IdDto(val id: String)

@Serializable
data class ListingDto(
    val id: String,
    @SerialName("competition_id") val competitionId: String,
    val kind: ListingKindDto,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("team_id") val teamId: String? = null,
    val active: Boolean = true,
) {
    fun toDomain(): Listing = when (kind) {
        ListingKindDto.SOLO -> Listing.Solo(
            ListingId(id), CompetitionId(competitionId), active,
            ProfileId(requireNotNull(profileId) { "solo listing $id without profile_id" }),
        )
        ListingKindDto.TEAM -> Listing.Team(
            ListingId(id), CompetitionId(competitionId), active,
            TeamId(requireNotNull(teamId) { "team listing $id without team_id" }),
        )
    }
}

@Serializable
data class SoloListingInsertDto(
    @SerialName("competition_id") val competitionId: String,
    @SerialName("profile_id") val profileId: String,
    val kind: ListingKindDto,
)

/** One row of `deck(p_listing, p_limit)`, i.e. the `listing_cards` view. */
@Serializable
data class DeckRowDto(
    @SerialName("listing_id") val listingId: String,
    val kind: ListingKindDto,
    val name: String,
    val headline: String? = null,
    val skills: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    @SerialName("open_roles") val openRoles: List<String> = emptyList(),
    @SerialName("member_count") val memberCount: Int = 1,
    val xp: Int = 0,
    @SerialName("reliability_score") val reliabilityScore: Int = 0,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("best_result") val bestResult: HackathonResultDto? = null,
    @SerialName("best_hackathon") val bestHackathon: String? = null,
    @SerialName("best_hackathon_year") val bestHackathonYear: Int? = null,
    @SerialName("hackathon_count") val hackathonCount: Int = 0,
    @SerialName("project_count") val projectCount: Int = 0,
) {
    private fun trackRecord(): TrackRecordSummary {
        val best = if (bestResult != null && bestHackathon != null && bestHackathonYear != null) {
            BestHackathon(bestResult.toDomain(), bestHackathon, bestHackathonYear)
        } else {
            null
        }
        return TrackRecordSummary(best, hackathonCount, projectCount)
    }

    fun toDomain(): DeckCard = when (kind) {
        ListingKindDto.SOLO -> DeckCard.Solo(
            ListingId(listingId), name, avatarUrl, headline.orEmpty(), skills, roles, xp, Reliability(reliabilityScore),
            trackRecord(),
        )
        ListingKindDto.TEAM -> DeckCard.Team(
            ListingId(listingId), name, avatarUrl, headline.orEmpty(), openRoles, memberCount, xp, Reliability(reliabilityScore),
            trackRecord(),
        )
    }
}

@Serializable
data class SwipeResultDto(
    val matched: Boolean,
    @SerialName("match_id") val matchId: String? = null,
    val remaining: Int,
) {
    fun toDomain() = SwipeResult(matched, matchId?.let(::MatchId), remaining)
}

/** One row of `my_matches(p_competition)`: the other side's card plus its contacts. */
@Serializable
data class MatchRowDto(
    @SerialName("match_id") val matchId: String,
    @SerialName("matched_at") val matchedAt: String,
    @SerialName("listing_id") val listingId: String,
    val kind: ListingKindDto,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val phone: String? = null,
    val instagram: String? = null,
    @SerialName("line_id") val lineId: String? = null,
) {
    fun toDomain(competitionId: CompetitionId) = Match(
        MatchId(matchId), competitionId, ListingId(listingId), kind.toDomain(),
        name, avatarUrl, Contacts.of(phone, instagram, lineId), parseTimestamp(matchedAt),
    )
}

@Serializable
data class ProfileBadgeDto(
    @SerialName("awarded_at") val awardedAt: String,
    val badges: BadgeDto,
) {
    fun toDomain() = Badge(badges.code, badges.name, badges.description, parseTimestamp(awardedAt))
}

@Serializable
data class BadgeDto(val code: String, val name: String, val description: String = "")

@Serializable
data class TeamIdDto(@SerialName("team_id") val teamId: String)

/** `teams` with its competition and members embedded. */
@Serializable
data class TeamRowDto(
    val id: String,
    val name: String,
    @SerialName("owner_id") val ownerId: String,
    val competitions: CompetitionDto,
    @SerialName("team_members") val members: List<TeamMemberDto>,
)

@Serializable
data class TeamMemberDto(@SerialName("profile_id") val profileId: String, val profiles: NameDto)

@Serializable
data class NameDto(val name: String)

@Serializable
data class EndorsementKeyDto(
    @SerialName("to_profile_id") val toProfileId: String,
    @SerialName("competition_id") val competitionId: String,
)

@Serializable
data class EndorsementInsertDto(
    @SerialName("from_profile_id") val fromProfileId: String,
    @SerialName("to_profile_id") val toProfileId: String,
    @SerialName("competition_id") val competitionId: String,
    val positive: Boolean,
)

@Serializable
enum class TeamResultDto {
    @SerialName("finalist") FINALIST,
    @SerialName("winner") WINNER;

    fun toDomain() = when (this) {
        FINALIST -> TeamResult.FINALIST
        WINNER -> TeamResult.WINNER
    }
}

/** Postgres `team_result` enum label. */
val TeamResult.wire: String
    get() = when (this) {
        TeamResult.FINALIST -> "finalist"
        TeamResult.WINNER -> "winner"
    }

@Serializable
data class TeamResultRowDto(
    @SerialName("team_id") val teamId: String,
    val result: TeamResultDto,
    @SerialName("reported_by") val reportedBy: String,
    @SerialName("confirmed_by") val confirmedBy: String? = null,
)

/** Postgres `hackathon_result` enum. */
@Serializable
enum class HackathonResultDto {
    @SerialName("participant") PARTICIPANT,
    @SerialName("honorable_mention") HONORABLE_MENTION,
    @SerialName("finalist") FINALIST,
    @SerialName("winner") WINNER;

    fun toDomain() = when (this) {
        PARTICIPANT -> HackathonResult.PARTICIPANT
        HONORABLE_MENTION -> HackathonResult.HONORABLE_MENTION
        FINALIST -> HackathonResult.FINALIST
        WINNER -> HackathonResult.WINNER
    }

    companion object {
        fun of(result: HackathonResult) = when (result) {
            HackathonResult.PARTICIPANT -> PARTICIPANT
            HackathonResult.HONORABLE_MENTION -> HONORABLE_MENTION
            HackathonResult.FINALIST -> FINALIST
            HackathonResult.WINNER -> WINNER
        }
    }
}

@Serializable
data class HackathonRowDto(
    val id: String,
    val name: String,
    val year: Int,
    val result: HackathonResultDto,
    @SerialName("project_name") val projectName: String? = null,
    val url: String? = null,
) {
    fun toDomain() = PastHackathon(PastHackathonId(id), name, year, result.toDomain(), projectName, url)
}

/** Writable `profile_hackathons` columns. Nulls are encoded so clearing a field clears it in the database. */
@Serializable
data class HackathonWriteDto(
    val name: String,
    val year: Int,
    val result: HackathonResultDto,
    @SerialName("project_name") val projectName: String?,
    val url: String?,
) {
    constructor(d: PastHackathonDraft) : this(d.name, d.year, HackathonResultDto.of(d.result), d.projectName, d.url)
}

@Serializable
data class HackathonInsertDto(
    @SerialName("profile_id") val profileId: String,
    val name: String,
    val year: Int,
    val result: HackathonResultDto,
    @SerialName("project_name") val projectName: String?,
    val url: String?,
) {
    constructor(profileId: String, d: PastHackathonDraft) :
        this(profileId, d.name, d.year, HackathonResultDto.of(d.result), d.projectName, d.url)
}

@Serializable
data class ProjectRowDto(
    val id: String,
    val title: String,
    val blurb: String,
    val url: String? = null,
    val tags: List<String> = emptyList(),
) {
    fun toDomain() = Project(ProjectId(id), title, blurb, url, tags)
}

@Serializable
data class ProjectWriteDto(val title: String, val blurb: String, val url: String?, val tags: List<String>) {
    constructor(d: ProjectDraft) : this(d.title, d.blurb, d.url, d.tags)
}

@Serializable
data class ProjectInsertDto(
    @SerialName("profile_id") val profileId: String,
    val title: String,
    val blurb: String,
    val url: String?,
    val tags: List<String>,
) {
    constructor(profileId: String, d: ProjectDraft) : this(profileId, d.title, d.blurb, d.url, d.tags)
}

internal fun parseTimestamp(value: String): Instant = OffsetDateTime.parse(value).toInstant()
