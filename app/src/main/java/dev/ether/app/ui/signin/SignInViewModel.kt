package dev.ether.app.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.AuthRepository
import dev.ether.app.data.SignUpResult
import dev.ether.app.domain.EtherError
import dev.ether.app.domain.AuthState
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SignInMode { SIGN_IN, CREATE_ACCOUNT }

/** CREDENTIALS takes email and password; VERIFY takes the code that confirms a new account's email. */
enum class SignInStep { CREDENTIALS, VERIFY }

data class SignInUiState(
    val mode: SignInMode = SignInMode.SIGN_IN,
    val step: SignInStep = SignInStep.CREDENTIALS,
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    /** Seconds until "Resend code" is allowed again; 0 means it can be tapped. */
    val resendIn: Int = 0,
) {
    val canSubmit: Boolean
        get() = !busy && when (step) {
            SignInStep.CREDENTIALS -> email.isValidEmail() && when (mode) {
                SignInMode.SIGN_IN -> password.isNotEmpty()
                // A hint only: the auth server enforces its own password policy.
                SignInMode.CREATE_ACCOUNT -> password.length >= MIN_PASSWORD
            }
            SignInStep.VERIFY -> code.length == CODE_LENGTH
        }

    companion object {
        const val CODE_LENGTH = 6
        const val MIN_PASSWORD = 6
        const val RESEND_COOLDOWN = 30
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

    fun onModeChange(mode: SignInMode) = _state.update { it.copy(mode = mode, error = null) }

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value.filter(Char::isDigit).take(SignInUiState.CODE_LENGTH), error = null) }
        // The last digit verifies on its own; nobody should have to hunt for a button after typing a code.
        if (_state.value.code.length == SignInUiState.CODE_LENGTH) submit()
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        val email = current.email.trim()
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                when {
                    current.step == SignInStep.VERIFY -> auth.verifyEmail(email, current.code)
                    current.mode == SignInMode.SIGN_IN -> auth.signIn(email, current.password)
                    auth.signUp(email, current.password) == SignUpResult.VERIFY_EMAIL -> return@runCatching toVerify()
                }
                // A session now exists; the app root moves on, so only the spinner needs clearing.
                _state.update { it.copy(busy = false) }
            }.onFailure { e ->
                if (e is EtherError.EmailNotConfirmed) {
                    // An account that never confirmed: send a fresh code and take them straight to it.
                    runCatching { auth.resendVerification(email) }
                    toVerify()
                } else {
                    _state.update { it.copy(busy = false, error = e.userMessage()) }
                }
            }
        }
    }

    private fun toVerify() {
        _state.update { it.copy(step = SignInStep.VERIFY, code = "", busy = false) }
        startCooldown()
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

    fun resend() {
        val current = _state.value
        if (current.step != SignInStep.VERIFY || current.busy || current.resendIn > 0) return
        _state.update { it.copy(busy = true, code = "", error = null) }
        viewModelScope.launch {
            runCatching { auth.resendVerification(current.email.trim()) }
                .onSuccess { _state.update { it.copy(busy = false) }; startCooldown() }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.userMessage()) } }
        }
    }

    fun back() {
        cooldown?.cancel()
        _state.update { it.copy(step = SignInStep.CREDENTIALS, code = "", error = null, resendIn = 0) }
    }

    private var cooldown: Job? = null

    private fun startCooldown() {
        cooldown?.cancel()
        cooldown = viewModelScope.launch {
            for (left in SignInUiState.RESEND_COOLDOWN downTo 0) {
                _state.update { it.copy(resendIn = left) }
                if (left > 0) delay(1_000)
            }
        }
    }
}
