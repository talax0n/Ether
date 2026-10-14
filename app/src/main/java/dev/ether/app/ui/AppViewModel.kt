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

class AppViewModel(auth: AuthRepository, private val profiles: ProfileRepository) : ViewModel() {
    private val recheck = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AppUiState> = combine(auth.authState.distinctUntilChanged(), recheck) { authState, _ -> authState }
        .transformLatest { authState ->
            when (authState) {
                AuthState.Loading -> emit(AppUiState.Loading)
                AuthState.SignedOut -> emit(AppUiState.SignedOut)
                is AuthState.SignedIn -> {
                    emit(AppUiState.Loading)
                    emit(
                        runCatching { profiles.myProfile() }.fold(
                            onSuccess = { if (it?.isComplete == true) AppUiState.Ready else AppUiState.NeedsProfile(authState.userId) },
                            onFailure = { AppUiState.Failed(it.userMessage()) },
                        ),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    fun recheck() = recheck.update { it + 1 }
}
