package dev.ether.app.data.fake

import dev.ether.app.data.AuthRepository
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.data.TeamRepository
import dev.ether.app.domain.AuthState
import dev.ether.app.domain.Badge
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.EtherError
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.ListingKind
import dev.ether.app.domain.Match
import dev.ether.app.domain.MatchId
import dev.ether.app.domain.MyTeam
import dev.ether.app.domain.Profile
import dev.ether.app.domain.ProfileDraft
import dev.ether.app.domain.ProfileId
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.domain.Reliability
import dev.ether.app.domain.ReportedResult
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import dev.ether.app.domain.TeamId
import dev.ether.app.domain.TeamResult
import dev.ether.app.domain.Teammate
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
    /** Simulates every competition being past its registration deadline. */
    private val registrationClosed: Boolean = false,
    private val clock: () -> Instant = { DemoData.now },
) : AuthRepository, ProfileRepository, CompetitionRepository, DeckRepository, MatchRepository, TeamRepository {

    private val auth = MutableStateFlow<AuthState>(if (signedIn) AuthState.SignedIn(DemoData.me.id) else AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = auth

    private val listings = DemoData.myListings.toMutableList()
    private val deck = DemoData.deck.toMutableList()
    private val swiped = mutableSetOf<Pair<ListingId, ListingId>>()
    private val inactive = mutableSetOf<ListingId>()
    private val matches = mutableListOf<Match>()
    private val teams = DemoData.myTeams.toMutableList()
    private var nextId = 1

    override suspend fun sendOtp(email: String) {
        require(email.contains('@')) { "Enter a valid email" }
    }

    override suspend fun verifyOtp(email: String, code: String) {
        if (code != DemoData.OTP) throw EtherError.InvalidCode()
        auth.value = AuthState.SignedIn(DemoData.me.id)
    }

    override suspend fun signOut() {
        auth.value = AuthState.SignedOut
    }

    override suspend fun myProfile(): Profile? = profile

    override suspend fun mySummary(): ProfileSummary =
        ProfileSummary(checkNotNull(profile), DemoData.badges, Reliability(4))

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
        if (registrationClosed) throw EtherError.RegistrationClosed()
        else Listing.Solo(ListingId("l-fake-${nextId++}"), id, active = true, profileId = DemoData.me.id)
            .also(listings::add)

    override suspend fun createTeam(id: CompetitionId, name: String, pitch: String, openRoles: List<String>): Listing =
        if (registrationClosed) throw EtherError.RegistrationClosed()
        else Listing.Team(ListingId("l-fake-${nextId++}"), id, active = true, teamId = TeamId("t-fake-${nextId++}"))
            .also(listings::add)

    override suspend fun deck(from: ListingId, limit: Int): List<DeckCard> =
        if (from in inactive) throw EtherError.ListingInactive()
        else deck.filter { (from to it.listingId) !in swiped }.take(limit)

    override suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection): SwipeResult {
        if (registrationClosed) throw EtherError.RegistrationClosed()
        if (from in inactive) throw EtherError.ListingInactive()
        if (remaining <= 0) throw EtherError.SwipeCapReached()
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
        // Like the DB: a match puts this solo on a team, which closes their solo listing.
        if (listings.first { it.id == from } is Listing.Solo) inactive += from
        return SwipeResult(true, match.id, remaining)
    }

    override suspend fun remainingSwipes(): Int = remaining

    override suspend fun matches(competitionId: CompetitionId): List<Match> =
        matches.filter { it.competitionId == competitionId }.sortedByDescending { it.createdAt }

    override suspend fun myTeams(): List<MyTeam> = teams.toList()

    override suspend fun endorse(competitionId: CompetitionId, teammate: ProfileId) {
        val team = teams.first { it.competition.id == competitionId }
        if (team.competition.eventEnd > clock()) throw EtherError.EventNotEnded()
        val mate = team.teammates.firstOrNull { it.id == teammate } ?: throw EtherError.NotTeammates()
        if (mate.endorsedByMe) throw EtherError.AlreadyEndorsed()
        updateTeam(team.id) { t -> t.copy(teammates = t.teammates.map { if (it.id == teammate) it.copy(endorsedByMe = true) else it }) }
    }

    override suspend fun reportResult(teamId: TeamId, result: TeamResult) {
        val team = teams.first { it.id == teamId }
        if (team.competition.eventEnd > clock()) throw EtherError.EventNotEnded()
        if (team.result?.confirmed == true) throw EtherError.ResultAlreadyConfirmed()
        updateTeam(teamId) { it.copy(result = ReportedResult(result, DemoData.me.name, reportedByMe = true, confirmed = false)) }
    }

    override suspend fun confirmResult(teamId: TeamId) {
        val reported = teams.first { it.id == teamId }.result ?: throw EtherError.NoResultReported()
        if (reported.reportedByMe) throw EtherError.CannotConfirmOwnResult()
        updateTeam(teamId) { it.copy(result = reported.copy(confirmed = true)) }
    }

    private fun updateTeam(id: TeamId, transform: (MyTeam) -> MyTeam) {
        teams.replaceAll { if (it.id == id) transform(it) else it }
    }
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
    val dataChallenge = CompetitionId("c-data-challenge")

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
        Competition(
            dataChallenge, "Data Challenge 2026", "Finished data science challenge.",
            null, Instant.parse("2026-09-15T16:59:00Z"), Instant.parse("2026-10-08T18:00:00Z"),
            minTeamSize = 2, maxTeamSize = 3,
        ),
    )

    /** The demo user's team in the finished Data Challenge: ready to endorse and report a result. */
    val myTeams = listOf(
        MyTeam(
            id = TeamId("t-night-owls"),
            name = "Night Owls",
            competition = competitions.last(),
            ownerId = me.id,
            teammates = listOf(
                Teammate(ProfileId("p-ana"), "Ana Putri", "https://wa.me/6281234567890", endorsedByMe = false),
                Teammate(ProfileId("p-budi"), "Budi Santoso", "https://t.me/budi", endorsedByMe = false),
            ),
            result = ReportedResult(TeamResult.FINALIST, "Ana Putri", reportedByMe = false, confirmed = false),
        ),
    )

    val myListings: List<Listing> = listOf(
        Listing.Solo(ListingId("l-me-gemastik"), gemastik, active = true, profileId = me.id),
    )

    val deck: List<DeckCard> = listOf(
        DeckCard.Solo(
            ListingId("l-ana"), "Ana Putri", null,
            "UI/UX designer. Figma all day, won a campus design jam last spring.",
            listOf("Figma", "User research", "Prototyping"), listOf("Designer"), xp = 310,
            reliability = Reliability(5),
        ),
        DeckCard.Team(
            ListingId("l-byte-brigade"), "Byte Brigade", null,
            "Building an offline-first clinic queue app. Backend is done, need someone on mobile.",
            listOf("Mobile", "Designer"), memberCount = 2, xp = 340, reliability = Reliability(3),
        ),
        DeckCard.Solo(
            ListingId("l-budi"), "Budi Santoso", null,
            "Embedded and data person. Can wire a sensor and plot what it says.",
            listOf("Python", "Pandas", "Arduino"), listOf("Data", "Hardware"), xp = 120,
            reliability = Reliability(2),
        ),
        DeckCard.Solo(
            ListingId("l-citra"), "Citra Lestari", null,
            "First hackathon. Fast learner, solid at React.",
            listOf("React", "TypeScript"), listOf("Frontend"), xp = 0,
            reliability = Reliability(0),
        ),
        DeckCard.Team(
            ListingId("l-pixel"), "Pixel Pushers", null,
            "Gamified recycling tracker. We pitch well, we need a backend.",
            listOf("Backend"), memberCount = 1, xp = 60, reliability = Reliability(0),
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
