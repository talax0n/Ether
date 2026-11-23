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
    val contacts: Contacts,
    val avatarUrl: String?,
    val xp: Int,
    val face: AvatarFace? = null,
) {
    /** Mirrors the database's profile-completion rule (check_profile_complete): the app gates on it, the DB awards XP for it. */
    val isComplete: Boolean
        get() = name.isNotBlank() && !major.isNullOrBlank() && year != null && bio.isNotBlank() &&
            skills.isNotEmpty() && roles.isNotEmpty() && !contacts.isEmpty
}

/** What the user can edit about themselves. XP and id are owned by the database. */
data class ProfileDraft(
    val name: String,
    val major: String,
    val year: Int,
    val bio: String,
    val skills: List<String>,
    val roles: List<String>,
    val contacts: Contacts,
    val face: AvatarFace? = null,
)

/** Illustrated faces a user can pick as their picture; it shows instead of a photo or initials. */
enum class AvatarFace { NADIA, BUDI, CITRA, DIMAS, EKA, FAJAR }

data class Competition(
    val id: CompetitionId,
    val name: String,
    val description: String,
    val url: String?,
    val registrationDeadline: Instant,
    val eventEnd: Instant,
    val minTeamSize: Int,
    val maxTeamSize: Int,
    val organizer: String? = null,
    /** Free text as entered, e.g. "Rp 50.000.000" or "$10,000 + swag". */
    val prize: String? = null,
    val mode: CompetitionMode = CompetitionMode.ONLINE,
    /** Venue city for offline and hybrid events; always null when online. */
    val city: String? = null,
    val tags: List<String> = emptyList(),
)

enum class CompetitionMode { ONLINE, OFFLINE, HYBRID }

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
    class HackathonLimitReached : EtherError("You can list up to 20 past hackathons. Remove one to add another.")
    class ProjectLimitReached : EtherError("You can list up to 20 projects. Remove one to add another.")
    class InvalidTrackRecord : EtherError("Something in that entry isn't accepted. Check the year and the link.")
    class WrongCredentials : EtherError("That email and password don't match. New here? Create an account.")
    class AccountExists : EtherError("There's already an account with that email. Sign in instead.")
    class WeakPassword : EtherError("Pick a stronger password: at least 6 characters.")
    class EmailNotConfirmed : EtherError("Confirm your email first: open the link we sent you, then sign in.")
    class TooManyEmails : EtherError("Too many emails sent. Wait a few minutes and try again.")
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
    val trackRecord: TrackRecordSummary
    val face: AvatarFace?

    data class Solo(
        override val listingId: ListingId,
        override val name: String,
        override val avatarUrl: String?,
        val headline: String,
        val skills: List<String>,
        val roles: List<String>,
        val xp: Int,
        val reliability: Reliability,
        override val trackRecord: TrackRecordSummary = TrackRecordSummary.NONE,
        override val face: AvatarFace? = null,
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
        override val trackRecord: TrackRecordSummary = TrackRecordSummary.NONE,
        override val face: AvatarFace? = null,
    ) : DeckCard
}

/** A match seen from the current user's side: [name] and [contacts] belong to the other listing. */
data class Match(
    val id: MatchId,
    val competitionId: CompetitionId,
    val otherListingId: ListingId,
    val otherKind: ListingKind,
    val name: String,
    val avatarUrl: String?,
    val contacts: Contacts,
    val createdAt: Instant,
    val face: AvatarFace? = null,
)

enum class TeamResult { FINALIST, WINNER }

data class ReportedResult(val result: TeamResult, val reporterName: String, val reportedByMe: Boolean, val confirmed: Boolean)

data class Teammate(val id: ProfileId, val name: String, val contacts: Contacts, val endorsedByMe: Boolean)

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
