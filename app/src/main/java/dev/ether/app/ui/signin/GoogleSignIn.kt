package dev.ether.app.ui.signin

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/** What Google's account picker produced. */
sealed interface GoogleResult {
    data class Token(val idToken: String, val rawNonce: String) : GoogleResult
    /** The user closed the sheet. Not an error. */
    data object Cancelled : GoogleResult
    data class Failed(val message: String) : GoogleResult
}

/**
 * Shows Google's account picker via Credential Manager and returns an ID token for Supabase. Google receives
 * the SHA-256 of a fresh nonce and Supabase gets the raw value, so a replayed token is rejected.
 */
suspend fun pickGoogleAccount(context: Context, webClientId: String): GoogleResult {
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).setNonce(hashedNonce).build())
        .build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        GoogleResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken, rawNonce)
    } catch (e: GetCredentialCancellationException) {
        GoogleResult.Cancelled
    } catch (e: NoCredentialException) {
        GoogleResult.Failed("No Google account on this phone. Add one in Settings, or use your email.")
    } catch (e: GetCredentialException) {
        GoogleResult.Failed("Google sign-in didn't work. Try again, or use your email.")
    }
}
