package dev.ether.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.AuthRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.domain.AuthState
import dev.ether.app.domain.ProfileId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update

sealed interface AppUiState {
    data object Loading : AppUiState
    data object SignedOut : AppUiState
    data class NeedsProfile(val userId: ProfileId) : AppUiState
    data object Ready : AppUiState
    data class Failed(val message: String) : AppUiState
}

class AppViewModel(private val auth: AuthRepository, private val profiles: ProfileRepository) : ViewModel() {
    private val recheck = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AppUiState> = combine(auth.authState.distinctUntilChanged(), recheck) { authState, _ -> authState }
        .transformLatest { authState ->
            when (authState) {
                AuthState.Loading -> emit(AppUiState.Loading)
                AuthState.SignedOut -> emit(AppUiState.SignedOut)
                is AuthState.SignedIn -> {
                    emit(AppUiState.Loading)
                    val profile = runCatching { profiles.myProfile() }
                    when {
                        profile.isFailure -> emit(AppUiState.Failed(profile.exceptionOrNull()!!.userMessage()))
                        // Sign-up always creates the profile row, so a missing row means the account was deleted
                        // (e.g. a DB reset) while this device still holds its token.
                        profile.getOrNull() == null -> {
                            runCatching { auth.signOut() }
                            emit(AppUiState.SignedOut)
                        }
                        profile.getOrNull()!!.isComplete -> emit(AppUiState.Ready)
                        else -> emit(AppUiState.NeedsProfile(authState.userId))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    fun recheck() = recheck.update { it + 1 }
}
