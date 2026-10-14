package dev.ether.app.data

import dev.ether.app.domain.AuthState
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.Match
import dev.ether.app.domain.Profile
import dev.ether.app.domain.ProfileDraft
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun sendOtp(email: String)
    suspend fun verifyOtp(email: String, code: String)
    suspend fun signOut()
}

interface ProfileRepository {
    /** Null when the signed-in user has not created a profile yet. */
    suspend fun myProfile(): Profile?
    suspend fun mySummary(): ProfileSummary
    suspend fun saveMyProfile(draft: ProfileDraft)
}

interface CompetitionRepository {
    suspend fun competitions(): List<Competition>
    suspend fun competition(id: CompetitionId): Competition
    /** The listing the user swipes from in this competition: their solo listing or their owned team's. */
    suspend fun myListing(id: CompetitionId): Listing?
    suspend fun joinSolo(id: CompetitionId): Listing
    suspend fun createTeam(id: CompetitionId, name: String, pitch: String, openRoles: List<String>): Listing
}

interface DeckRepository {
    suspend fun deck(from: ListingId, limit: Int = 20): List<DeckCard>
    /** @throws dev.ether.app.domain.SwipeCapReachedException when the daily cap is used up. */
    suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection): SwipeResult
    suspend fun remainingSwipes(): Int
}

interface MatchRepository {
    suspend fun matches(competitionId: CompetitionId): List<Match>
}
