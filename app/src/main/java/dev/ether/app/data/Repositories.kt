package dev.ether.app.data

import dev.ether.app.domain.AuthState
import dev.ether.app.domain.Competition
import dev.ether.app.domain.CompetitionId
import dev.ether.app.domain.DeckCard
import dev.ether.app.domain.Listing
import dev.ether.app.domain.ListingId
import dev.ether.app.domain.Match
import dev.ether.app.domain.MyTeam
import dev.ether.app.domain.ProfileId
import dev.ether.app.domain.TeamId
import dev.ether.app.domain.TeamResult
import dev.ether.app.domain.Profile
import dev.ether.app.domain.ProfileDraft
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.domain.SwipeDirection
import dev.ether.app.domain.SwipeResult
import kotlinx.coroutines.flow.Flow

// Implementations throw dev.ether.app.domain.EtherError for every failure, so callers can show its message.

/** Where creating an account leaves the user: signed in, or waiting on the code mailed to them. */
enum class SignUpResult { SIGNED_IN, VERIFY_EMAIL }

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signIn(email: String, password: String)
    /** Creates the account. Whether a session starts at once depends on the project's "Confirm email" setting. */
    suspend fun signUp(email: String, password: String): SignUpResult
    /** Confirms a new account's email with the 6-digit code from its confirmation mail, which starts the session. */
    suspend fun verifyEmail(email: String, code: String)
    suspend fun resendVerification(email: String)
    /** Mails a 6-digit reset code. Unknown emails succeed silently, so the screen can't be used to probe for accounts. */
    suspend fun sendPasswordReset(email: String)
    /** Proves the reset [code], which starts a session, then sets [newPassword] on that account. */
    suspend fun resetPassword(email: String, code: String, newPassword: String)
    /** Exchanges a Google ID token (from Credential Manager) for a session. [rawNonce] is the unhashed nonce sent to Google. */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String)
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
    suspend fun swipe(from: ListingId, to: ListingId, direction: SwipeDirection): SwipeResult
    suspend fun remainingSwipes(): Int
}

interface MatchRepository {
    suspend fun matches(competitionId: CompetitionId): List<Match>
}

interface TeamRepository {
    suspend fun myTeams(): List<MyTeam>
    suspend fun endorse(competitionId: CompetitionId, teammate: ProfileId)
    suspend fun reportResult(teamId: TeamId, result: TeamResult)
    suspend fun confirmResult(teamId: TeamId)
}
