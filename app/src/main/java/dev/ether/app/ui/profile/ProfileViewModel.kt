package dev.ether.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ether.app.data.AuthRepository
import dev.ether.app.data.ProfileRepository
import dev.ether.app.domain.ProfileSummary
import dev.ether.app.ui.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Loaded(val summary: ProfileSummary) : ProfileUiState
    data class Failed(val message: String) : ProfileUiState
}

class ProfileViewModel(private val profiles: ProfileRepository, private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load() {
        _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { profiles.mySummary() }
                .fold({ ProfileUiState.Loaded(it) }, { ProfileUiState.Failed(it.userMessage()) })
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
