package dev.ether.app.data.supabase

import dev.ether.app.domain.Badge
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
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
    fun toDomain(contactUrl: String?) = Profile(
        ProfileId(id), name, major, year, bio.orEmpty(), skills, roles, contactUrl, avatarUrl, xp,
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

@Serializable
data class ProfileContactDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("contact_url") val contactUrl: String,
)

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
) {
    fun toDomain() = Competition(
        CompetitionId(id), name, description.orEmpty(), url,
        parseTimestamp(registrationDeadline), parseTimestamp(eventEnd), minTeamSize, maxTeamSize,
    )
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
    val kind: ListingKindDto = ListingKindDto.SOLO,
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
) {
    fun toDomain(): DeckCard = when (kind) {
        ListingKindDto.SOLO -> DeckCard.Solo(
            ListingId(listingId), name, avatarUrl, headline.orEmpty(), skills, roles, xp, Reliability(reliabilityScore),
        )
        ListingKindDto.TEAM -> DeckCard.Team(
            ListingId(listingId), name, avatarUrl, headline.orEmpty(), openRoles, memberCount, xp, Reliability(reliabilityScore),
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

/** One row of `my_matches(p_competition)`: the other side's card plus its contact link. */
@Serializable
data class MatchRowDto(
    @SerialName("match_id") val matchId: String,
    @SerialName("matched_at") val matchedAt: String,
    @SerialName("listing_id") val listingId: String,
    val kind: ListingKindDto,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("contact_url") val contactUrl: String? = null,
) {
    fun toDomain(competitionId: CompetitionId) = Match(
        MatchId(matchId), competitionId, ListingId(listingId), kind.toDomain(),
        name, avatarUrl, contactUrl, parseTimestamp(matchedAt),
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
    val positive: Boolean = true,
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

internal fun parseTimestamp(value: String): Instant = OffsetDateTime.parse(value).toInstant()
