package dev.ether.app.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.AuthRepository
import dev.ether.app.data.SignUpResult
import dev.ether.app.domain.AuthState
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SignInMode { SIGN_IN, CREATE_ACCOUNT }

/** WELCOME picks Google or email. CREDENTIALS takes email and password. */
enum class SignInStep { WELCOME, CREDENTIALS }

data class SignInUiState(
    val mode: SignInMode = SignInMode.SIGN_IN,
    val step: SignInStep = SignInStep.WELCOME,
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    /** A non-error message, e.g. that a confirmation link was mailed. */
    val notice: String? = null,
) {
    val canSubmit: Boolean
        get() = !busy && step == SignInStep.CREDENTIALS && email.isValidEmail() && when (mode) {
            SignInMode.SIGN_IN -> password.isNotEmpty()
            // A hint only: the auth server enforces its own password policy.
            SignInMode.CREATE_ACCOUNT -> password.length >= MIN_PASSWORD
        }

    companion object {
        const val MIN_PASSWORD = 6
    }
}

private fun String.isValidEmail() = trim().let { '@' in it && it.substringAfter('@').contains('.') }

/** Signing in is complete when the auth flow reports a session; the app root reacts to that, not to this state. */
class SignInViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    init {
        // This ViewModel outlives the session, so start clean for whoever signs in after a sign-out.
        viewModelScope.launch {
            auth.authState.collect { if (it is AuthState.SignedIn) _state.value = SignInUiState() }
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onModeChange(mode: SignInMode) = _state.update { it.copy(mode = mode, error = null, notice = null) }

    fun continueWithEmail() = _state.update { it.copy(step = SignInStep.CREDENTIALS, error = null) }

    fun back() = _state.update { it.copy(step = SignInStep.WELCOME, password = "", error = null, notice = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        val email = current.email.trim()
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            runCatching {
                if (current.mode == SignInMode.SIGN_IN) {
                    auth.signIn(email, current.password)
                    null
                } else {
                    auth.signUp(email, current.password)
                }
            }.onSuccess { result ->
                // A session now exists and the app root moves on, unless the project still wants the email confirmed.
                _state.update {
                    if (result == SignUpResult.VERIFY_EMAIL) {
                        it.copy(busy = false, mode = SignInMode.SIGN_IN, password = "", notice = "Open the link we sent to $email, then sign in.")
                    } else {
                        it.copy(busy = false)
                    }
                }
            }.onFailure { e ->
                _state.update { it.copy(busy = false, error = e.userMessage()) }
            }
        }
    }

    fun signInWithGoogle(idToken: String, rawNonce: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { auth.signInWithGoogle(idToken, rawNonce) }
                .onSuccess { _state.update { it.copy(busy = false) } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.userMessage()) } }
        }
    }

    /** Google's own sheet failed before a token existed (no account, network, misconfiguration). */
    fun googleFailed(message: String) = _state.update { it.copy(busy = false, error = message) }
}
