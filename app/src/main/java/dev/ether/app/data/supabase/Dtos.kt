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
    val major: String,
    val year: Int,
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val xp: Int = 0,
) {
    fun toDomain(contactUrl: String?) = Profile(
        ProfileId(id), name, major, year, bio, skills, roles, contactUrl, avatarUrl, xp,
    )
}

@Serializable
data class ProfileUpsertDto(
    val id: String,
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
    val description: String = "",
    val url: String? = null,
    @SerialName("registration_deadline") val registrationDeadline: String,
    @SerialName("event_end") val eventEnd: String,
    @SerialName("min_team_size") val minTeamSize: Int,
    @SerialName("max_team_size") val maxTeamSize: Int,
) {
    fun toDomain() = Competition(
        CompetitionId(id), name, description, url,
        parseTimestamp(registrationDeadline), parseTimestamp(eventEnd), minTeamSize, maxTeamSize,
    )
}

@Serializable
data class TeamInsertDto(
    @SerialName("competition_id") val competitionId: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val pitch: String,
    @SerialName("open_roles") val openRoles: List<String>,
)

@Serializable
data class IdDto(val id: String)

@Serializable
data class TeamMemberDto(
    @SerialName("team_id") val teamId: String,
    @SerialName("profile_id") val profileId: String,
)

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
data class ListingInsertDto(
    @SerialName("competition_id") val competitionId: String,
    val kind: ListingKindDto,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("team_id") val teamId: String? = null,
)

/** One row of `deck(p_listing, p_limit)`. Columns are provisional until the DB migration lands. */
@Serializable
data class DeckRowDto(
    @SerialName("listing_id") val listingId: String,
    val kind: ListingKindDto,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val major: String? = null,
    val year: Int? = null,
    val bio: String? = null,
    val skills: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val xp: Int = 0,
    @SerialName("endorsements_positive") val endorsementsPositive: Int = 0,
    @SerialName("endorsements_total") val endorsementsTotal: Int = 0,
    val pitch: String? = null,
    @SerialName("open_roles") val openRoles: List<String> = emptyList(),
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_team_size") val maxTeamSize: Int = 0,
) {
    fun toDomain(): DeckCard = when (kind) {
        ListingKindDto.SOLO -> DeckCard.Solo(
            ListingId(listingId), name, avatarUrl, major.orEmpty(), year ?: 0, bio.orEmpty(),
            skills, roles, xp, Reliability(endorsementsPositive, endorsementsTotal),
        )
        ListingKindDto.TEAM -> DeckCard.Team(
            ListingId(listingId), name, avatarUrl, pitch.orEmpty(), openRoles, memberCount, maxTeamSize,
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

/** One row of `my_matches(p_competition)`. Columns are provisional until the DB migration lands. */
@Serializable
data class MatchRowDto(
    @SerialName("match_id") val matchId: String,
    @SerialName("competition_id") val competitionId: String,
    @SerialName("other_listing_id") val otherListingId: String,
    @SerialName("other_kind") val otherKind: ListingKindDto,
    @SerialName("other_name") val otherName: String,
    @SerialName("other_avatar_url") val otherAvatarUrl: String? = null,
    @SerialName("contact_url") val contactUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun toDomain() = Match(
        MatchId(matchId), CompetitionId(competitionId), ListingId(otherListingId), otherKind.toDomain(),
        otherName, otherAvatarUrl, contactUrl, parseTimestamp(createdAt),
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
data class EndorsementDto(val positive: Boolean)

internal fun parseTimestamp(value: String): Instant = OffsetDateTime.parse(value).toInstant()
