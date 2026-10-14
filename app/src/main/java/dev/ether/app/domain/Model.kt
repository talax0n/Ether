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
) {
    /** Mirrors the database's profile-completion rule (check_profile_complete): the app gates on it, the DB awards XP for it. */
    val isComplete: Boolean
        get() = name.isNotBlank() && !major.isNullOrBlank() && year != null && bio.isNotBlank() &&
            skills.isNotEmpty() && roles.isNotEmpty() && !contactUrl.isNullOrBlank()
}

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

/**
 * A failure the user can act on, mapped at the data boundary from the backend's stable error strings
 * (supabase/CONTRACT.md). [message] is the text shown to the user.
 */
sealed class EtherError(message: String) : Exception(message) {
    class RegistrationClosed : EtherError("Registration for this competition has closed.")
    class AlreadyInTeam : EtherError("You're already in a team for this competition.")
    class ListingInactive : EtherError("That listing is no longer open. It may have just filled up.")
    class TeamToTeam : EtherError("Teams can only swipe on solo students.")
    class TeamFull : EtherError("That team is already full.")
    class SwipeCapReached : EtherError("You've used all 30 swipes for today. Come back tomorrow.")
    class TeamNameRequired : EtherError("Give your team a name.")
    class EventNotEnded : EtherError("You can do this once the event has ended.")
    class NotTeammates : EtherError("You can only endorse your own teammates.")
    class AlreadyEndorsed : EtherError("You already endorsed this teammate.")
    class TeamFormedAfterDeadline : EtherError("This team formed after registration closed, so it can't report a result.")
    class ResultAlreadyConfirmed : EtherError("Your team's result is already confirmed.")
    class NoResultReported : EtherError("No result has been reported yet.")
    class CannotConfirmOwnResult : EtherError("A teammate has to confirm the result you reported.")
    class EmailDomainNotAllowed(domain: String) : EtherError("Use your university email (@$domain)")
    class InvalidCode : EtherError("That code is wrong or has expired.")
    class Offline : EtherError("Can't reach Ether. Check your connection and try again.")
    class Unknown : EtherError("Something went wrong. Please try again.")
}

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

enum class TeamResult { FINALIST, WINNER }

data class ReportedResult(val result: TeamResult, val reporterName: String, val reportedByMe: Boolean, val confirmed: Boolean)

data class Teammate(val id: ProfileId, val name: String, val contactUrl: String?, val endorsedByMe: Boolean)

/** A team the signed-in user belongs to. [teammates] excludes the user. */
data class MyTeam(
    val id: TeamId,
    val name: String,
    val competition: Competition,
    val ownerId: ProfileId,
    val teammates: List<Teammate>,
    val result: ReportedResult?,
)

data class Badge(val code: String, val name: String, val description: String, val awardedAt: Instant)

data class ProfileSummary(val profile: Profile, val badges: List<Badge>, val reliability: Reliability)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: ProfileId) : AuthState
}
