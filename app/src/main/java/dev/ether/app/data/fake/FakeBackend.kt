package dev.ether.app.data.fake

import dev.ether.app.data.AuthRepository
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.domain.AuthState
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
import dev.ether.app.domain.ProfileDraft
import dev.ether.app.domain.ProfileId
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.domain.Reliability
import dev.ether.app.domain.SwipeCapReachedException
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import dev.ether.app.domain.TeamId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * In-memory stand-in for the whole backend, for unit tests and for running the app before the
 * database exists. It mimics the DB rules loosely (cap, mutual match); Postgres stays the real authority.
 */
class FakeBackend(
    signedIn: Boolean = false,
    private var profile: Profile? = DemoData.me,
    private var remaining: Int = 30,
    private val clock: () -> Instant = { DemoData.now },
) : AuthRepository, ProfileRepository, CompetitionRepository, DeckRepository, MatchRepository {

    private val auth = MutableStateFlow<AuthState>(if (signedIn) AuthState.SignedIn(DemoData.me.id) else AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = auth

    private val listings = DemoData.myListings.toMutableList()
    private val deck = DemoData.deck.toMutableList()
    private val swiped = mutableSetOf<Pair<ListingId, ListingId>>()
    private val matches = mutableListOf<Match>()
    private var nextId = 1

    override suspend fun sendOtp(email: String) {
        require(email.contains('@')) { "Enter a valid email" }
    }

    override suspend fun verifyOtp(email: String, code: String) {
        if (code != DemoData.OTP) throw IllegalArgumentException("Invalid or expired code")
        auth.value = AuthState.SignedIn(DemoData.me.id)
    }

    override suspend fun signOut() {
        auth.value = AuthState.SignedOut
    }

    override suspend fun myProfile(): Profile? = profile

    override suspend fun mySummary(): ProfileSummary =
        ProfileSummary(checkNotNull(profile), DemoData.badges, Reliability(positive = 4, total = 5))

    override suspend fun saveMyProfile(draft: ProfileDraft) {
        val base = profile ?: DemoData.me.copy(xp = 0)
        profile = base.copy(
            name = draft.name, major = draft.major, year = draft.year, bio = draft.bio,
            skills = draft.skills, roles = draft.roles, contactUrl = draft.contactUrl,
        )
    }

    override suspend fun competitions(): List<Competition> = DemoData.competitions

    override suspend fun competition(id: CompetitionId): Competition =
        DemoData.competitions.first { it.id == id }

    override suspend fun myListing(id: CompetitionId): Listing? = listings.firstOrNull { it.competitionId == id }

    override suspend fun joinSolo(id: CompetitionId): Listing =
        Listing.Solo(ListingId("l-fake-${nextId++}"), id, active = true, profileId = DemoData.me.id)
            .also(listings::add)

    override suspend fun createTeam(id: CompetitionId, name: String, pitch: String, openRoles: List<String>): Listing =
        Listing.Team(ListingId("l-fake-${nextId++}"), id, active = true, teamId = TeamId("t-fake-${nextId++}"))
            .also(listings::add)

    override suspend fun deck(from: ListingId, limit: Int): List<DeckCard> =
        deck.filter { (from to it.listingId) !in swiped }.take(limit)

    override suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection): SwipeResult {
        if (remaining <= 0) throw SwipeCapReachedException()
        remaining--
        swiped += from to to
        val card = deck.first { it.listingId == to }
        val contact = DemoData.likesYouBack[to]
        if (direction == SwipeDirection.LEFT || contact == null) return SwipeResult(false, null, remaining)
        val competitionId = listings.first { it.id == from }.competitionId
        val match = Match(
            id = MatchId("m-${nextId++}"),
            competitionId = competitionId,
            otherListingId = to,
            otherKind = if (card is DeckCard.Team) ListingKind.TEAM else ListingKind.SOLO,
            name = card.name,
            avatarUrl = card.avatarUrl,
            contactUrl = contact,
            createdAt = clock(),
        )
        matches += match
        return SwipeResult(true, match.id, remaining)
    }

    override suspend fun remainingSwipes(): Int = remaining

    override suspend fun matches(competitionId: CompetitionId): List<Match> =
        matches.filter { it.competitionId == competitionId }.sortedByDescending { it.createdAt }
}

