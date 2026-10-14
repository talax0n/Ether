package dev.ether.app.domain

import java.time.Instant

@JvmInline value class ProfileId(val value: String)
@JvmInline value class CompetitionId(val value: String)
@JvmInline value class TeamId(val value: String)
@JvmInline value class ListingId(val value: String)
@JvmInline value class MatchId(val value: String)

data class Profile(
    val id: ProfileId,
    val name: String,
    val major: String?,
    val year: Int?,
    val bio: String,
    val skills: List<String>,
    val roles: List<String>,
    val contactUrl: String?,
    val avatarUrl: String?,
    val xp: Int,
)

/** What the user can edit about themselves. XP and id are owned by the database. */
data class ProfileDraft(
    val name: String,
    val major: String,
    val year: Int,
    val bio: String,
    val skills: List<String>,
    val roles: List<String>,
    val contactUrl: String,
)

data class Competition(
    val id: CompetitionId,
    val name: String,
    val description: String,
    val url: String?,
    val registrationDeadline: Instant,
    val eventEnd: Instant,
    val minTeamSize: Int,
    val maxTeamSize: Int,
)

data class Team(
    val id: TeamId,
    val competitionId: CompetitionId,
    val ownerId: ProfileId,
    val name: String,
    val pitch: String,
    val openRoles: List<String>,
)

enum class ListingKind { SOLO, TEAM }

sealed interface Listing {
    val id: ListingId
    val competitionId: CompetitionId
    val active: Boolean
    val kind: ListingKind

    data class Solo(
        override val id: ListingId,
        override val competitionId: CompetitionId,
        override val active: Boolean,
        val profileId: ProfileId,
    ) : Listing {
        override val kind get() = ListingKind.SOLO
    }

    data class Team(
        override val id: ListingId,
        override val competitionId: CompetitionId,
        override val active: Boolean,
        val teamId: TeamId,
    ) : Listing {
        override val kind get() = ListingKind.TEAM
    }
}

enum class SwipeDirection { LEFT, RIGHT }

data class SwipeResult(val matched: Boolean, val matchId: MatchId?, val remaining: Int)

/** The database refused a swipe because today's cap is used up. */
class SwipeCapReachedException : Exception("Daily swipe limit reached")

/** Positive endorsements received, as computed by the database's `reliability_score`. */
@JvmInline value class Reliability(val endorsements: Int) {
    val label: String get() = if (endorsements == 0) "New" else "$endorsements endorsements"
}

sealed interface DeckCard {
    val listingId: ListingId
    val name: String
    val avatarUrl: String?

    data class Solo(
        override val listingId: ListingId,
        override val name: String,
        override val avatarUrl: String?,
        val headline: String,
        val skills: List<String>,
        val roles: List<String>,
        val xp: Int,
        val reliability: Reliability,
    ) : DeckCard

    data class Team(
        override val listingId: ListingId,
        override val name: String,
        override val avatarUrl: String?,
        val pitch: String,
        val openRoles: List<String>,
        val memberCount: Int,
        val xp: Int,
        val reliability: Reliability,
    ) : DeckCard
}

/** A match seen from the current user's side: [name] and [contactUrl] belong to the other listing. */
data class Match(
    val id: MatchId,
    val competitionId: CompetitionId,
    val otherListingId: ListingId,
    val otherKind: ListingKind,
    val name: String,
    val avatarUrl: String?,
    val contactUrl: String?,
    val createdAt: Instant,
)

data class Badge(val code: String, val name: String, val description: String, val awardedAt: Instant)

data class ProfileSummary(val profile: Profile, val badges: List<Badge>, val reliability: Reliability)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: ProfileId) : AuthState
}
