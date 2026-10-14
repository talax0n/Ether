package dev.ether.app.data.supabase

import dev.ether.app.data.AuthRepository
import dev.ether.app.data.CompetitionRepository
import dev.ether.app.data.DeckRepository
import dev.ether.app.data.MatchRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.domain.AuthState
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.Match
import dev.ether.app.domain.Profile
import dev.ether.app.domain.ProfileDraft
import dev.ether.app.domain.ProfileId
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.domain.Reliability
import dev.ether.app.domain.SwipeCapReachedException
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

    override suspend fun sendOtp(email: String) {
        client.auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    override suspend fun verifyOtp(email: String, code: String) {
        client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }
}

private fun SupabaseClient.uid(): String = checkNotNull(auth.currentUserOrNull()?.id) { "Not signed in" }

class SupabaseProfileRepository(private val client: SupabaseClient) : ProfileRepository {
    override suspend fun myProfile(): Profile? {
        val uid = client.uid()
        val row = client.from("profiles").select { filter { eq("id", uid) } }.decodeSingleOrNull<ProfileDto>()
            ?: return null
        val contact = client.from("profile_contacts").select { filter { eq("profile_id", uid) } }
            .decodeSingleOrNull<ProfileContactDto>()
        return row.toDomain(contact?.contactUrl)
    }

    override suspend fun mySummary(): ProfileSummary {
        val uid = client.uid()
        val profile = checkNotNull(myProfile()) { "Profile not created" }
        val badges = client.from("profile_badges")
            .select(Columns.raw("awarded_at, badges(code, name, description)")) { filter { eq("profile_id", uid) } }
            .decodeList<ProfileBadgeDto>()
            .map { it.toDomain() }
        val endorsements = client.from("endorsements")
            .select(Columns.list("positive")) { filter { eq("to_profile_id", uid) } }
            .decodeList<EndorsementDto>()
        return ProfileSummary(profile, badges, Reliability(endorsements.count { it.positive }, endorsements.size))
    }

    override suspend fun saveMyProfile(draft: ProfileDraft) {
        val uid = client.uid()
        client.from("profiles").upsert(
            ProfileUpsertDto(uid, draft.name, draft.major, draft.year, draft.bio, draft.skills, draft.roles),
        )
        client.from("profile_contacts").upsert(ProfileContactDto(uid, draft.contactUrl))
    }
}

class SupabaseCompetitionRepository(private val client: SupabaseClient) : CompetitionRepository {
    override suspend fun competitions(): List<Competition> =
        client.from("competitions").select { order("registration_deadline", Order.ASCENDING) }
            .decodeList<CompetitionDto>().map { it.toDomain() }

    override suspend fun competition(id: CompetitionId): Competition =
        client.from("competitions").select { filter { eq("id", id.value) } }.decodeSingle<CompetitionDto>().toDomain()

    override suspend fun myListing(id: CompetitionId): Listing? {
        val uid = client.uid()
        client.from("listings").select { filter { eq("competition_id", id.value); eq("profile_id", uid) } }
            .decodeSingleOrNull<ListingDto>()?.let { return it.toDomain() }
        val ownedTeam = client.from("teams").select(Columns.list("id")) {
            filter { eq("competition_id", id.value); eq("owner_id", uid) }
        }.decodeSingleOrNull<IdDto>() ?: return null
        return client.from("listings").select { filter { eq("team_id", ownedTeam.id) } }
            .decodeSingleOrNull<ListingDto>()?.toDomain()
    }

    override suspend fun joinSolo(id: CompetitionId): Listing =
        client.from("listings").insert(ListingInsertDto(id.value, ListingKindDto.SOLO, profileId = client.uid())) { select() }
            .decodeSingle<ListingDto>().toDomain()

    override suspend fun createTeam(id: CompetitionId, name: String, pitch: String, openRoles: List<String>): Listing {
        val uid = client.uid()
        val team = client.from("teams").insert(TeamInsertDto(id.value, uid, name, pitch, openRoles)) { select(Columns.list("id")) }
            .decodeSingle<IdDto>()
        client.from("team_members").insert(TeamMemberDto(team.id, uid))
        return client.from("listings").insert(ListingInsertDto(id.value, ListingKindDto.TEAM, teamId = team.id)) { select() }
            .decodeSingle<ListingDto>().toDomain()
    }
}

class SupabaseDeckRepository(private val client: SupabaseClient) : DeckRepository {
    override suspend fun deck(from: ListingId, limit: Int): List<DeckCard> =
        client.postgrest.rpc("deck", buildJsonObject {
            put("p_listing", from.value)
            put("p_limit", limit)
        }).decodeList<DeckRowDto>().map { it.toDomain() }

    override suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection): SwipeResult =
        try {
            client.postgrest.rpc("swipe", buildJsonObject {
                put("p_from", from.value)
                put("p_to", to.value)
                put("p_direction", direction.wire)
            }).decodeAs<SwipeResultDto>().toDomain()
        } catch (e: PostgrestRestException) {
            // The cap error's exact shape is up to the DB; asking for the count avoids parsing its message.
            if (remainingSwipes() <= 0) throw SwipeCapReachedException() else throw e
        }

    override suspend fun remainingSwipes(): Int =
        client.postgrest.rpc("remaining_swipes").decodeAs<Int>()
}

class SupabaseMatchRepository(private val client: SupabaseClient) : MatchRepository {
    override suspend fun matches(competitionId: CompetitionId): List<Match> =
        client.postgrest.rpc("my_matches", buildJsonObject { put("p_competition", competitionId.value) })
            .decodeList<MatchRowDto>().map { it.toDomain() }
}