object DemoData {
    const val OTP = "123456"
    val now: Instant = Instant.parse("2026-10-15T09:00:00Z")

    val me = Profile(
        id = ProfileId("p-me"),
        name = "Theo",
        major = "Computer Science",
        year = 3,
        bio = "Android dev who likes shipping fast.",
        skills = listOf("Kotlin", "Compose", "Postgres"),
        roles = listOf("Mobile"),
        contactUrl = "https://t.me/theo",
        avatarUrl = null,
        xp = 225,
    )

    val gemastik = CompetitionId("c-gemastik")
    val campusHack = CompetitionId("c-campus-hack")

    val competitions = listOf(
        Competition(
            gemastik, "GEMASTIK 2026", "National ICT competition. Software dev, UX, and data mining tracks.",
            "https://gemastik.kemdikbud.go.id", Instant.parse("2026-11-01T16:59:00Z"),
            Instant.parse("2026-12-10T10:00:00Z"), minTeamSize = 2, maxTeamSize = 3,
        ),
        Competition(
            campusHack, "Campus Hackathon", "48-hour build sprint on campus. Theme announced at kickoff.",
            null, Instant.parse("2026-11-20T16:59:00Z"), Instant.parse("2026-11-29T18:00:00Z"),
            minTeamSize = 3, maxTeamSize = 5,
        ),
    )

    val myListings: List<Listing> = listOf(
        Listing.Solo(ListingId("l-me-gemastik"), gemastik, active = true, profileId = me.id),
    )

    val deck: List<DeckCard> = listOf(
        DeckCard.Solo(
            ListingId("l-ana"), "Ana Putri", null, "Information Systems", 2,
            "UI/UX designer. Figma all day, won a campus design jam last spring.",
            listOf("Figma", "User research", "Prototyping"), listOf("Designer"), xp = 310,
            reliability = Reliability(5, 5),
        ),
        DeckCard.Team(
            ListingId("l-byte-brigade"), "Byte Brigade", null,
            "Building an offline-first clinic queue app. Backend is done, need someone on mobile.",
            listOf("Mobile", "Designer"), memberCount = 2, maxTeamSize = 3,
        ),
        DeckCard.Solo(
            ListingId("l-budi"), "Budi Santoso", null, "Electrical Engineering", 4,
            "Embedded and data person. Can wire a sensor and plot what it says.",
            listOf("Python", "Pandas", "Arduino"), listOf("Data", "Hardware"), xp = 120,
            reliability = Reliability(2, 3),
        ),
        DeckCard.Solo(
            ListingId("l-citra"), "Citra Lestari", null, "Computer Science", 1,
            "First hackathon. Fast learner, solid at React.",
            listOf("React", "TypeScript"), listOf("Frontend"), xp = 0,
            reliability = Reliability(0, 0),
        ),
        DeckCard.Team(
            ListingId("l-pixel"), "Pixel Pushers", null,
            "Gamified recycling tracker. We pitch well, we need a backend.",
            listOf("Backend"), memberCount = 1, maxTeamSize = 3,
        ),
    )

    /** Listings that already swiped right on the demo user, mapped to the contact revealed on match. */
    val likesYouBack = mapOf(
        ListingId("l-ana") to "https://wa.me/6281234567890",
        ListingId("l-byte-brigade") to "https://t.me/bytebrigade",
    )

    val badges = listOf(
        Badge("first_team", "First Team", "Formed or joined your first team", Instant.parse("2026-03-02T10:00:00Z")),
    )
}
