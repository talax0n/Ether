package dev.ether.app.data.supabase

import android.util.Log
import dev.ether.app.BuildConfig
import dev.ether.app.domain.EtherError
import java.io.IOException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException

/**
 * Maps the text of a backend error to a domain error. Postgres raises stable snake_case strings
 * (supabase/CONTRACT.md); GoTrue hides the sign-up trigger's error behind a generic 500.
 */
internal fun etherErrorFor(text: String, emailDomain: String = BuildConfig.ALLOWED_EMAIL_DOMAIN): EtherError? = when {
    "registration_closed" in text -> EtherError.RegistrationClosed()
    "already_in_team" in text -> EtherError.AlreadyInTeam()
    "listing_inactive" in text -> EtherError.ListingInactive()
    "team_to_team" in text -> EtherError.TeamToTeam()
    "team_full" in text -> EtherError.TeamFull()
    "swipe_cap_reached" in text -> EtherError.SwipeCapReached()
    "team_name_required" in text -> EtherError.TeamNameRequired()
    "event_not_ended" in text -> EtherError.EventNotEnded()
    "not_teammates" in text -> EtherError.NotTeammates()
    "duplicate key" in text && "endorsements" in text -> EtherError.AlreadyEndorsed()
    "team_formed_after_deadline" in text -> EtherError.TeamFormedAfterDeadline()
    "result_already_confirmed" in text -> EtherError.ResultAlreadyConfirmed()
    "no_result_reported" in text -> EtherError.NoResultReported()
    "cannot_confirm_own_result" in text -> EtherError.CannotConfirmOwnResult()
    "email_domain_not_allowed" in text || "Database error saving new user" in text ->
        EtherError.EmailDomainNotAllowed(emailDomain)
    "otp_expired" in text || "Token has expired or is invalid" in text -> EtherError.InvalidCode()
    else -> null
}

internal fun Throwable.toEtherError(): EtherError = when (this) {
    is EtherError -> this
    is RestException -> etherErrorFor(listOfNotNull(error, description, message).joinToString("\n"))
    is IOException -> EtherError.Offline()
    else -> null
} ?: EtherError.Unknown().also { Log.w("Ether", "Unmapped backend error", this) }

/** Runs a backend call, rethrowing anything it throws as an [EtherError]. */
internal suspend inline fun <T> backend(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw e.toEtherError()
}
