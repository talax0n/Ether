package dev.ether.app.data.supabase

import dev.ether.app.data.AuthRepository
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.data.TeamRepository
import dev.ether.app.domain.AuthState
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.Match
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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {
    override val authState: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Initializing -> AuthState.Loading
            is SessionStatus.Authenticated ->
                status.session.user?.id?.let { AuthState.SignedIn(ProfileId(it)) } ?: AuthState.SignedOut
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            // A failed refresh keeps the stored session; stay signed in and let calls retry.
            is SessionStatus.RefreshFailure ->
                client.auth.currentUserOrNull()?.id?.let { AuthState.SignedIn(ProfileId(it)) } ?: AuthState.SignedOut
        }
    }

    override suspend fun sendOtp(email: String): Unit = backend {
        client.auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    override suspend fun verifyOtp(email: String, code: String): Unit = backend {
        client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    override suspend fun signOut(): Unit = backend {
        client.auth.signOut()
    }
}

private fun SupabaseClient.uid(): String = checkNotNull(auth.currentUserOrNull()?.id) { "Not signed in" }

class SupabaseProfileRepository(private val client: SupabaseClient) : ProfileRepository {
    override suspend fun myProfile(): Profile? = backend {
        val uid = client.uid()
        val row = client.from("profiles").select { filter { eq("id", uid) } }.decodeSingleOrNull<ProfileDto>()
            ?: return@backend null
        val contact = client.from("profile_contacts").select { filter { eq("profile_id", uid) } }
            .decodeSingleOrNull<ProfileContactDto>()
        row.toDomain(contact?.contactUrl)
    }

    override suspend fun mySummary(): ProfileSummary = backend {
        val uid = client.uid()
        val profile = checkNotNull(myProfile()) { "Profile not created" }
        val badges = client.from("profile_badges")
            .select(Columns.raw("awarded_at, badges(code, name, description)")) { filter { eq("profile_id", uid) } }
            .decodeList<ProfileBadgeDto>()
            .map { it.toDomain() }
        val score = client.postgrest.rpc("reliability_score", buildJsonObject { put("p_profile", uid) }).decodeAs<Int>()
        ProfileSummary(profile, badges, Reliability(score))
    }

    // The sign-up trigger creates the profiles row and clients may only UPDATE it.
    override suspend fun saveMyProfile(draft: ProfileDraft): Unit = backend {
        val uid = client.uid()
        client.from("profiles").update(
            ProfileUpdateDto(draft.name, draft.major, draft.year, draft.bio, draft.skills, draft.roles),
        ) { filter { eq("id", uid) } }
        // No upsert: on conflict PostgREST also rewrites profile_id, and only contact_url is granted for UPDATE.
        val updated = client.from("profile_contacts").update({ set("contact_url", draft.contactUrl) }) {
            filter { eq("profile_id", uid) }
            select()
        }.decodeList<ProfileContactDto>()
        if (updated.isEmpty()) client.from("profile_contacts").insert(ProfileContactDto(uid, draft.contactUrl))
    }
}

class SupabaseCompetitionRepository(private val client: SupabaseClient) : CompetitionRepository {
    override suspend fun competitions(): List<Competition> = backend {
        client.from("competitions").select { order("registration_deadline", Order.ASCENDING) }
            .decodeList<CompetitionDto>().map { it.toDomain() }
    }

    override suspend fun competition(id: CompetitionId): Competition = backend {
        client.from("competitions").select { filter { eq("id", id.value) } }.decodeSingle<CompetitionDto>().toDomain()
    }

    // After a SOLO↔SOLO match the user keeps an inactive solo listing and owns the new team's listing,
    // so the active one wins.
    override suspend fun myListing(id: CompetitionId): Listing? = backend {
        val uid = client.uid()
        val solo = client.from("listings").select { filter { eq("competition_id", id.value); eq("profile_id", uid) } }
            .decodeSingleOrNull<ListingDto>()?.toDomain()
        val team = client.from("teams").select(Columns.list("id")) {
            filter { eq("competition_id", id.value); eq("owner_id", uid) }
        }.decodeSingleOrNull<IdDto>()?.let { owned ->
            client.from("listings").select { filter { eq("team_id", owned.id) } }.decodeSingleOrNull<ListingDto>()?.toDomain()
        }
        listOfNotNull(team, solo).firstOrNull { it.active } ?: solo ?: team
    }

    override suspend fun joinSolo(id: CompetitionId): Listing = backend {
        client.from("listings").insert(SoloListingInsertDto(id.value, client.uid(), ListingKindDto.SOLO)) { select() }
            .decodeSingle<ListingDto>().toDomain()
    }

    override suspend fun createTeam(id: CompetitionId, name: String, pitch: String, openRoles: List<String>): Listing = backend {
        val teamId = client.postgrest.rpc("create_team", buildJsonObject {
            put("p_competition", id.value)
            put("p_name", name)
            put("p_pitch", pitch)
            putJsonArray("p_open_roles") { openRoles.forEach { add(it) } }
        }).decodeAs<String>()
        client.from("listings").select { filter { eq("team_id", teamId) } }.decodeSingle<ListingDto>().toDomain()
    }
}

class SupabaseDeckRepository(private val client: SupabaseClient) : DeckRepository {
    override suspend fun deck(from: ListingId, limit: Int): List<DeckCard> = backend {
        client.postgrest.rpc("deck", buildJsonObject {
            put("p_listing", from.value)
            put("p_limit", limit)
        }).decodeList<DeckRowDto>().map { it.toDomain() }
    }

    override suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection): SwipeResult = backend {
        client.postgrest.rpc("swipe", buildJsonObject {
            put("p_from", from.value)
            put("p_to", to.value)
            put("p_direction", direction.wire)
        }).decodeAs<SwipeResultDto>().toDomain()
    }

    override suspend fun remainingSwipes(): Int = backend {
        client.postgrest.rpc("remaining_swipes").decodeAs<Int>()
    }
}

