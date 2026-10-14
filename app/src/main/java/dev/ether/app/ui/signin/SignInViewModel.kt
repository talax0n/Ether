package dev.ether.app.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.AuthRepository
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SignInStep { EMAIL, CODE }

data class SignInUiState(
    val step: SignInStep = SignInStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val busy: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = !busy && when (step) {
            SignInStep.EMAIL -> email.isValidEmail()
            SignInStep.CODE -> code.length == CODE_LENGTH
        }

    companion object {
        const val CODE_LENGTH = 6
    }
}

private fun String.isValidEmail() = trim().let { '@' in it && it.substringAfter('@').contains('.') }

/** Signing in is complete when the auth flow reports a session; the app root reacts to that, not to this state. */
class SignInViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onCodeChange(value: String) = _state.update {
        it.copy(code = value.filter(Char::isDigit).take(SignInUiState.CODE_LENGTH), error = null)
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                when (current.step) {
                    SignInStep.EMAIL -> auth.sendOtp(current.email.trim())
                    SignInStep.CODE -> auth.verifyOtp(current.email.trim(), current.code)
                }
            }.onSuccess {
                _state.update { if (current.step == SignInStep.EMAIL) it.copy(step = SignInStep.CODE, busy = false) else it.copy(busy = false) }
            }.onFailure { e ->
                _state.update { it.copy(busy = false, error = e.userMessage()) }
            }
        }
    }

    fun changeEmail() = _state.update { it.copy(step = SignInStep.EMAIL, code = "", error = null) }
}