class SupabaseMatchRepository(private val client: SupabaseClient) : MatchRepository {
    override suspend fun matches(competitionId: CompetitionId): List<Match> = backend {
        client.postgrest.rpc("my_matches", buildJsonObject { put("p_competition", competitionId.value) })
            .decodeList<MatchRowDto>().map { it.toDomain(competitionId) }
    }
}

class SupabaseTeamRepository(private val client: SupabaseClient) : TeamRepository {
    override suspend fun myTeams(): List<MyTeam> = backend {
        val uid = client.uid()
        val teamIds = client.from("team_members").select(Columns.list("team_id")) { filter { eq("profile_id", uid) } }
            .decodeList<TeamIdDto>().map { it.teamId }
        if (teamIds.isEmpty()) return@backend emptyList()
        coroutineScope {
            val teams = async {
                client.from("teams")
                    .select(Columns.raw("id, name, owner_id, competitions(*), team_members(profile_id, profiles(name))")) {
                        filter { isIn("id", teamIds) }
                    }.decodeList<TeamRowDto>()
            }
            // RLS returns only the contacts of people who share a team or a match with the caller.
            val contacts = async {
                client.from("profile_contacts").select().decodeList<ProfileContactDto>()
                    .associate { it.profileId to it.contactUrl }
            }
            val endorsed = async {
                client.from("endorsements").select(Columns.list("to_profile_id", "competition_id")) {
                    filter { eq("from_profile_id", uid) }
                }.decodeList<EndorsementKeyDto>().toSet()
            }
            val results = async {
                client.from("team_results").select { filter { isIn("team_id", teamIds) } }
                    .decodeList<TeamResultRowDto>().associateBy { it.teamId }
            }
            teams.await().map { t ->
                MyTeam(
                    id = TeamId(t.id),
                    name = t.name,
                    competition = t.competitions.toDomain(),
                    ownerId = ProfileId(t.ownerId),
                    teammates = t.members.filter { it.profileId != uid }.map { m ->
                        Teammate(
                            ProfileId(m.profileId), m.profiles.name, contacts.await()[m.profileId],
                            endorsedByMe = EndorsementKeyDto(m.profileId, t.competitions.id) in endorsed.await(),
                        )
                    }.sortedBy { it.name },
                    result = results.await()[t.id]?.let { r ->
                        ReportedResult(
                            r.result.toDomain(),
                            reporterName = t.members.firstOrNull { it.profileId == r.reportedBy }?.profiles?.name.orEmpty(),
                            reportedByMe = r.reportedBy == uid,
                            confirmed = r.confirmedBy != null,
                        )
                    },
                )
            }.sortedByDescending { it.competition.eventEnd }
        }
    }

    override suspend fun endorse(competitionId: CompetitionId, teammate: ProfileId): Unit = backend {
        client.from("endorsements").insert(EndorsementInsertDto(client.uid(), teammate.value, competitionId.value, positive = true))
    }

    override suspend fun reportResult(teamId: TeamId, result: TeamResult): Unit = backend {
        client.postgrest.rpc("report_result", buildJsonObject {
            put("p_team", teamId.value)
            put("p_result", result.wire)
        })
    }

    override suspend fun confirmResult(teamId: TeamId): Unit = backend {
        client.postgrest.rpc("confirm_result", buildJsonObject { put("p_team", teamId.value) })
    }
}
